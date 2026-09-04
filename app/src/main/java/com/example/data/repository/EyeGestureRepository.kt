package com.example.data.repository

import com.example.data.local.CalibrationDao
import com.example.data.local.GestureDao
import com.example.data.local.HistoryDao
import com.example.data.local.ProfileDao
import com.example.data.model.ActionProfile
import com.example.data.model.CalibrationData
import com.example.data.model.GestureAction
import com.example.data.model.GestureHistoryItem
import com.example.data.model.GestureType
import kotlinx.coroutines.flow.Flow

class EyeGestureRepository(
    private val gestureDao: GestureDao,
    private val profileDao: ProfileDao,
    private val historyDao: HistoryDao,
    private val calibrationDao: CalibrationDao
) {
    fun getActionsForProfile(profileId: Long): Flow<List<GestureAction>> =
        gestureDao.getActionsForProfile(profileId)

    fun getAllActions(): Flow<List<GestureAction>> =
        gestureDao.getAllActions()

    suspend fun getActionForGesture(profileId: Long, gestureType: GestureType): GestureAction? =
        gestureDao.getActionForGesture(profileId, gestureType)

    suspend fun saveAction(action: GestureAction): Long =
        gestureDao.insertAction(action)

    suspend fun updateAction(action: GestureAction) =
        gestureDao.updateAction(action)

    suspend fun deleteAction(action: GestureAction) =
        gestureDao.deleteAction(action)

    suspend fun deleteActionById(actionId: Long) =
        gestureDao.deleteActionById(actionId)

    fun getAllProfiles(): Flow<List<ActionProfile>> =
        profileDao.getAllProfiles()

    suspend fun getProfileById(profileId: Long): ActionProfile? =
        profileDao.getProfileById(profileId)

    suspend fun saveProfile(profile: ActionProfile): Long =
        profileDao.insertProfile(profile)

    suspend fun deleteProfile(profile: ActionProfile) =
        profileDao.deleteProfile(profile)

    fun getRecentHistory(): Flow<List<GestureHistoryItem>> =
        historyDao.getRecentHistory()

    suspend fun addHistoryItem(item: GestureHistoryItem) =
        historyDao.insertHistory(item)

    suspend fun clearHistory() =
        historyDao.clearHistory()

    fun getCalibrationData(): Flow<CalibrationData?> =
        calibrationDao.getCalibrationData()

    suspend fun getCalibrationDataSync(): CalibrationData? =
        calibrationDao.getCalibrationDataSync()

    suspend fun saveCalibrationData(data: CalibrationData) =
        calibrationDao.saveCalibrationData(data)
}
