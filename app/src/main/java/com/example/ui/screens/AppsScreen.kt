package com.example.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.model.ActionType
import com.example.data.model.GestureAction
import com.example.data.model.GestureType
import com.example.ui.components.DuplicateActionConflictDialog
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EyeCyan
import com.example.ui.theme.EyeEmerald
import com.example.viewmodel.EyeGestureViewModel
import com.example.viewmodel.InstalledAppInfo

@Composable
fun AppsScreen(
    viewModel: EyeGestureViewModel,
    modifier: Modifier = Modifier
) {
    val apps by viewModel.installedApps.collectAsState()
    val isLoading by viewModel.isLoadingApps.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val duplicateConflict by viewModel.duplicateConflict.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedAppForConfig by remember { mutableStateOf<InstalledAppInfo?>(null) }
    var showAddAppActionDialog by remember { mutableStateOf(false) }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("apps_screen")) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Installed App Library",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Assign eye gestures to launch and automate apps",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF16253B),
                    border = BorderStroke(1.dp, EyeCyan)
                ) {
                    Text(
                        text = "${apps.size} Apps",
                        color = EyeCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search installed applications...", color = Color(0xFF64748B)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = EyeCyan)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EyeEmerald,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("app_search_field")
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EyeEmerald)
                }
            } else if (filteredApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No applications match your search.", color = Color(0xFF94A3B8), fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppListItem(
                            app = app,
                            onClick = { selectedAppForConfig = app }
                        )
                    }
                }
            }
        }

        // App Configuration Sheet / Modal (Section 6)
        selectedAppForConfig?.let { app ->
            AppActionConfigDialog(
                app = app,
                onAddAction = {
                    showAddAppActionDialog = true
                },
                onDeleteAction = { action ->
                    viewModel.deleteAction(action)
                },
                onDismiss = {
                    selectedAppForConfig = null
                }
            )
        }

        // Add Eye Action specifically for this App
        if (showAddAppActionDialog && selectedAppForConfig != null) {
            val app = selectedAppForConfig!!
            AddAppSpecificActionDialog(
                app = app,
                activeProfileId = activeProfile?.id ?: 1L,
                onSave = { newAction ->
                    showAddAppActionDialog = false
                    viewModel.saveOrUpdateActionWithDuplicateCheck(newAction) {
                        // Success
                    }
                },
                onDismiss = { showAddAppActionDialog = false }
            )
        }

        // Duplicate Action Conflict Dialog
        DuplicateActionConflictDialog(
            conflictState = duplicateConflict,
            onEditExisting = {
                viewModel.dismissConflict()
            },
            onReplace = {
                viewModel.resolveConflictReplace()
            },
            onCancel = {
                viewModel.dismissConflict()
            }
        )
    }
}

@Composable
private fun AppListItem(
    app: InstalledAppInfo,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, if (app.configuredActions.isNotEmpty()) EyeEmerald else DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("app_item_${app.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            if (app.icon != null) {
                val bitmap = remember(app.icon) {
                    try {
                        app.icon.toBitmap(80, 80).asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.label,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = EyeCyan,
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = EyeCyan,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (app.configuredActions.isNotEmpty()) {
                        "${app.configuredActions.size} Eye Gesture(s) configured"
                    } else {
                        app.packageName
                    },
                    fontSize = 12.sp,
                    color = if (app.configuredActions.isNotEmpty()) EyeEmerald else Color(0xFF64748B)
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Configure",
                tint = if (app.configuredActions.isNotEmpty()) EyeEmerald else Color(0xFF64748B),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Dialog showing Current Eye Actions for selected App and "+ Add Action" (Section 6).
 */
@Composable
private fun AppActionConfigDialog(
    app: InstalledAppInfo,
    onAddAction: () -> Unit,
    onDeleteAction: (GestureAction) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.label,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Current Eye Actions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EyeCyan
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (app.configuredActions.isEmpty()) {
                    Text(
                        text = "No eye actions currently mapped to launch this app.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                } else {
                    app.configuredActions.forEach { action ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceElevated,
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "\"${action.gestureType.displayName}\" → Open",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )

                                IconButton(
                                    onClick = { onDeleteAction(action) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAddAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EyeEmerald,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_add_action_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Add Action", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = EyeCyan)
            }
        },
        containerColor = Color(0xFF10192A),
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Dialog to bind an eye gesture specifically to launching this App.
 */
@Composable
private fun AddAppSpecificActionDialog(
    app: InstalledAppInfo,
    activeProfileId: Long,
    onSave: (GestureAction) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedGesture by remember { mutableStateOf(GestureType.TRIPLE_BLINK) }
    var cooldown by remember { mutableStateOf(3) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Map Eye Gesture to ${app.label}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select Trigger Gesture:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(GestureType.values()) { gesture ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedGesture == gesture) Color(0x3300E676) else DarkSurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (selectedGesture == gesture) EyeEmerald else DarkBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { selectedGesture = gesture }
                        ) {
                            Text(
                                text = "${gesture.category}: ${gesture.displayName}",
                                color = if (selectedGesture == gesture) EyeEmerald else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (selectedGesture == gesture) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        GestureAction(
                            profileId = activeProfileId,
                            gestureType = selectedGesture,
                            actionType = ActionType.OPEN_APP,
                            targetPackageName = app.packageName,
                            targetAppLabel = app.label,
                            isEnabled = true,
                            cooldownSeconds = cooldown
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EyeEmerald, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Assign Gesture", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Cancel")
            }
        },
        containerColor = Color(0xFF10192A),
        shape = RoundedCornerShape(16.dp)
    )
}
