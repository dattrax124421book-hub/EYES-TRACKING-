package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ActionChainStep
import com.example.data.model.ActionProfile
import com.example.data.model.ActionType
import com.example.data.model.CalibrationData
import com.example.data.model.GestureAction
import com.example.data.model.GestureHistoryItem
import com.example.data.model.GestureType
import com.example.data.model.LiveTrackingState
import com.example.data.repository.EyeGestureRepository
import com.example.tracking.ActionExecutionEngine
import com.example.tracking.EyeTrackingEngine
import com.example.tracking.GestureDetectorEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null,
    val configuredActions: List<GestureAction> = emptyList()
)

data class GestureFeedbackState(
    val isVisible: Boolean = false,
    val gestureType: GestureType = GestureType.TRIPLE_BLINK,
    val actionDescription: String = "",
    val actionType: ActionType? = null
)

data class DuplicateConflictState(
    val isConflict: Boolean = false,
    val pendingAction: GestureAction? = null,
    val existingAction: GestureAction? = null
)

data class ConfirmationPromptState(
    val isPending: Boolean = false,
    val action: GestureAction? = null,
    val gesture: GestureType? = null
)

class EyeGestureViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EyeGestureRepository
    val trackingEngine: EyeTrackingEngine
    val gestureDetector: GestureDetectorEngine
    val actionExecutionEngine: ActionExecutionEngine

    // UI state flows from Room
    val allProfiles: StateFlow<List<ActionProfile>>
    private val _activeProfileId = MutableStateFlow<Long>(1)
    val activeProfileId: StateFlow<Long> = _activeProfileId.asStateFlow()

    private val _activeProfile = MutableStateFlow<ActionProfile?>(null)
    val activeProfile: StateFlow<ActionProfile?> = _activeProfile.asStateFlow()

    private val _profileActions = MutableStateFlow<List<GestureAction>>(emptyList())
    val profileActions: StateFlow<List<GestureAction>> = _profileActions.asStateFlow()

    val historyItems: StateFlow<List<GestureHistoryItem>>
    val calibrationData: StateFlow<CalibrationData?>

    // Live tracking state
    val liveTrackingState: StateFlow<LiveTrackingState>

    // Lock and Test mode
    private val _isGestureLocked = MutableStateFlow(false)
    val isGestureLocked: StateFlow<Boolean> = _isGestureLocked.asStateFlow()

    private val _isTestMode = MutableStateFlow(false)
    val isTestMode: StateFlow<Boolean> = _isTestMode.asStateFlow()

    // Overlay visual settings
    val showTrackingBall = MutableStateFlow(true)
    val showEyeLandmarks = MutableStateFlow(true)
    val showFaceLandmarks = MutableStateFlow(false)
    val showConfidenceDisplay = MutableStateFlow(true)

    // Detected Gesture Feedback Banner
    private val _feedbackState = MutableStateFlow(GestureFeedbackState())
    val feedbackState: StateFlow<GestureFeedbackState> = _feedbackState.asStateFlow()
    private var feedbackDismissJob: Job? = null

    // Duplicate conflict dialog state
    private val _duplicateConflict = MutableStateFlow(DuplicateConflictState())
    val duplicateConflict: StateFlow<DuplicateConflictState> = _duplicateConflict.asStateFlow()

    // Confirmation dialog state
    private val _confirmationPrompt = MutableStateFlow(ConfirmationPromptState())
    val confirmationPrompt: StateFlow<ConfirmationPromptState> = _confirmationPrompt.asStateFlow()

    // Installed apps
    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()
    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    // Last detected gesture for Quick display
    private val _lastDetectedGesture = MutableStateFlow<GestureType?>(null)
    val lastDetectedGesture: StateFlow<GestureType?> = _lastDetectedGesture.asStateFlow()

    // Test mode recognition banner
    private val _testSuccessMessage = MutableStateFlow<String?>(null)
    val testSuccessMessage: StateFlow<String?> = _testSuccessMessage.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = EyeGestureRepository(
            database.gestureDao(),
            database.profileDao(),
            database.historyDao(),
            database.calibrationDao()
        )

        allProfiles = repository.getAllProfiles()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        historyItems = repository.getRecentHistory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        calibrationData = repository.getCalibrationData()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        // Initialize engines
        actionExecutionEngine = ActionExecutionEngine(
            context = application,
            scope = viewModelScope,
            onLogHistory = { item ->
                viewModelScope.launch { repository.addHistoryItem(item) }
            },
            onRequestConfirmation = { action, gesture ->
                _confirmationPrompt.value = ConfirmationPromptState(
                    isPending = true,
                    action = action,
                    gesture = gesture
                )
            }
        )

        gestureDetector = GestureDetectorEngine(viewModelScope) { gesture ->
            handleRecognizedGesture(gesture)
        }

        trackingEngine = EyeTrackingEngine(application) { frameState ->
            gestureDetector.processTrackingFrame(frameState)
        }

        liveTrackingState = trackingEngine.trackingState

        // Observe active profile and actions
        viewModelScope.launch {
            allProfiles.collect { profiles ->
                if (profiles.isNotEmpty()) {
                    val currentId = _activeProfileId.value
                    val current = profiles.firstOrNull { it.id == currentId } ?: profiles.first()
                    _activeProfile.value = current
                    _activeProfileId.value = current.id
                }
            }
        }

        viewModelScope.launch {
            _activeProfileId.collect { profileId ->
                repository.getActionsForProfile(profileId).collect { actions ->
                    _profileActions.value = actions
                    updateInstalledAppsWithActions(actions)
                }
            }
        }

        viewModelScope.launch {
            calibrationData.collect { cal ->
                cal?.let {
                    trackingEngine.calibration = it
                    gestureDetector.calibration = it
                }
            }
        }

        loadInstalledApps()
    }

    private fun handleRecognizedGesture(gesture: GestureType) {
        _lastDetectedGesture.value = gesture

        val actions = _profileActions.value
        val assignedAction = actions.firstOrNull { it.gestureType == gesture && it.isEnabled }
        val currentProfileName = _activeProfile.value?.name ?: "Default"

        if (_isTestMode.value) {
            _testSuccessMessage.value = "✅ Gesture Recognized: ${gesture.displayName}\nTest Successful!"
            viewModelScope.launch {
                delay(3000)
                _testSuccessMessage.value = null
            }
        }

        // Show temporary feedback card
        showFeedback(gesture, assignedAction)

        // Forward to execution engine
        actionExecutionEngine.handleGesture(gesture, assignedAction, currentProfileName)
    }

    private fun showFeedback(gesture: GestureType, assignedAction: GestureAction?) {
        val actionText = if (assignedAction != null) {
            val appLabel = assignedAction.targetAppLabel?.let { " ($it)" } ?: ""
            "${assignedAction.actionType.displayName}$appLabel"
        } else {
            "No action assigned"
        }

        _feedbackState.value = GestureFeedbackState(
            isVisible = true,
            gestureType = gesture,
            actionDescription = actionText,
            actionType = assignedAction?.actionType
        )

        feedbackDismissJob?.cancel()
        feedbackDismissJob = viewModelScope.launch {
            delay(3500)
            _feedbackState.value = _feedbackState.value.copy(isVisible = false)
        }
    }

    fun dismissFeedback() {
        feedbackDismissJob?.cancel()
        _feedbackState.value = _feedbackState.value.copy(isVisible = false)
    }

    fun toggleGestureLock() {
        val newLock = !_isGestureLocked.value
        _isGestureLocked.value = newLock
        actionExecutionEngine.isGestureLocked = newLock
    }

    fun toggleTestMode() {
        val newTest = !_isTestMode.value
        _isTestMode.value = newTest
        actionExecutionEngine.isTestMode = newTest
        if (!newTest) {
            _testSuccessMessage.value = null
        }
    }

    fun selectProfile(profile: ActionProfile) {
        _activeProfileId.value = profile.id
        _activeProfile.value = profile
    }

    fun createProfile(name: String, description: String, iconKey: String) {
        viewModelScope.launch {
            val newId = repository.saveProfile(
                ActionProfile(
                    name = name,
                    description = description,
                    iconKey = iconKey,
                    isDefault = false
                )
            )
            _activeProfileId.value = newId
        }
    }

    fun deleteProfile(profile: ActionProfile) {
        viewModelScope.launch {
            if (profile.id != 1L) {
                repository.deleteProfile(profile)
                _activeProfileId.value = 1L
            }
        }
    }

    /**
     * Check for duplicate action before saving as mandated by Section 7.
     */
    fun saveOrUpdateActionWithDuplicateCheck(action: GestureAction, onConfirmed: () -> Unit) {
        viewModelScope.launch {
            val existing = _profileActions.value.firstOrNull {
                it.gestureType == action.gestureType && it.id != action.id
            }
            if (existing != null) {
                // Show Duplicate Conflict Dialog
                _duplicateConflict.value = DuplicateConflictState(
                    isConflict = true,
                    pendingAction = action,
                    existingAction = existing
                )
            } else {
                repository.saveAction(action)
                onConfirmed()
            }
        }
    }

    fun resolveConflictReplace() {
        val pending = _duplicateConflict.value.pendingAction
        val existing = _duplicateConflict.value.existingAction
        if (pending != null && existing != null) {
            viewModelScope.launch {
                repository.deleteAction(existing)
                repository.saveAction(pending)
                _duplicateConflict.value = DuplicateConflictState()
            }
        }
    }

    fun dismissConflict() {
        _duplicateConflict.value = DuplicateConflictState()
    }

    fun deleteAction(action: GestureAction) {
        viewModelScope.launch {
            repository.deleteAction(action)
        }
    }

    fun toggleActionEnabled(action: GestureAction) {
        viewModelScope.launch {
            repository.updateAction(action.copy(isEnabled = !action.isEnabled))
        }
    }

    fun confirmPendingAction() {
        val prompt = _confirmationPrompt.value
        val action = prompt.action
        val gesture = prompt.gesture
        val profileName = _activeProfile.value?.name ?: "Default"

        if (action != null && gesture != null) {
            actionExecutionEngine.executeActionDirectly(action, gesture, profileName)
        }
        _confirmationPrompt.value = ConfirmationPromptState()
    }

    fun cancelPendingAction() {
        val prompt = _confirmationPrompt.value
        val profileName = _activeProfile.value?.name ?: "Default"
        prompt.gesture?.let { gesture ->
            viewModelScope.launch {
                repository.addHistoryItem(
                    GestureHistoryItem(
                        gestureName = gesture.displayName,
                        actionName = prompt.action?.actionType?.displayName ?: "Action",
                        profileName = profileName,
                        status = "CANCELLED"
                    )
                )
            }
        }
        _confirmationPrompt.value = ConfirmationPromptState()
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun updateCalibration(newCalibration: CalibrationData) {
        viewModelScope.launch {
            repository.saveCalibrationData(newCalibration)
            trackingEngine.calibration = newCalibration
            gestureDetector.calibration = newCalibration
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingApps.value = true
            val pm = getApplication<Application>().packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val apps = resolveInfos.mapNotNull { resolveInfo ->
                try {
                    val pkg = resolveInfo.activityInfo.packageName
                    val label = resolveInfo.loadLabel(pm).toString()
                    val icon = resolveInfo.loadIcon(pm)
                    InstalledAppInfo(packageName = pkg, label = label, icon = icon)
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.label.lowercase() }

            withContext(Dispatchers.Main) {
                _installedApps.value = apps
                updateInstalledAppsWithActions(_profileActions.value)
                _isLoadingApps.value = false
            }
        }
    }

    private fun updateInstalledAppsWithActions(actions: List<GestureAction>) {
        val currentApps = _installedApps.value
        if (currentApps.isEmpty()) return

        val updated = currentApps.map { app ->
            val mapped = actions.filter { it.targetPackageName == app.packageName }
            app.copy(configuredActions = mapped)
        }
        _installedApps.value = updated
    }

    override fun onCleared() {
        super.onCleared()
        trackingEngine.release()
        actionExecutionEngine.release()
        gestureDetector.reset()
    }
}
