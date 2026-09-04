package com.example.tracking

import com.example.data.model.CalibrationData
import com.example.data.model.GestureType
import com.example.data.model.LiveTrackingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Robust gesture detector distinguishing deliberate eye/gaze gestures
 * from natural involuntary blinking and casual head movements.
 */
class GestureDetectorEngine(
    private val scope: CoroutineScope,
    private val onGestureRecognized: (GestureType) -> Unit
) {
    var calibration: CalibrationData = CalibrationData()

    // Blink detection states
    private var leftEyeWasClosed = false
    private var rightEyeWasClosed = false
    private var bothEyesClosedStartTime = 0L
    private var leftEyeWinkStartTime = 0L
    private var rightEyeWinkStartTime = 0L

    // Blink counter state machine
    private var currentBlinkCount = 0
    private var lastBlinkCompletedTime = 0L
    private var blinkSequenceJob: Job? = null

    // Gaze direction tracking
    private var currentGazeDirection: GestureType? = null
    private var gazeDirectionStartTime = 0L
    private var gazeHeldEmitted = false

    // Sliding event history for combination gestures
    private data class RecentEvent(val type: GestureType, val timestamp: Long)
    private val recentEvents = mutableListOf<RecentEvent>()

    // Global debounce to prevent repeating triggers
    private var lastGestureEmittedTimestamp = 0L
    private val globalDebounceMs = 600L

    fun processTrackingFrame(state: LiveTrackingState, currentTime: Long = System.currentTimeMillis()) {
        if (!state.isFaceDetected) {
            resetTransitoryStates()
            return
        }

        if (currentTime - lastGestureEmittedTimestamp < globalDebounceMs) {
            return
        }

        val blinkThreshold = calibration.blinkThreshold
        val leftWinkThreshold = calibration.leftWinkThreshold
        val rightWinkThreshold = calibration.rightWinkThreshold

        val leftClosed = state.leftEyeOpenProb < blinkThreshold
        val rightClosed = state.rightEyeOpenProb < blinkThreshold
        val bothClosed = leftClosed && rightClosed

        val leftWink = state.leftEyeOpenProb < leftWinkThreshold && state.rightEyeOpenProb > 0.65f
        val rightWink = state.rightEyeOpenProb < rightWinkThreshold && state.leftEyeOpenProb > 0.65f

        // 1. Process Winks (Left / Right Eye Blink)
        if (leftWink) {
            if (leftEyeWinkStartTime == 0L) leftEyeWinkStartTime = currentTime
        } else {
            if (leftEyeWinkStartTime > 0L) {
                val duration = currentTime - leftEyeWinkStartTime
                if (duration in 180..850) {
                    emitGesture(GestureType.LEFT_EYE_BLINK)
                }
                leftEyeWinkStartTime = 0L
            }
        }

        if (rightWink) {
            if (rightEyeWinkStartTime == 0L) rightEyeWinkStartTime = currentTime
        } else {
            if (rightEyeWinkStartTime > 0L) {
                val duration = currentTime - rightEyeWinkStartTime
                if (duration in 180..850) {
                    emitGesture(GestureType.RIGHT_EYE_BLINK)
                }
                rightEyeWinkStartTime = 0L
            }
        }

        // 2. Process Both-Eyes Blinks (Single, Double, Triple, Both-Eyes Long)
        if (bothClosed) {
            if (bothEyesClosedStartTime == 0L) {
                bothEyesClosedStartTime = currentTime
            }
        } else {
            if (bothEyesClosedStartTime > 0L) {
                val closedDuration = currentTime - bothEyesClosedStartTime
                bothEyesClosedStartTime = 0L

                // Long blink check (700ms - 2000ms)
                if (closedDuration in 700..2000) {
                    emitGesture(GestureType.BOTH_EYES_BLINK)
                    currentBlinkCount = 0
                    blinkSequenceJob?.cancel()
                } else if (closedDuration in 120..650) {
                    // Valid deliberate blink
                    currentBlinkCount++
                    lastBlinkCompletedTime = currentTime

                    // Cancel previous finalize job and start a new evaluation timer
                    blinkSequenceJob?.cancel()
                    blinkSequenceJob = scope.launch(Dispatchers.Default) {
                        delay(450) // Inter-blink timeout
                        finalizeBlinkSequence()
                    }
                }
            }
        }

        // 3. Process Gaze Direction & Hold Gaze
        val yaw = state.headEulerYaw
        val pitch = state.headEulerPitch

        val detectedDirection: GestureType? = when {
            yaw < calibration.lookLeftThresholdAngle -> GestureType.LOOK_LEFT
            yaw > calibration.lookRightThresholdAngle -> GestureType.LOOK_RIGHT
            pitch < calibration.lookUpThresholdAngle -> GestureType.LOOK_UP
            pitch > calibration.lookDownThresholdAngle -> GestureType.LOOK_DOWN
            else -> null
        }

        if (detectedDirection != null) {
            if (currentGazeDirection != detectedDirection) {
                currentGazeDirection = detectedDirection
                gazeDirectionStartTime = currentTime
                gazeHeldEmitted = false
                // Record the quick gaze for combinations
                recordRecentEvent(detectedDirection, currentTime)
            } else {
                // Check for Hold Gaze (> 950ms)
                if (!gazeHeldEmitted && currentTime - gazeDirectionStartTime > 950) {
                    val holdGesture = when (detectedDirection) {
                        GestureType.LOOK_LEFT -> GestureType.HOLD_GAZE_LEFT
                        GestureType.LOOK_RIGHT -> GestureType.HOLD_GAZE_RIGHT
                        GestureType.LOOK_UP -> GestureType.HOLD_GAZE_UP
                        GestureType.LOOK_DOWN -> GestureType.HOLD_GAZE_DOWN
                        else -> null
                    }
                    if (holdGesture != null) {
                        gazeHeldEmitted = true
                        emitGesture(holdGesture, currentTime)
                    }
                }
            }
        } else {
            // Neutral gaze
            if (currentGazeDirection != null) {
                // If looked in direction for 250..900ms and returned, emit Look Direction gesture
                val gazeDuration = currentTime - gazeDirectionStartTime
                if (!gazeHeldEmitted && gazeDuration in 250..900) {
                    emitGesture(currentGazeDirection!!, currentTime)
                }
                currentGazeDirection = null
                gazeDirectionStartTime = 0L
                gazeHeldEmitted = false
            }
        }

        leftEyeWasClosed = leftClosed
        rightEyeWasClosed = rightClosed
    }

    private fun finalizeBlinkSequence() {
        val count = currentBlinkCount
        currentBlinkCount = 0

        val gesture = when (count) {
            1 -> GestureType.SINGLE_BLINK
            2 -> GestureType.DOUBLE_BLINK
            3 -> GestureType.TRIPLE_BLINK
            4 -> GestureType.QUAD_BLINK
            else -> if (count >= 5) GestureType.QUAD_BLINK else null
        }

        gesture?.let { emitGesture(it) }
    }

    private fun recordRecentEvent(type: GestureType, now: Long = System.currentTimeMillis()) {
        val windowMs = (calibration.combinationWindowSeconds * 1000).toLong()
        recentEvents.removeAll { now - it.timestamp > windowMs }
        recentEvents.add(RecentEvent(type, now))
    }

    private fun emitGesture(gesture: GestureType, now: Long = System.currentTimeMillis()) {
        recordRecentEvent(gesture, now)

        // Check combination patterns within combination window
        val windowMs = (calibration.combinationWindowSeconds * 1000).toLong()
        val activeEvents = recentEvents.filter { now - it.timestamp <= windowMs }

        var resolvedGesture = gesture

        // Check for 3-step combination: Look Right -> Look Left -> Blink
        if (activeEvents.size >= 3) {
            val lastThree = activeEvents.takeLast(3).map { it.type }
            if (lastThree == listOf(GestureType.LOOK_RIGHT, GestureType.LOOK_LEFT, GestureType.SINGLE_BLINK)) {
                resolvedGesture = GestureType.LOOK_RIGHT_LOOK_LEFT_BLINK
                recentEvents.clear()
            }
        }

        // Check for 2-step combinations:
        // "Look Right + Blink"
        if (resolvedGesture == gesture && activeEvents.size >= 2) {
            val lastTwo = activeEvents.takeLast(2).map { it.type }
            if (lastTwo == listOf(GestureType.LOOK_RIGHT, GestureType.SINGLE_BLINK)) {
                resolvedGesture = GestureType.LOOK_RIGHT_AND_BLINK
                recentEvents.clear()
            } else if (lastTwo == listOf(GestureType.LOOK_LEFT, GestureType.DOUBLE_BLINK)) {
                resolvedGesture = GestureType.LOOK_LEFT_AND_DOUBLE_BLINK
                recentEvents.clear()
            }
        }

        lastGestureEmittedTimestamp = now
        onGestureRecognized(resolvedGesture)
    }

    private fun resetTransitoryStates() {
        leftEyeWinkStartTime = 0L
        rightEyeWinkStartTime = 0L
        bothEyesClosedStartTime = 0L
        currentGazeDirection = null
        gazeDirectionStartTime = 0L
        gazeHeldEmitted = false
        currentBlinkCount = 0
        blinkSequenceJob?.cancel()
    }

    fun reset() {
        resetTransitoryStates()
        recentEvents.clear()
    }
}
