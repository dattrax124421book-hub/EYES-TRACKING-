package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ActionProfile
import com.example.data.model.CalibrationData
import com.example.data.model.GestureAction
import com.example.data.model.GestureHistoryItem
import com.example.data.model.GestureType
import kotlinx.coroutines.flow.Flow

@Dao
interface GestureDao {
    @Query("SELECT * FROM gesture_actions WHERE profileId = :profileId ORDER BY id ASC")
    fun getActionsForProfile(profileId: Long): Flow<List<GestureAction>>

    @Query("SELECT * FROM gesture_actions ORDER BY id ASC")
    fun getAllActions(): Flow<List<GestureAction>>

    @Query("SELECT * FROM gesture_actions WHERE profileId = :profileId AND gestureType = :gestureType LIMIT 1")
    suspend fun getActionForGesture(profileId: Long, gestureType: GestureType): GestureAction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: GestureAction): Long

    @Update
    suspend fun updateAction(action: GestureAction)

    @Delete
    suspend fun deleteAction(action: GestureAction)

    @Query("DELETE FROM gesture_actions WHERE id = :actionId")
    suspend fun deleteActionById(actionId: Long)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM action_profiles ORDER BY id ASC")
    fun getAllProfiles(): Flow<List<ActionProfile>>

    @Query("SELECT * FROM action_profiles WHERE id = :profileId LIMIT 1")
    suspend fun getProfileById(profileId: Long): ActionProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ActionProfile): Long

    @Update
    suspend fun updateProfile(profile: ActionProfile)

    @Delete
    suspend fun deleteProfile(profile: ActionProfile)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM gesture_history ORDER BY timestamp DESC LIMIT 200")
    fun getRecentHistory(): Flow<List<GestureHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: GestureHistoryItem)

    @Query("DELETE FROM gesture_history")
    suspend fun clearHistory()
}

@Dao
interface CalibrationDao {
    @Query("SELECT * FROM calibration_data WHERE id = 1 LIMIT 1")
    fun getCalibrationData(): Flow<CalibrationData?>

    @Query("SELECT * FROM calibration_data WHERE id = 1 LIMIT 1")
    suspend fun getCalibrationDataSync(): CalibrationData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCalibrationData(data: CalibrationData)
}
