package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EyeCyan
import com.example.ui.theme.EyeEmerald
import com.example.viewmodel.EyeGestureViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: EyeGestureViewModel,
    onNavigateToCalibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showBall by viewModel.showTrackingBall.collectAsState()
    val showEyeLandmarks by viewModel.showEyeLandmarks.collectAsState()
    val showFaceLandmarks by viewModel.showFaceLandmarks.collectAsState()
    val showConfidence by viewModel.showConfidenceDisplay.collectAsState()
    val calibrationData by viewModel.calibrationData.collectAsState()

    val lastCalibratedDateStr = calibrationData?.let {
        if (it.lastCalibratedTime > 0) {
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(it.lastCalibratedTime))
        } else "Never"
    } ?: "Default settings"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "Preferences & Engine",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "Configure eye overlay elements, calibration, and safety boundaries",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }

        // Overlay Visual Settings Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth().testTag("overlay_settings_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Live Tracking Visual Overlays",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                SettingSwitchRow(
                    title = "Green Eye-Tracking Ball",
                    subtitle = "Dynamic smoothed circle following detected pupil gaze",
                    checked = showBall,
                    onCheckedChange = { viewModel.showTrackingBall.value = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchRow(
                    title = "Eye Feature Landmarks",
                    subtitle = "Cyan crosshair markers on left and right eye points",
                    checked = showEyeLandmarks,
                    onCheckedChange = { viewModel.showEyeLandmarks.value = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchRow(
                    title = "Facial Contour Landmarks",
                    subtitle = "Subtle geometry points across face and nose bridge",
                    checked = showFaceLandmarks,
                    onCheckedChange = { viewModel.showFaceLandmarks.value = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchRow(
                    title = "Confidence Metric HUD",
                    subtitle = "Real-time tracking accuracy percentage chip",
                    checked = showConfidence,
                    onCheckedChange = { viewModel.showConfidenceDisplay.value = it }
                )
            }
        }

        // Calibration Management Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = BorderStroke(1.dp, EyeEmerald),
            modifier = Modifier.fillMaxWidth().testTag("calibration_settings_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Eye Calibration Profile",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Last calibrated: $lastCalibratedDateStr",
                            fontSize = 11.sp,
                            color = EyeEmerald
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = EyeEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Current sensitivity: ${calibrationData?.sensitivityLevel ?: "Medium"} • Smoothing: ${"%.2f".format(calibrationData?.smoothingFactor ?: 0.25f)}",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onNavigateToCalibration,
                    colors = ButtonDefaults.buttonColors(containerColor = EyeEmerald, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Launch Calibration Wizard", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Privacy & Local Processing Architecture
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth().testTag("privacy_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = EyeCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "100% On-Device Privacy Architecture",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "All computer vision analysis using Google ML Kit Face & Eye Detection runs entirely on your local CPU/GPU. No video frames, photographs, or biometrics are ever stored, logged, or uploaded to any remote server.",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 18.sp
                )
            }
        }

        // Action Engine Capability & Security Disclosures (System Spec)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth().testTag("disclosures_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFFD600),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Android Automation Scope",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This app leverages legitimate standard Android APIs: Intent launching for Camera/Browser/Gallery/Apps, AudioManager for volume & media dispatch, CameraManager for torch/flashlight, and TextToSpeech. In strict compliance with Android security guidelines, it does not inject unauthorized touch taps into third-party apps.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EyeEmerald,
                uncheckedThumbColor = Color(0xFF64748B),
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}
