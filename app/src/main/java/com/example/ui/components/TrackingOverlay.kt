package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LiveTrackingState
import com.example.ui.theme.EyeCyan
import com.example.ui.theme.EyeEmerald

@Composable
fun TrackingOverlay(
    trackingState: LiveTrackingState,
    profileName: String,
    isGestureLocked: Boolean,
    showTrackingBall: Boolean,
    showEyeLandmarks: Boolean,
    showFaceLandmarks: Boolean,
    showConfidence: Boolean,
    lastDetectedGestureName: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().testTag("tracking_overlay")) {

        // 1. Canvas for smoothed tracking ball and optional landmarks
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // Render Face Landmarks if enabled
            if (showFaceLandmarks && trackingState.landmarks.isNotEmpty()) {
                trackingState.landmarks.forEach { point ->
                    drawCircle(
                        color = Color(0x6600E5FF),
                        radius = 2.5f.dp.toPx(),
                        center = Offset(point.x * canvasW, point.y * canvasH)
                    )
                }
            }

            // Render Eye Landmarks if enabled
            if (showEyeLandmarks) {
                trackingState.leftEyePoint?.let { pt ->
                    val cx = pt.x * canvasW
                    val cy = pt.y * canvasH
                    drawCircle(
                        color = EyeCyan,
                        radius = 6.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = EyeCyan,
                        radius = 2.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }

                trackingState.rightEyePoint?.let { pt ->
                    val cx = pt.x * canvasW
                    val cy = pt.y * canvasH
                    drawCircle(
                        color = EyeCyan,
                        radius = 6.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = EyeCyan,
                        radius = 2.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }

            // Render Smoothed Green Eye-Tracking Ball
            if (showTrackingBall && trackingState.isFaceDetected) {
                val ballX = trackingState.gazeX * canvasW
                val ballY = trackingState.gazeY * canvasH

                // Outer ambient glow ring
                drawCircle(
                    color = Color(0x3300E676),
                    radius = 24.dp.toPx(),
                    center = Offset(ballX, ballY)
                )

                // Mid ring
                drawCircle(
                    color = EyeEmerald,
                    radius = 14.dp.toPx(),
                    center = Offset(ballX, ballY),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Core tracking pupil dot
                drawCircle(
                    color = EyeEmerald,
                    radius = 7.dp.toPx(),
                    center = Offset(ballX, ballY)
                )

                // Center highlight
                drawCircle(
                    color = Color.White,
                    radius = 2.5f.dp.toPx(),
                    center = Offset(ballX - 1.5.dp.toPx(), ballY - 1.5.dp.toPx())
                )
            }
        }

        // 2. Top Status HUD: Confidence, Profile, and Gesture Lock
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Confidence Pill
                if (showConfidence) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xCC0D1626),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2E48)),
                        modifier = Modifier.testTag("confidence_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (trackingState.isFaceDetected) EyeEmerald else Color.Gray,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Eye Tracking: ${if (trackingState.isFaceDetected) "${trackingState.trackingConfidencePercent}%" else "Offline"}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Profile Badge & Lock Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isGestureLocked) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xCCFF5252),
                            modifier = Modifier.padding(end = 6.dp).testTag("locked_badge")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Gestures Locked",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LOCKED",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xCC131F33),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF233654))
                    ) {
                        Text(
                            text = profileName,
                            color = EyeCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Detection Checkpoints Card (Face, Left Eye, Right Eye)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xBB0B1322),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x441F3250)),
                modifier = Modifier.fillMaxWidth().testTag("detection_status_panel")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(
                        label = "Face",
                        isDetected = trackingState.isFaceDetected
                    )
                    StatusBadge(
                        label = "Left Eye",
                        isDetected = trackingState.isLeftEyeDetected
                    )
                    StatusBadge(
                        label = "Right Eye",
                        isDetected = trackingState.isRightEyeDetected
                    )
                }
            }

            // Real-time Status or Guidance Message
            if (trackingState.statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trackingState.statusMessage,
                    color = if (trackingState.isFaceDetected) EyeEmerald else Color(0xFFFFB74D),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // 3. Bottom Last Detected Gesture Indicator
        AnimatedVisibility(
            visible = !lastDetectedGestureName.isNullOrEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xDD0D1E16),
                border = androidx.compose.foundation.BorderStroke(1.dp, EyeEmerald),
                modifier = Modifier.testTag("last_gesture_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Detected Gesture:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "\"${lastDetectedGestureName.orEmpty()}\"",
                        color = EyeEmerald,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, isDetected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = if (isDetected) "Detected ✓" else "Searching...",
            color = if (isDetected) EyeEmerald else Color(0xFFFF8A80),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
