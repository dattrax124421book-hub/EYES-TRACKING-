package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalibrationData
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EyeCyan
import com.example.ui.theme.EyeEmerald
import com.example.viewmodel.EyeGestureViewModel

@Composable
fun CalibrationScreen(
    viewModel: EyeGestureViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val liveTracking by viewModel.liveTrackingState.collectAsState()
    val savedCalibration by viewModel.calibrationData.collectAsState()

    var currentStep by remember { mutableIntStateOf(1) } // Steps 1 to 7

    // Dynamic calibration values matching CalibrationData model
    var normalOpenLeft by remember { mutableFloatStateOf(savedCalibration?.normalEyeOpenLeft ?: 0.85f) }
    var normalOpenRight by remember { mutableFloatStateOf(savedCalibration?.normalEyeOpenRight ?: 0.85f) }
    var blinkThreshold by remember { mutableFloatStateOf(savedCalibration?.blinkThreshold ?: 0.35f) }
    var leftWinkThreshold by remember { mutableFloatStateOf(savedCalibration?.leftWinkThreshold ?: 0.30f) }
    var rightWinkThreshold by remember { mutableFloatStateOf(savedCalibration?.rightWinkThreshold ?: 0.30f) }
    var lookLeftThreshold by remember { mutableFloatStateOf(savedCalibration?.lookLeftThresholdAngle ?: -12.0f) }
    var lookRightThreshold by remember { mutableFloatStateOf(savedCalibration?.lookRightThresholdAngle ?: 12.0f) }
    var lookUpThreshold by remember { mutableFloatStateOf(savedCalibration?.lookUpThresholdAngle ?: -10.0f) }
    var lookDownThreshold by remember { mutableFloatStateOf(savedCalibration?.lookDownThresholdAngle ?: 10.0f) }
    var smoothingFactor by remember { mutableFloatStateOf(savedCalibration?.smoothingFactor ?: 0.25f) }
    var sensitivityLevel by remember { mutableStateOf(savedCalibration?.sensitivityLevel ?: "Medium") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("calibration_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onFinish) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "Eye Calibration Wizard",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Step $currentStep of 7",
                    fontSize = 12.sp,
                    color = EyeCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Bar Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            for (i in 1..7) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            if (i == currentStep) EyeEmerald else if (i < currentStep) EyeCyan else Color(0xFF23314B),
                            CircleShape
                        )
                )
            }
        }

        // Live Telemetry Mini-Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Telemetry:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )

                Text(
                    text = "L: ${"%.2f".format(liveTracking.leftEyeOpenProb)} | R: ${"%.2f".format(liveTracking.rightEyeOpenProb)} | Yaw: ${"%.1f".format(liveTracking.headEulerYaw)}°",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = if (liveTracking.isFaceDetected) EyeEmerald else Color(0xFFFFB74D)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calibration Step Content (Section 3)
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "step_anim"
        ) { step ->
            when (step) {
                1 -> CalibrationStepCard(
                    title = "Step 1: Normal Eye-Open State",
                    instruction = "Look directly at the screen naturally with both eyes open. The app records your resting openness.",
                    currentReading = "Left: ${"%.2f".format(liveTracking.leftEyeOpenProb)}, Right: ${"%.2f".format(liveTracking.rightEyeOpenProb)}",
                    targetThresholdText = "Open Baseline: L ${"%.2f".format(normalOpenLeft)} / R ${"%.2f".format(normalOpenRight)}",
                    onSample = {
                        if (liveTracking.leftEyeOpenProb > 0.4f) normalOpenLeft = liveTracking.leftEyeOpenProb
                        if (liveTracking.rightEyeOpenProb > 0.4f) normalOpenRight = liveTracking.rightEyeOpenProb
                    }
                )

                2 -> CalibrationStepCard(
                    title = "Step 2: Normal Blink State",
                    instruction = "Close your eyes or blink normally. The app determines your blink closure threshold.",
                    currentReading = "Left: ${"%.2f".format(liveTracking.leftEyeOpenProb)}, Right: ${"%.2f".format(liveTracking.rightEyeOpenProb)}",
                    targetThresholdText = "Blink Cutoff: ${"%.2f".format(blinkThreshold)}",
                    onSample = {
                        val avg = (liveTracking.leftEyeOpenProb + liveTracking.rightEyeOpenProb) / 2f
                        if (avg < 0.45f) blinkThreshold = (avg + 0.12f).coerceAtMost(0.38f)
                    }
                )

                3 -> CalibrationStepCard(
                    title = "Step 3: Wink Left Eye",
                    instruction = "Close only your left eye while keeping your right eye open. The app learns your left wink threshold.",
                    currentReading = "Left: ${"%.2f".format(liveTracking.leftEyeOpenProb)} (Right: ${"%.2f".format(liveTracking.rightEyeOpenProb)})",
                    targetThresholdText = "Left Wink Cutoff: ${"%.2f".format(leftWinkThreshold)}",
                    onSample = {
                        if (liveTracking.leftEyeOpenProb < 0.35f && liveTracking.rightEyeOpenProb > 0.5f) {
                            leftWinkThreshold = (liveTracking.leftEyeOpenProb + 0.1f).coerceAtMost(0.35f)
                        }
                    }
                )

                4 -> CalibrationStepCard(
                    title = "Step 4: Wink Right Eye",
                    instruction = "Close only your right eye while keeping your left eye open. The app verifies your right wink threshold.",
                    currentReading = "Right: ${"%.2f".format(liveTracking.rightEyeOpenProb)} (Left: ${"%.2f".format(liveTracking.leftEyeOpenProb)})",
                    targetThresholdText = "Right Wink Cutoff: ${"%.2f".format(rightWinkThreshold)}",
                    onSample = {
                        if (liveTracking.rightEyeOpenProb < 0.35f && liveTracking.leftEyeOpenProb > 0.5f) {
                            rightWinkThreshold = (liveTracking.rightEyeOpenProb + 0.1f).coerceAtMost(0.35f)
                        }
                    }
                )

                5 -> CalibrationStepCard(
                    title = "Step 5: Gaze Bounds Left & Right",
                    instruction = "Turn your gaze or head slightly left, then right to establish directional boundaries.",
                    currentReading = "Head Yaw: ${"%.1f".format(liveTracking.headEulerYaw)}°",
                    targetThresholdText = "Left: ${"%.1f".format(lookLeftThreshold)}° | Right: +${"%.1f".format(lookRightThreshold)}°",
                    onSample = {
                        val yaw = liveTracking.headEulerYaw
                        if (yaw < -5f) lookLeftThreshold = yaw * 0.85f
                        if (yaw > 5f) lookRightThreshold = yaw * 0.85f
                    }
                )

                6 -> CalibrationStepCard(
                    title = "Step 6: Gaze Bounds Up & Down",
                    instruction = "Tilt your gaze up, then down to configure vertical gaze detection boundaries.",
                    currentReading = "Head Pitch: ${"%.1f".format(liveTracking.headEulerPitch)}°",
                    targetThresholdText = "Up: ${"%.1f".format(lookUpThreshold)}° | Down: +${"%.1f".format(lookDownThreshold)}°",
                    onSample = {
                        val pitch = liveTracking.headEulerPitch
                        if (pitch < -5f) lookUpThreshold = pitch * 0.85f
                        if (pitch > 5f) lookDownThreshold = pitch * 0.85f
                    }
                )

                7 -> Step7AdjustmentCard(
                    smoothingAlpha = smoothingFactor,
                    onAlphaChange = { smoothingFactor = it },
                    sensitivityLevel = sensitivityLevel,
                    onSensitivityChange = { sensitivityLevel = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentStep > 1) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (currentStep < 7) {
                Button(
                    onClick = { currentStep++ },
                    colors = ButtonDefaults.buttonColors(containerColor = EyeEmerald, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Next Step", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            } else {
                Button(
                    onClick = {
                        // Save Calibration
                        val cal = CalibrationData(
                            normalEyeOpenLeft = normalOpenLeft,
                            normalEyeOpenRight = normalOpenRight,
                            blinkThreshold = blinkThreshold,
                            leftWinkThreshold = leftWinkThreshold,
                            rightWinkThreshold = rightWinkThreshold,
                            lookLeftThresholdAngle = lookLeftThreshold,
                            lookRightThresholdAngle = lookRightThreshold,
                            lookUpThresholdAngle = lookUpThreshold,
                            lookDownThresholdAngle = lookDownThreshold,
                            sensitivityLevel = sensitivityLevel,
                            smoothingFactor = smoothingFactor,
                            lastCalibratedTime = System.currentTimeMillis()
                        )
                        viewModel.updateCalibration(cal)
                        onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EyeEmerald, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_calibration_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Complete", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CalibrationStepCard(
    title: String,
    instruction: String,
    currentReading: String,
    targetThresholdText: String,
    onSample: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = BorderStroke(1.5.dp, EyeEmerald),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = instruction,
                fontSize = 13.sp,
                color = Color(0xFFCBD5E1),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Live Measurement", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(
                        text = currentReading,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = EyeCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = targetThresholdText,
                        fontSize = 12.sp,
                        color = EyeEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSample,
                colors = ButtonDefaults.buttonColors(containerColor = EyeCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Capture Reading", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Step7AdjustmentCard(
    smoothingAlpha: Float,
    onAlphaChange: (Float) -> Unit,
    sensitivityLevel: String,
    onSensitivityChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = BorderStroke(1.5.dp, EyeEmerald),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Step 7: Sensitivity & Smoothing",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Tracking Smoothing Filter (EMA)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text("Lower = smoother & less jitter; Higher = faster reaction", fontSize = 11.sp, color = Color(0xFF94A3B8))
            Slider(
                value = smoothingAlpha,
                onValueChange = onAlphaChange,
                valueRange = 0.10f..0.50f,
                steps = 8,
                colors = SliderDefaults.colors(thumbColor = EyeEmerald, activeTrackColor = EyeEmerald)
            )
            Text("Factor: ${"%.2f".format(smoothingAlpha)}", fontSize = 12.sp, color = EyeEmerald)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Overall Sensitivity Level", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Low", "Medium", "High").forEach { level ->
                    val isSelected = sensitivityLevel.equals(level, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) EyeEmerald else DarkSurface,
                        border = BorderStroke(1.dp, if (isSelected) EyeEmerald else DarkBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        onClick = { onSensitivityChange(level) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = level,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
