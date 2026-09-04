package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActionChainStep
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
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun GesturesScreen(
    viewModel: EyeGestureViewModel,
    onNavigateToApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actions by viewModel.profileActions.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val isTestMode by viewModel.isTestMode.collectAsState()
    val testSuccessMsg by viewModel.testSuccessMessage.collectAsState()
    val duplicateConflict by viewModel.duplicateConflict.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Gestures, 1: Action Chains, 2: Test Mode
    var showActionBuilderDialog by remember { mutableStateOf(false) }
    var editingAction by remember { mutableStateOf<GestureAction?>(null) }
    var isBuildingChain by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().testTag("gestures_screen")) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Gesture Actions",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Profile: ${activeProfile?.name ?: "Accessibility"}",
                        fontSize = 12.sp,
                        color = EyeCyan
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isTestMode) Color(0x3300E5FF) else Color(0x22FFFFFF),
                    modifier = Modifier.clickable { viewModel.toggleTestMode() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = if (isTestMode) EyeCyan else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTestMode) "Test Active" else "Test Mode",
                            color = if (isTestMode) EyeCyan else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = EyeEmerald,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = EyeEmerald
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Standard Actions (${actions.filter { !it.isChain }.size})", fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_standard_actions")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Action Chains (${actions.filter { it.isChain }.size})", fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_action_chains")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Test Lab", fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_test_lab")
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> StandardActionsTab(
                    actions = actions.filter { !it.isChain },
                    onEditAction = { action ->
                        editingAction = action
                        isBuildingChain = false
                        showActionBuilderDialog = true
                    },
                    onDeleteAction = { viewModel.deleteAction(it) },
                    onToggleEnabled = { viewModel.toggleActionEnabled(it) }
                )

                1 -> ActionChainsTab(
                    chains = actions.filter { it.isChain },
                    onEditChain = { chain ->
                        editingAction = chain
                        isBuildingChain = true
                        showActionBuilderDialog = true
                    },
                    onDeleteChain = { viewModel.deleteAction(it) },
                    onToggleEnabled = { viewModel.toggleActionEnabled(it) }
                )

                2 -> GestureTestLabTab(
                    viewModel = viewModel,
                    testSuccessMsg = testSuccessMsg
                )
            }
        }

        // Floating Action Button to Add New Action or Chain
        if (selectedTab != 2) {
            FloatingActionButton(
                onClick = {
                    editingAction = null
                    isBuildingChain = (selectedTab == 1)
                    showActionBuilderDialog = true
                },
                containerColor = EyeEmerald,
                contentColor = Color.Black,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("add_gesture_action_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Gesture Action"
                )
            }
        }

        // Action Builder Dialog (Section 5 & Section 8)
        if (showActionBuilderDialog) {
            ActionBuilderDialog(
                editingAction = editingAction,
                isChain = isBuildingChain,
                activeProfileId = activeProfile?.id ?: 1L,
                installedApps = installedApps,
                onSave = { newAction ->
                    showActionBuilderDialog = false
                    viewModel.saveOrUpdateActionWithDuplicateCheck(newAction) {
                        editingAction = null
                    }
                },
                onDismiss = {
                    showActionBuilderDialog = false
                    editingAction = null
                }
            )
        }

        // Duplicate Action Conflict Dialog (Section 7)
        DuplicateActionConflictDialog(
            conflictState = duplicateConflict,
            onEditExisting = {
                val existing = duplicateConflict.existingAction
                viewModel.dismissConflict()
                if (existing != null) {
                    editingAction = existing
                    isBuildingChain = existing.isChain
                    showActionBuilderDialog = true
                }
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
private fun StandardActionsTab(
    actions: List<GestureAction>,
    onEditAction: (GestureAction) -> Unit,
    onDeleteAction: (GestureAction) -> Unit,
    onToggleEnabled: (GestureAction) -> Unit
) {
    if (actions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No standard actions configured.\nTap '+' below to assign an eye gesture!",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(actions, key = { it.id }) { action ->
                GestureActionCard(
                    action = action,
                    onEdit = { onEditAction(action) },
                    onDelete = { onDeleteAction(action) },
                    onToggleEnabled = { onToggleEnabled(action) }
                )
            }
        }
    }
}

