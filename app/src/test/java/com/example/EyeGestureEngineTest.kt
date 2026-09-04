package com.example

import com.example.data.model.ActionProfile
import com.example.data.model.ActionType
import com.example.data.model.CalibrationData
import com.example.data.model.GestureAction
import com.example.data.model.GestureType
import com.example.data.model.LiveTrackingState
import com.example.tracking.GestureDetectorEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EyeGestureEngineTest {

    @Test
    fun `test gesture enum values and categories`() {
        assertEquals("Blink", GestureType.SINGLE_BLINK.category)
        assertEquals("3 × Blink", GestureType.TRIPLE_BLINK.displayName)
        assertEquals("Gaze", GestureType.LOOK_LEFT.category)
        assertEquals("Combined", GestureType.LOOK_RIGHT_AND_BLINK.category)
    }

    @Test
    fun `test action types available`() {
        assertNotNull(ActionType.LAUNCH_CAMERA)
        assertNotNull(ActionType.OPEN_APP)
        assertNotNull(ActionType.MEDIA_PLAY_PAUSE)
        assertNotNull(ActionType.FLASHLIGHT_TOGGLE)
        assertNotNull(ActionType.HAPTIC_PULSE)
        assertNotNull(ActionType.SPEAK_NOTIFICATION)
    }

    @Test
    fun `test gaze direction detection`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        var recognizedGesture: GestureType? = null
        val detector = GestureDetectorEngine(testScope) { gesture ->
            recognizedGesture = gesture
        }

        val cal = CalibrationData(
            lookLeftThresholdAngle = -12f,
            lookRightThresholdAngle = 12f
        )
        detector.calibration = cal

        // Simulate looking left with significant negative yaw
        val lookLeftFrame = LiveTrackingState(
            isTrackingActive = true,
            isFaceDetected = true,
            leftEyeOpenProb = 0.9f,
            rightEyeOpenProb = 0.9f,
            headEulerYaw = -20.0f,
            headEulerPitch = 0f
        )

        val baseTime = 100000L
        detector.processTrackingFrame(lookLeftFrame, baseTime)
        advanceTimeBy(350)

        // Return gaze to neutral center
        val neutralFrame = LiveTrackingState(
            isTrackingActive = true,
            isFaceDetected = true,
            leftEyeOpenProb = 0.9f,
            rightEyeOpenProb = 0.9f,
            headEulerYaw = 0.0f,
            headEulerPitch = 0f
        )
        detector.processTrackingFrame(neutralFrame, baseTime + 350L)

        assertEquals(GestureType.LOOK_LEFT, recognizedGesture)
    }

    @Test
    fun `test action profile model defaults`() {
        val defaultProfile = ActionProfile(
            id = 1,
            name = "Accessibility",
            description = "High-visibility eye controls",
            isDefault = true
        )
        assertTrue(defaultProfile.isDefault)
        assertEquals("Accessibility", defaultProfile.name)
    }

    @Test
    fun `test duplicate action detection logic`() {
        val actions = listOf(
            GestureAction(
                id = 10,
                profileId = 1,
                gestureType = GestureType.TRIPLE_BLINK,
                actionType = ActionType.LAUNCH_CAMERA
            )
        )

        val newPendingAction = GestureAction(
            id = 0,
            profileId = 1,
            gestureType = GestureType.TRIPLE_BLINK,
            actionType = ActionType.FLASHLIGHT_TOGGLE
        )

        val conflict = actions.firstOrNull {
            it.gestureType == newPendingAction.gestureType && it.id != newPendingAction.id
        }

        assertNotNull(conflict)
        assertEquals(ActionType.LAUNCH_CAMERA, conflict?.actionType)
    }
}
