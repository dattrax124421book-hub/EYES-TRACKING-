package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.components.ActionConfirmationDialog
import com.example.ui.components.GestureFeedbackPanel
import com.example.ui.components.TrackingOverlay
import com.example.ui.theme.EyeCyan
import com.example.ui.theme.EyeEmerald
import com.example.viewmodel.EyeGestureViewModel
import java.util.concurrent.Executors

@Composable
fun TrackingScreen(
    viewModel: EyeGestureViewModel,
    onNavigateToCalibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val trackingState by viewModel.liveTrackingState.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val isGestureLocked by viewModel.isGestureLocked.collectAsState()
    val isTestMode by viewModel.isTestMode.collectAsState()
    val lastGesture by viewModel.lastDetectedGesture.collectAsState()
    val feedbackState by viewModel.feedbackState.collectAsState()
    val confirmationPrompt by viewModel.confirmationPrompt.collectAsState()

    val showBall by viewModel.showTrackingBall.collectAsState()
    val showEyeLandmarks by viewModel.showEyeLandmarks.collectAsState()
    val showFaceLandmarks by viewModel.showFaceLandmarks.collectAsState()
    val showConfidence by viewModel.showConfidenceDisplay.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("tracking_screen")
    ) {
        if (hasCameraPermission) {
            // CameraX Live Preview with front camera
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val cameraExecutor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build().also {
                                    it.setAnalyzer(cameraExecutor, viewModel.trackingEngine)
                                }

                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                                .build()

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            // Fallback if front camera lens fails
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlaid Tracking Visualization (Tracking Ball, Landmarks, HUD)
            TrackingOverlay(
                trackingState = trackingState,
                profileName = activeProfile?.name ?: "Accessibility",
                isGestureLocked = isGestureLocked,
                showTrackingBall = showBall,
                showEyeLandmarks = showEyeLandmarks,
                showFaceLandmarks = showFaceLandmarks,
                showConfidence = showConfidence,
                lastDetectedGestureName = lastGesture?.displayName
            )

            // Floating Quick Control Pills at bottom
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xDD0C1524),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2E48)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .testTag("floating_controls_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lock Toggle
                    IconButton(
                        onClick = { viewModel.toggleGestureLock() },
                        modifier = Modifier.testTag("track_lock_button")
                    ) {
                        Icon(
                            imageVector = if (isGestureLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Toggle Gesture Lock",
                            tint = if (isGestureLocked) Color(0xFFFF5252) else EyeEmerald
                        )
                    }

                    // Test Mode Toggle
                    IconButton(
                        onClick = { viewModel.toggleTestMode() },
                        modifier = Modifier.testTag("track_test_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Toggle Test Mode",
                            tint = if (isTestMode) EyeCyan else Color(0xFF64748B)
                        )
                    }

                    // Eye Landmarks Toggle
                    IconButton(
                        onClick = { viewModel.showEyeLandmarks.value = !showEyeLandmarks },
                        modifier = Modifier.testTag("track_landmarks_button")
                    ) {
                        Icon(
                            imageVector = if (showEyeLandmarks) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Landmarks",
                            tint = if (showEyeLandmarks) EyeCyan else Color(0xFF64748B)
                        )
                    }

                    // Calibration Shortcut
                    IconButton(
                        onClick = onNavigateToCalibration,
                        modifier = Modifier.testTag("track_calibration_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Calibrate",
                            tint = EyeEmerald
                        )
                    }
                }
            }

            // Detected Gesture Feedback Panel (Section 4)
            GestureFeedbackPanel(
                feedbackState = feedbackState,
                onTestClick = {
                    viewModel.toggleTestMode()
                    viewModel.dismissFeedback()
                },
                onDoneClick = {
                    viewModel.dismissFeedback()
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp)
            )

            // Confirmation Dialog (Section 9)
            ActionConfirmationDialog(
                promptState = confirmationPrompt,
                onConfirm = { viewModel.confirmPendingAction() },
                onCancel = { viewModel.cancelPendingAction() }
            )

        } else {
            // Permission Denied State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = EyeCyan,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Camera Permission Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "EyeGesture processes camera frames 100% on-device to track face, eyes, blinks, and gaze direction. No frames are stored or transmitted.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EyeEmerald,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("grant_camera_permission_button")
                ) {
                    Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
