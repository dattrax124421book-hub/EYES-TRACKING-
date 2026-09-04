package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EyeCyan
import com.example.ui.theme.EyeEmerald
import com.example.viewmodel.ConfirmationPromptState
import com.example.viewmodel.DuplicateConflictState
import com.example.viewmodel.GestureFeedbackState

/**
 * Temporary feedback panel displaying recognized gesture and assigned action (Section 4).
 */
@Composable
fun GestureFeedbackPanel(
    feedbackState: GestureFeedbackState,
    onTestClick: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = feedbackState.isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF0101C2E)),
            border = BorderStroke(1.5.dp, EyeEmerald),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("gesture_feedback_panel")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Recognized",
                            tint = EyeEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gesture Detected",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EyeEmerald
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x3300E676)
                    ) {
                        Text(
                            text = "CONFIRMED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EyeEmerald,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "\"${feedbackState.gestureType.displayName}\"",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Possible assigned action:",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                Text(
                    text = feedbackState.actionDescription,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EyeCyan
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onTestClick,
                        border = BorderStroke(1.dp, EyeCyan),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EyeCyan),
                        modifier = Modifier.testTag("feedback_test_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TEST", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onDoneClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EyeEmerald,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.testTag("feedback_done_button")
                    ) {
                        Text("DONE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Duplicate Action Conflict Dialog (Section 7).
 */
@Composable
fun DuplicateActionConflictDialog(
    conflictState: DuplicateConflictState,
    onEditExisting: () -> Unit,
    onReplace: () -> Unit,
    onCancel: () -> Unit
) {
    if (!conflictState.isConflict || conflictState.pendingAction == null) return

    val gestureName = conflictState.pendingAction.gestureType.displayName
    val existingActionLabel = conflictState.existingAction?.actionType?.displayName
        ?: conflictState.existingAction?.targetAppLabel ?: "Another Action"

    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Conflict Warning",
                tint = Color(0xFFFFD600),
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "⚠️ This Action Is Already Used",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column {
                Text(
                    text = "\"$gestureName\" is already assigned to $existingActionLabel.",
                    fontSize = 14.sp,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Would you like to replace the existing assignment, edit it, or cancel?",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onReplace,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EyeEmerald,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("conflict_replace_button")
            ) {
                Text("Replace", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("conflict_cancel_button")
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onEditExisting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E2E48),
                        contentColor = EyeCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("conflict_edit_button")
                ) {
                    Text("Edit Existing")
                }
            }
        },
        containerColor = Color(0xFF10192A),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("duplicate_conflict_dialog")
    )
}

/**
 * Confirmation Dialog for high-impact actions (Section 9).
 */
@Composable
fun ActionConfirmationDialog(
    promptState: ConfirmationPromptState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    if (!promptState.isPending || promptState.action == null) return

    val actionName = promptState.action.actionType.displayName +
            (promptState.action.targetAppLabel?.let { " ($it)" } ?: "")
    val gestureName = promptState.gesture?.displayName ?: "Eye Gesture"

    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Confirmation",
                tint = EyeCyan,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Run $actionName?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Text(
                text = "Detected \"$gestureName\". Execute this action now?",
                fontSize = 14.sp,
                color = Color(0xFFCBD5E1)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EyeEmerald,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_yes_button")
            ) {
                Text("YES", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_cancel_button")
            ) {
                Text("CANCEL")
            }
        },
        containerColor = Color(0xFF10192A),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("action_confirmation_dialog")
    )
}
