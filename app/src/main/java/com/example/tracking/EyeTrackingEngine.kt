package com.example.tracking

import android.content.Context
import android.graphics.PointF
import android.media.Image
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.data.model.CalibrationData
import com.example.data.model.LiveTrackingState
import com.example.data.model.TrackingPoint
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * On-device face and eye tracking engine utilizing CameraX and ML Kit.
 * Computes eye openness, head gaze yaw/pitch, smoothed eye pupil coordinates, and confidence.
 */
class EyeTrackingEngine(
    private val context: Context,
    private val onFrameMetrics: (LiveTrackingState) -> Unit
) : ImageAnalysis.Analyzer {

    private val _trackingState = MutableStateFlow(LiveTrackingState())
    val trackingState: StateFlow<LiveTrackingState> = _trackingState.asStateFlow()

    private var detector: FaceDetector

    // Calibration thresholds & smoothing
    var calibration: CalibrationData = CalibrationData()
        set(value) {
            field = value
            smoothingFactor = value.smoothingFactor
        }

    private var smoothingFactor: Float = 0.25f

    // Smoothed gaze tracking ball coordinates
    private var smoothedGazeX: Float = 0.5f
    private var smoothedGazeY: Float = 0.5f

    // Frame throttling to optimize battery & CPU (~25 fps max)
    private var lastAnalyzedTimestamp = 0L
    private val frameIntervalMs = 38L // ~26 FPS ceiling

    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.12f)
            .build()
        detector = FaceDetection.getClient(options)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp < frameIntervalMs) {
            imageProxy.close()
            return
        }
        lastAnalyzedTimestamp = currentTimestamp

        val mediaImage: Image? = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        // Fast brightness estimation from Y-plane
        val avgLuminance = calculateAverageLuminance(imageProxy)
        val isLightingDark = avgLuminance < 35

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        val imageWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.height else imageProxy.width
        val imageHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.width else imageProxy.height

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                processFaces(faces, imageWidth, imageHeight, isLightingDark)
            }
            .addOnFailureListener {
                _trackingState.value = _trackingState.value.copy(
                    isFaceDetected = false,
                    isTrackingActive = true,
                    statusMessage = "Tracking sensor error: ${it.localizedMessage ?: "Unknown"}",
                    trackingConfidencePercent = 0
                )
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun processFaces(
        faces: List<Face>,
        imageWidth: Int,
        imageHeight: Int,
        isLightingDark: Boolean
    ) {
        if (faces.isEmpty()) {
            val status = if (isLightingDark) "Lighting is too dark. Move to better light." else "Face not detected. Position face in camera view."
            val newState = _trackingState.value.copy(
                isTrackingActive = true,
                isFaceDetected = false,
                isLeftEyeDetected = false,
                isRightEyeDetected = false,
                leftEyeOpenProb = 0f,
                rightEyeOpenProb = 0f,
                trackingConfidencePercent = 0,
                statusMessage = status
            )
            _trackingState.value = newState
            onFrameMetrics(newState)
            return
        }

        // Use the primary face (largest face in frame)
        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: faces[0]

        val leftEyeOpen = face.leftEyeOpenProbability ?: -1f
        val rightEyeOpen = face.rightEyeOpenProbability ?: -1f
        val leftEyeDetected = leftEyeOpen >= 0f
        val rightEyeDetected = rightEyeOpen >= 0f

        val yaw = face.headEulerAngleY // Turn left/right (- is left, + is right)
        val pitch = face.headEulerAngleX // Up/down (- is up, + is down)
        val roll = face.headEulerAngleZ

        val leftEyeLandmark = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEyeLandmark = face.getLandmark(FaceLandmark.RIGHT_EYE)
        val noseLandmark = face.getLandmark(FaceLandmark.NOSE_BASE)

        // Landmarks normalization (0..1)
        val landmarksList = mutableListOf<TrackingPoint>()
        var leftEyePoint: TrackingPoint? = null
        var rightEyePoint: TrackingPoint? = null
        var nosePoint: TrackingPoint? = null

        face.allLandmarks.forEach { lm ->
            val normX = 1f - (lm.position.x / imageWidth.toFloat()) // Mirror for front camera
            val normY = lm.position.y / imageHeight.toFloat()
            val point = TrackingPoint(normX.coerceIn(0f, 1f), normY.coerceIn(0f, 1f))
            landmarksList.add(point)

            if (lm.landmarkType == FaceLandmark.LEFT_EYE) leftEyePoint = point
            if (lm.landmarkType == FaceLandmark.RIGHT_EYE) rightEyePoint = point
            if (lm.landmarkType == FaceLandmark.NOSE_BASE) nosePoint = point
        }

        // Calculate raw gaze point on screen
        // Baseline from eye center + head yaw/pitch offsets
        val eyeCenterX = if (leftEyePoint != null && rightEyePoint != null) {
            (leftEyePoint!!.x + rightEyePoint!!.x) / 2f
        } else {
            0.5f
        }
        val eyeCenterY = if (leftEyePoint != null && rightEyePoint != null) {
            (leftEyePoint!!.y + rightEyePoint!!.y) / 2f
        } else {
            0.45f
        }

        // Map head yaw & pitch to screen coordinate shift
        val yawFactor = 0.015f
        val pitchFactor = 0.015f
        val targetGazeX = (eyeCenterX - (yaw * yawFactor)).coerceIn(0.05f, 0.95f)
        val targetGazeY = (eyeCenterY + (pitch * pitchFactor)).coerceIn(0.05f, 0.95f)

        // Exponential Moving Average Smoothing to eliminate jitter
        smoothedGazeX += smoothingFactor * (targetGazeX - smoothedGazeX)
        smoothedGazeY += smoothingFactor * (targetGazeY - smoothedGazeY)

        // Confidence calculation
        var confidence = 70
        if (leftEyeDetected && rightEyeDetected) confidence += 20
        if (leftEyeLandmark != null && rightEyeLandmark != null) confidence += 8
        if (!isLightingDark) confidence += 2
        confidence = min(99, confidence)

        val statusMsg = when {
            isLightingDark -> "Low lighting: detection confidence may be lower"
            !leftEyeDetected && !rightEyeDetected -> "Detecting eyes..."
            !leftEyeDetected || !rightEyeDetected -> "Only one eye is clearly visible"
            else -> "Tracking Active ✓"
        }

        val updatedState = LiveTrackingState(
            isTrackingActive = true,
            isFaceDetected = true,
            isLeftEyeDetected = leftEyeDetected,
            isRightEyeDetected = rightEyeDetected,
            leftEyeOpenProb = if (leftEyeOpen >= 0f) leftEyeOpen else 0.5f,
            rightEyeOpenProb = if (rightEyeOpen >= 0f) rightEyeOpen else 0.5f,
            headEulerYaw = yaw,
            headEulerPitch = pitch,
            headEulerRoll = roll,
            gazeX = smoothedGazeX,
            gazeY = smoothedGazeY,
            rawGazeX = targetGazeX,
            rawGazeY = targetGazeY,
            trackingConfidencePercent = confidence,
            statusMessage = statusMsg,
            landmarks = landmarksList,
            leftEyePoint = leftEyePoint,
            rightEyePoint = rightEyePoint,
            nosePoint = nosePoint
        )

        _trackingState.value = updatedState
        onFrameMetrics(updatedState)
    }

    private fun calculateAverageLuminance(imageProxy: ImageProxy): Int {
        val plane = imageProxy.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        buffer.rewind()

        var sum = 0L
        val step = max(1, data.size / 200) // Sample 200 pixels
        var count = 0
        for (i in 0 until data.size step step) {
            sum += (data[i].toInt() and 0xFF)
            count++
        }
        return if (count > 0) (sum / count).toInt() else 128
    }

    fun release() {
        try {
            detector.close()
        } catch (e: Exception) {
            // Ignore on cleanup
        }
    }
}