@Composable
private fun ActionChainsTab(
    chains: List<GestureAction>,
    onEditChain: (GestureAction) -> Unit,
    onDeleteChain: (GestureAction) -> Unit,
    onToggleEnabled: (GestureAction) -> Unit
) {
    if (chains.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = EyeCyan,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Action Chains",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "One gesture can execute multiple supported actions sequentially.\nExample: \"3 × Blink\" → Open Camera → Wait 1s → Vibrate.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chains, key = { it.id }) { chain ->
                GestureActionCard(
                    action = chain,
                    onEdit = { onEditChain(chain) },
                    onDelete = { onDeleteChain(chain) },
                    onToggleEnabled = { onToggleEnabled(chain) }
                )
            }
        }
    }
}

@Composable
private fun GestureActionCard(
    action: GestureAction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (action.isEnabled) DarkSurfaceElevated else DarkSurface
        ),
        border = BorderStroke(1.dp, if (action.isEnabled) DarkBorder else Color(0x22FFFFFF)),
        modifier = Modifier.fillMaxWidth().testTag("gesture_action_card_${action.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x3300E676)
                    ) {
                        Text(
                            text = action.gestureType.category.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EyeEmerald,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = action.gestureType.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (action.isEnabled) Color.White else Color.Gray
                    )
                }

                Switch(
                    checked = action.isEnabled,
                    onCheckedChange = { onToggleEnabled() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EyeEmerald,
                        uncheckedThumbColor = Color(0xFF64748B),
                        uncheckedTrackColor = Color(0xFF1E293B)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Action: " + action.actionType.displayName +
                        (action.targetAppLabel?.let { " ($it)" } ?: ""),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (action.isEnabled) EyeCyan else Color.Gray
            )

            if (action.isChain && action.chainStepsJson.isNotEmpty()) {
                Text(
                    text = "Sequential Chain Enabled",
                    fontSize = 12.sp,
                    color = Color(0xFFFFD600)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cooldown: ${action.cooldownSeconds}s • Delay: ${action.delaySeconds}s • Confirm: ${if (action.requiresConfirmation) "ON" else "OFF"}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Action",
                            tint = EyeCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Action",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Gesture Test Lab (Section 10).
 */
@Composable
private fun GestureTestLabTab(
    viewModel: EyeGestureViewModel,
    testSuccessMsg: String?
) {
    val lastGesture by viewModel.lastDetectedGesture.collectAsState()
    val isTestMode by viewModel.isTestMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = BorderStroke(1.5.dp, EyeCyan),
            modifier = Modifier.fillMaxWidth().testTag("gesture_test_mode_card")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = null,
                    tint = EyeCyan,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Gesture Recognition Tester",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Perform gestures in front of the camera. The recognizer verifies your motion without executing any system actions.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.toggleTestMode() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTestMode) Color(0xFFFF5252) else EyeEmerald,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isTestMode) "Exit Test Mode" else "Activate Test Mode",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Latest Gesture Evaluated:",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = lastGesture?.displayName ?: "No gesture detected yet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (lastGesture != null) EyeEmerald else Color.Gray
                )

                if (!testSuccessMsg.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x3300E676),
                        border = BorderStroke(1.dp, EyeEmerald),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = testSuccessMsg,
                            color = EyeEmerald,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Action & Action Chain Builder Dialog (Section 5 & Section 8).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionBuilderDialog(
    editingAction: GestureAction?,
    isChain: Boolean,
    activeProfileId: Long,
    installedApps: List<com.example.viewmodel.InstalledAppInfo>,
    onSave: (GestureAction) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedGesture by remember { mutableStateOf(editingAction?.gestureType ?: GestureType.TRIPLE_BLINK) }
    var selectedActionType by remember { mutableStateOf(editingAction?.actionType ?: ActionType.LAUNCH_CAMERA) }
    var selectedPackage by remember { mutableStateOf(editingAction?.targetPackageName) }
    var selectedAppLabel by remember { mutableStateOf(editingAction?.targetAppLabel) }
    var customPhrase by remember { mutableStateOf(editingAction?.customPhrase ?: "Command recognized") }
    var cooldownSeconds by remember { mutableIntStateOf(editingAction?.cooldownSeconds ?: 3) }
    var delaySeconds by remember { mutableFloatStateOf(editingAction?.delaySeconds ?: 0f) }
    var requiresConfirmation by remember { mutableStateOf(editingAction?.requiresConfirmation ?: false) }

    // Action Chain steps state (Section 8)
    val chainSteps = remember {
        mutableStateListOf<ActionChainStep>().apply {
            if (isChain && editingAction?.chainStepsJson?.isNotEmpty() == true) {
                try {
                    val array = JSONArray(editingAction.chainStepsJson)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        add(
                            ActionChainStep(
                                stepOrder = i,
                                actionType = ActionType.valueOf(obj.getString("actionType")),
                                targetPackageName = if (obj.has("targetPackageName")) obj.getString("targetPackageName") else null,
                                targetAppLabel = if (obj.has("targetAppLabel")) obj.getString("targetAppLabel") else null,
                                customPhrase = if (obj.has("customPhrase")) obj.getString("customPhrase") else null,
                                delayAfterSeconds = if (obj.has("delayAfterSeconds")) obj.getDouble("delayAfterSeconds").toFloat() else 1.0f,
                                isEnabled = if (obj.has("isEnabled")) obj.getBoolean("isEnabled") else true
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Ignore fallback
                }
            } else if (isChain) {
                add(ActionChainStep(stepOrder = 0, actionType = ActionType.LAUNCH_CAMERA, delayAfterSeconds = 1.0f))
                add(ActionChainStep(stepOrder = 1, actionType = ActionType.HAPTIC_PULSE, delayAfterSeconds = 0f))
            }
        }
    }

    var gestureExpanded by remember { mutableStateOf(false) }
    var actionExpanded by remember { mutableStateOf(false) }
    var appPickerExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isChain) "Action Chain Builder" else (if (editingAction != null) "Edit Gesture Action" else "New Gesture Action"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Trigger Gesture Dropdown
                item {
                    Text("Trigger Eye Gesture", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    ExposedDropdownMenuBox(
                        expanded = gestureExpanded,
                        onExpandedChange = { gestureExpanded = !gestureExpanded }
                    ) {
                        OutlinedTextField(
                            value = "${selectedGesture.category}: ${selectedGesture.displayName}",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gestureExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EyeEmerald,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = gestureExpanded,
                            onDismissRequest = { gestureExpanded = false },
                            modifier = Modifier.background(DarkSurfaceElevated)
                        ) {
                            GestureType.values().forEach { gesture ->
                                DropdownMenuItem(
                                    text = { Text("${gesture.category}: ${gesture.displayName}", color = Color.White) },
                                    onClick = {
                                        selectedGesture = gesture
                                        gestureExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // If not chain: Primary Action Selector
                if (!isChain) {
                    item {
                        Text("Action To Execute", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        ExposedDropdownMenuBox(
                            expanded = actionExpanded,
                            onExpandedChange = { actionExpanded = !actionExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedActionType.displayName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EyeEmerald,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = actionExpanded,
                                onDismissRequest = { actionExpanded = false },
                                modifier = Modifier.background(DarkSurfaceElevated)
                            ) {
                                ActionType.values().forEach { action ->
                                    DropdownMenuItem(
                                        text = { Text(action.displayName, color = Color.White) },
                                        onClick = {
                                            selectedActionType = action
                                            actionExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // If OPEN_APP: Target App Dropdown
                    if (selectedActionType == ActionType.OPEN_APP) {
                        item {
                            Text("Target Installed App", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            ExposedDropdownMenuBox(
                                expanded = appPickerExpanded,
                                onExpandedChange = { appPickerExpanded = !appPickerExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedAppLabel ?: "Select launchable app",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = appPickerExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EyeCyan,
                                        unfocusedBorderColor = DarkBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = appPickerExpanded,
                                    onDismissRequest = { appPickerExpanded = false },
                                    modifier = Modifier.background(DarkSurfaceElevated)
                                ) {
                                    installedApps.forEach { app ->
                                        DropdownMenuItem(
                                            text = { Text(app.label, color = Color.White) },
                                            onClick = {
                                                selectedPackage = app.packageName
                                                selectedAppLabel = app.label
                                                appPickerExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // If SPEAK_NOTIFICATION: Custom phrase field
                    if (selectedActionType == ActionType.SPEAK_NOTIFICATION) {
                        item {
                            Text("Text To Speak (TTS)", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            OutlinedTextField(
                                value = customPhrase,
                                onValueChange = { customPhrase = it },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EyeCyan,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // Action Chain Steps Builder (Section 8)
                    item {
                        Text(
                            text = "Chain Sequence Steps:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EyeEmerald
                        )
                    }

                    items(chainSteps.size) { index ->
                        val step = chainSteps[index]
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Step ${index + 1}: ${step.actionType.displayName}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Wait ${step.delayAfterSeconds}s then next",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            if (chainSteps.size > 1) chainSteps.removeAt(index)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Step",
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                chainSteps.add(
                                    ActionChainStep(
                                        stepOrder = chainSteps.size,
                                        actionType = ActionType.HAPTIC_PULSE,
                                        delayAfterSeconds = 1.0f
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E2E48),
                                contentColor = EyeCyan
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Chain Step", fontSize = 12.sp)
                        }
                    }
                }

                // Cooldown Setting
                item {
                    Column {
                        Text("Cooldown: ${cooldownSeconds}s (Accidental trigger protection)", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Slider(
                            value = cooldownSeconds.toFloat(),
                            onValueChange = { cooldownSeconds = it.toInt() },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = EyeEmerald,
                                activeTrackColor = EyeEmerald
                            )
                        )
                    }
                }

                // Optional Delay
                item {
                    Column {
                        Text("Execution Delay: ${delaySeconds}s", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Slider(
                            value = delaySeconds,
                            onValueChange = { delaySeconds = it },
                            valueRange = 0f..5f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = EyeCyan,
                                activeTrackColor = EyeCyan
                            )
                        )
                    }
                }

                // Optional Confirmation Mode Toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Require Confirmation", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("Prompt YES/CANCEL before executing", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Switch(
                            checked = requiresConfirmation,
                            onCheckedChange = { requiresConfirmation = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EyeEmerald
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val chainJson = if (isChain) {
                        val array = JSONArray()
                        chainSteps.forEachIndexed { i, step ->
                            val obj = JSONObject().apply {
                                put("stepOrder", i)
                                put("actionType", step.actionType.name)
                                put("targetPackageName", step.targetPackageName)
                                put("targetAppLabel", step.targetAppLabel)
                                put("customPhrase", step.customPhrase)
                                put("delayAfterSeconds", step.delayAfterSeconds.toDouble())
                                put("isEnabled", step.isEnabled)
                            }
                            array.put(obj)
                        }
                        array.toString()
                    } else ""

                    val newAction = GestureAction(
                        id = editingAction?.id ?: 0L,
                        profileId = activeProfileId,
                        gestureType = selectedGesture,
                        actionType = if (isChain) (chainSteps.firstOrNull()?.actionType ?: ActionType.LAUNCH_CAMERA) else selectedActionType,
                        targetPackageName = selectedPackage,
                        targetAppLabel = selectedAppLabel,
                        customPhrase = customPhrase,
                        isEnabled = true,
                        cooldownSeconds = cooldownSeconds,
                        requiresConfirmation = requiresConfirmation,
                        delaySeconds = delaySeconds,
                        isChain = isChain,
                        chainStepsJson = chainJson
                    )
                    onSave(newAction)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EyeEmerald,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_action_button")
            ) {
                Text("Save Action", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel")
            }
        },
        containerColor = Color(0xFF10192A),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("action_builder_dialog")
    )
}
