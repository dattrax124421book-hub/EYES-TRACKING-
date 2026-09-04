package com.example.tracking

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import android.widget.Toast
import com.example.data.model.ActionChainStep
import com.example.data.model.ActionType
import com.example.data.model.GestureAction
import com.example.data.model.GestureHistoryItem
import com.example.data.model.GestureType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Executes legitimate Android automation actions triggered by recognized eye gestures.
 * Handles cooldowns, gesture lock, confirmation requests, action chains, and history logging.
 */
class ActionExecutionEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onLogHistory: (GestureHistoryItem) -> Unit,
    private val onRequestConfirmation: (GestureAction, GestureType) -> Unit
) {
    var isGestureLocked: Boolean = false
    var isTestMode: Boolean = false

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private var isTorchOn = false

    // Tracking cooldown timestamps per gesture
    private val cooldownTimestamps = mutableMapOf<GestureType, Long>()

    init {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                isTtsReady = true
            }
        }
    }

    fun handleGesture(
        gesture: GestureType,
        action: GestureAction?,
        profileName: String
    ) {
        val now = System.currentTimeMillis()

        // 1. Gesture Lock Check
        if (isGestureLocked) {
            triggerHapticFeedback(50)
            onLogHistory(
                GestureHistoryItem(
                    gestureName = gesture.displayName,
                    actionName = action?.actionType?.displayName ?: "No Action",
                    profileName = profileName,
                    status = "LOCKED"
                )
            )
            return
        }

        // 2. Test Mode Check
        if (isTestMode) {
            triggerHapticFeedback(80)
            onLogHistory(
                GestureHistoryItem(
                    gestureName = gesture.displayName,
                    actionName = action?.actionType?.displayName ?: "Test (No Assigned Action)",
                    profileName = profileName,
                    status = "TEST_MODE"
                )
            )
            return
        }

        if (action == null || !action.isEnabled) {
            // No action configured for this gesture
            onLogHistory(
                GestureHistoryItem(
                    gestureName = gesture.displayName,
                    actionName = "Unassigned",
                    profileName = profileName,
                    status = "UNASSIGNED"
                )
            )
            return
        }

        // 3. Cooldown Check
        val lastExecuted = cooldownTimestamps[gesture] ?: 0L
        val cooldownMs = action.cooldownSeconds * 1000L
        if (now - lastExecuted < cooldownMs) {
            onLogHistory(
                GestureHistoryItem(
                    gestureName = gesture.displayName,
                    actionName = action.actionType.displayName,
                    profileName = profileName,
                    status = "COOLDOWN"
                )
            )
            return
        }

        // 4. Confirmation Check
        if (action.requiresConfirmation) {
            onRequestConfirmation(action, gesture)
            return
        }

        // 5. Execute Action
        executeActionDirectly(action, gesture, profileName)
    }

    fun executeActionDirectly(
        action: GestureAction,
        gesture: GestureType,
        profileName: String
    ) {
        cooldownTimestamps[gesture] = System.currentTimeMillis()

        scope.launch(Dispatchers.Main) {
            if (action.delaySeconds > 0) {
                delay((action.delaySeconds * 1000).toLong())
            }

            if (action.isChain && action.chainStepsJson.isNotEmpty()) {
                executeActionChain(action.chainStepsJson, gesture, profileName)
            } else {
                performSingleAction(action.actionType, action.targetPackageName, action.customPhrase)
                onLogHistory(
                    GestureHistoryItem(
                        gestureName = gesture.displayName,
                        actionName = action.actionType.displayName + (action.targetAppLabel?.let { " ($it)" } ?: ""),
                        profileName = profileName,
                        status = "EXECUTED"
                    )
                )
            }
        }
    }

    private suspend fun executeActionChain(
        chainJson: String,
        gesture: GestureType,
        profileName: String
    ) {
        val steps = parseChainSteps(chainJson)
        var executedCount = 0

        for (step in steps) {
            if (!step.isEnabled) continue
            performSingleAction(step.actionType, step.targetPackageName, step.customPhrase)
            executedCount++
            if (step.delayAfterSeconds > 0) {
                delay((step.delayAfterSeconds * 1000).toLong())
            }
        }

        onLogHistory(
            GestureHistoryItem(
                gestureName = gesture.displayName,
                actionName = "Chain (${executedCount} steps executed)",
                profileName = profileName,
                status = "EXECUTED"
            )
        )
    }

    private fun performSingleAction(
        actionType: ActionType,
        targetPackage: String?,
        customPhrase: String?
    ) {
        try {
            when (actionType) {
                ActionType.LAUNCH_CAMERA -> {
                    triggerHapticFeedback(100)
                    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "Launching Camera", Toast.LENGTH_SHORT).show()
                    }
                }

                ActionType.OPEN_APP -> {
                    triggerHapticFeedback(100)
                    if (!targetPackage.isNullOrEmpty()) {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
                        if (launchIntent != null) {
                            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(launchIntent)
                        } else {
                            Toast.makeText(context, "App not found: $targetPackage", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                ActionType.OPEN_GALLERY -> {
                    triggerHapticFeedback(80)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        type = "image/*"
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "Opening Gallery", Toast.LENGTH_SHORT).show()
                    }
                }

                ActionType.OPEN_BROWSER -> {
                    triggerHapticFeedback(80)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }

                ActionType.OPEN_SETTINGS -> {
                    triggerHapticFeedback(80)
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }

                ActionType.MEDIA_PLAY_PAUSE -> {
                    triggerHapticFeedback(120)
                    val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    val eventUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    audioManager?.dispatchMediaKeyEvent(eventDown)
                    audioManager?.dispatchMediaKeyEvent(eventUp)
                    Toast.makeText(context, "Media Play/Pause toggled", Toast.LENGTH_SHORT).show()
                }

                ActionType.VOLUME_UP -> {
                    triggerHapticFeedback(60)
                    audioManager?.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                }

                ActionType.VOLUME_DOWN -> {
                    triggerHapticFeedback(60)
                    audioManager?.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                }

                ActionType.FLASHLIGHT_TOGGLE -> {
                    triggerHapticFeedback(100)
                    toggleFlashlight()
                }

                ActionType.HAPTIC_PULSE -> {
                    triggerHapticFeedback(250)
                }

                ActionType.SPEAK_NOTIFICATION -> {
                    val phrase = if (!customPhrase.isNullOrBlank()) customPhrase else "Gesture command recognized"
                    if (isTtsReady) {
                        textToSpeech?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "eye_tts")
                    } else {
                        Toast.makeText(context, phrase, Toast.LENGTH_SHORT).show()
                    }
                }

                ActionType.GO_BACK_HINT -> {
                    triggerHapticFeedback(80)
                    Toast.makeText(context, "Navigation Back signal triggered", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Action error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFlashlight() {
        try {
            cameraManager?.let { manager ->
                val cameraId = manager.cameraIdList.firstOrNull { id ->
                    manager.getCameraCharacteristics(id).get(
                        android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE
                    ) == true
                }
                if (cameraId != null) {
                    isTorchOn = !isTorchOn
                    manager.setTorchMode(cameraId, isTorchOn)
                    val status = if (isTorchOn) "Flashlight ON" else "Flashlight OFF"
                    Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    private fun triggerHapticFeedback(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            // Ignore if vibration disallowed
        }
    }

    private fun parseChainSteps(jsonStr: String): List<ActionChainStep> {
        val steps = mutableListOf<ActionChainStep>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val type = ActionType.valueOf(obj.getString("actionType"))
                val targetPkg = if (obj.has("targetPackageName")) obj.getString("targetPackageName") else null
                val targetLabel = if (obj.has("targetAppLabel")) obj.getString("targetAppLabel") else null
                val phrase = if (obj.has("customPhrase")) obj.getString("customPhrase") else null
                val delay = if (obj.has("delayAfterSeconds")) obj.getDouble("delayAfterSeconds").toFloat() else 1.0f
                val enabled = if (obj.has("isEnabled")) obj.getBoolean("isEnabled") else true

                steps.add(
                    ActionChainStep(
                        stepOrder = i,
                        actionType = type,
                        targetPackageName = targetPkg,
                        targetAppLabel = targetLabel,
                        customPhrase = phrase,
                        delayAfterSeconds = delay,
                        isEnabled = enabled
                    )
                )
            }
        } catch (e: Exception) {
            // Fallback empty
        }
        return steps
    }

    fun release() {
        try {
            if (isTorchOn) {
                toggleFlashlight()
            }
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
