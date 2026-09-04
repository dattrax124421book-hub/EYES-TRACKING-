package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Gestures detectable by the eye-tracking engine.
 */
enum class GestureType(val displayName: String, val category: String) {
    // Blink gestures
    SINGLE_BLINK("Single Blink", "Blink"),
    DOUBLE_BLINK("Double Blink", "Blink"),
    TRIPLE_BLINK("3 × Blink", "Blink"),
    QUAD_BLINK("4 × Blink", "Blink"),
    LEFT_EYE_BLINK("Left-Eye Blink", "Blink"),
    RIGHT_EYE_BLINK("Right-Eye Blink", "Blink"),
    BOTH_EYES_BLINK("Both-Eyes Long Blink", "Blink"),

    // Gaze gestures
    LOOK_LEFT("Look Left", "Gaze"),
    LOOK_RIGHT("Look Right", "Gaze"),
    LOOK_UP("Look Up", "Gaze"),
    LOOK_DOWN("Look Down", "Gaze"),
    HOLD_GAZE_LEFT("Hold Gaze Left", "Gaze"),
    HOLD_GAZE_RIGHT("Hold Gaze Right", "Gaze"),
    HOLD_GAZE_UP("Hold Gaze Up", "Gaze"),
    HOLD_GAZE_DOWN("Hold Gaze Down", "Gaze"),

    // Combined gestures
    LOOK_RIGHT_AND_BLINK("Look Right + Blink", "Combined"),
    LOOK_LEFT_AND_DOUBLE_BLINK("Look Left + Double Blink", "Combined"),
    LOOK_RIGHT_LOOK_LEFT_BLINK("Look Right → Look Left → Blink", "Combined")
}

/**
 * Actions that can be performed when a gesture is recognized.
 */
enum class ActionType(val displayName: String, val iconName: String) {
    OPEN_APP("Open Installed App", "Apps"),
    LAUNCH_CAMERA("Launch Camera", "CameraAlt"),
    OPEN_GALLERY("Open Gallery", "PhotoLibrary"),
    OPEN_BROWSER("Open Web Browser", "Language"),
    OPEN_SETTINGS("Open Android Settings", "Settings"),
    MEDIA_PLAY_PAUSE("Play / Pause Media", "PlayCircle"),
    VOLUME_UP("Volume Up", "VolumeUp"),
    VOLUME_DOWN("Volume Down", "VolumeDown"),
    FLASHLIGHT_TOGGLE("Toggle Flashlight", "FlashlightOn"),
    HAPTIC_PULSE("Haptic Vibration Pulse", "Vibration"),
    SPEAK_NOTIFICATION("Speak Custom Phrase (TTS)", "RecordVoiceOver"),
    GO_BACK_HINT("Simulate Back Navigation", "ArrowBack")
}

/**
 * Persisted Action mapping for gestures.
 */
@Entity(tableName = "gesture_actions")
data class GestureAction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long = 1, // Profile identifier
    val gestureType: GestureType,
    val actionType: ActionType,
    val targetPackageName: String? = null,
    val targetAppLabel: String? = null,
    val customPhrase: String? = null,
    val isEnabled: Boolean = true,
    val cooldownSeconds: Int = 3, // Cooldown prevention
    val requiresConfirmation: Boolean = false, // Optional confirmation
    val delaySeconds: Float = 0f, // Optional delay
    val isChain: Boolean = false, // If true, executes chain steps
    val chainStepsJson: String = "" // Serialized chain steps
)

/**
 * Step inside an Action Chain.
 */
data class ActionChainStep(
    val stepOrder: Int,
    val actionType: ActionType,
    val targetPackageName: String? = null,
    val targetAppLabel: String? = null,
    val customPhrase: String? = null,
    val delayAfterSeconds: Float = 1.0f,
    val isEnabled: Boolean = true
)

/**
 * Execution Profile (e.g. Gaming, Video, Reading, Accessibility, Custom).
 */
@Entity(tableName = "action_profiles")
data class ActionProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val iconKey: String = "accessibility",
    val isDefault: Boolean = false
)

/**
 * History item recording gesture recognitions and executions.
 */
@Entity(tableName = "gesture_history")
data class GestureHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val gestureName: String,
    val actionName: String,
    val profileName: String,
    val status: String // "EXECUTED", "TEST_MODE", "LOCKED", "COOLDOWN", "CANCELLED"
)

/**
 * Calibration profile storing user-specific eye/face tracking thresholds.
 */
@Entity(tableName = "calibration_data")
data class CalibrationData(
    @PrimaryKey val id: Int = 1,
    val normalEyeOpenLeft: Float = 0.90f,
    val normalEyeOpenRight: Float = 0.90f,
    val blinkThreshold: Float = 0.35f,
    val leftWinkThreshold: Float = 0.30f,
    val rightWinkThreshold: Float = 0.30f,
    val lookLeftThresholdAngle: Float = -12f,
    val lookRightThresholdAngle: Float = 12f,
    val lookUpThresholdAngle: Float = -10f,
    val lookDownThresholdAngle: Float = 10f,
    val sensitivityLevel: String = "Medium", // Low, Medium, High, Custom
    val smoothingFactor: Float = 0.25f, // EMA alpha
    val combinationWindowSeconds: Float = 1.5f,
    val lastCalibratedTime: Long = System.currentTimeMillis()
)

/**
 * Live frame tracking state emitted by the EyeTrackingEngine.
 */
data class LiveTrackingState(
    val isTrackingActive: Boolean = false,
    val isFaceDetected: Boolean = false,
    val isLeftEyeDetected: Boolean = false,
    val isRightEyeDetected: Boolean = false,
    val leftEyeOpenProb: Float = 0f,
    val rightEyeOpenProb: Float = 0f,
    val headEulerYaw: Float = 0f, // Negative is turn left, positive is turn right
    val headEulerPitch: Float = 0f, // Negative is look up, positive is look down
    val headEulerRoll: Float = 0f,
    val gazeX: Float = 0.5f, // Normalized 0..1 smoothed tracking ball position
    val gazeY: Float = 0.5f,
    val rawGazeX: Float = 0.5f,
    val rawGazeY: Float = 0.5f,
    val trackingConfidencePercent: Int = 0,
    val statusMessage: String = "Tracking ready",
    val landmarks: List<TrackingPoint> = emptyList(),
    val leftEyePoint: TrackingPoint? = null,
    val rightEyePoint: TrackingPoint? = null,
    val nosePoint: TrackingPoint? = null
)

data class TrackingPoint(
    val x: Float, // Normalized 0f..1f
    val y: Float
)
