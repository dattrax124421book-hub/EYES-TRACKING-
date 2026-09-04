package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ActionProfile
import com.example.data.model.ActionType
import com.example.data.model.CalibrationData
import com.example.data.model.GestureAction
import com.example.data.model.GestureHistoryItem
import com.example.data.model.GestureType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Converters {
    @TypeConverter
    fun fromGestureType(value: GestureType): String = value.name

    @TypeConverter
    fun toGestureType(value: String): GestureType = try {
        GestureType.valueOf(value)
    } catch (e: Exception) {
        GestureType.TRIPLE_BLINK
    }

    @TypeConverter
    fun fromActionType(value: ActionType): String = value.name

    @TypeConverter
    fun toActionType(value: String): ActionType = try {
        ActionType.valueOf(value)
    } catch (e: Exception) {
        ActionType.LAUNCH_CAMERA
    }
}

@Database(
    entities = [
        GestureAction::class,
        ActionProfile::class,
        GestureHistoryItem::class,
        CalibrationData::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gestureDao(): GestureDao
    abstract fun profileDao(): ProfileDao
    abstract fun historyDao(): HistoryDao
    abstract fun calibrationDao(): CalibrationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "eye_gesture_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            suspend fun populateInitialData(database: AppDatabase) {
                val profileDao = database.profileDao()
                val gestureDao = database.gestureDao()
                val calibrationDao = database.calibrationDao()

                // Insert Default Profiles
                val p1 = profileDao.insertProfile(
                    ActionProfile(
                        id = 1,
                        name = "Accessibility",
                        description = "Everyday eye gesture controls for hands-free navigation",
                        iconKey = "accessibility",
                        isDefault = true
                    )
                )
                val p2 = profileDao.insertProfile(
                    ActionProfile(
                        id = 2,
                        name = "Gaming",
                        description = "Quick eye triggers for gaming haptics and shortcuts",
                        iconKey = "sports_esports"
                    )
                )
                val p3 = profileDao.insertProfile(
                    ActionProfile(
                        id = 3,
                        name = "Video",
                        description = "Hands-free video playback and volume control",
                        iconKey = "movie"
                    )
                )
                val p4 = profileDao.insertProfile(
                    ActionProfile(
                        id = 4,
                        name = "Reading",
                        description = "Gentle gestures for scrolling, browser, and navigation",
                        iconKey = "menu_book"
                    )
                )
                val p5 = profileDao.insertProfile(
                    ActionProfile(
                        id = 5,
                        name = "Custom",
                        description = "User-defined automation sequences and combinations",
                        iconKey = "build"
                    )
                )

                // Populate Default Actions for Accessibility (Profile 1)
                gestureDao.insertAction(
                    GestureAction(
                        profileId = 1,
                        gestureType = GestureType.TRIPLE_BLINK,
                        actionType = ActionType.LAUNCH_CAMERA,
                        cooldownSeconds = 3,
                        requiresConfirmation = false,
                        isEnabled = true
                    )
                )
                gestureDao.insertAction(
                    GestureAction(
                        profileId = 1,
                        gestureType = GestureType.DOUBLE_BLINK,
                        actionType = ActionType.FLASHLIGHT_TOGGLE,
                        cooldownSeconds = 2,
                        requiresConfirmation = false,
                        isEnabled = true
                    )
                )
                gestureDao.insertAction(
                    GestureAction(
                        profileId = 1,
                        gestureType = GestureType.LOOK_RIGHT_AND_BLINK,
                        actionType = ActionType.VOLUME_UP,
                        cooldownSeconds = 1,
                        requiresConfirmation = false,
                        isEnabled = true
                    )
                )
                gestureDao.insertAction(
                    GestureAction(
                        profileId = 1,
                        gestureType = GestureType.LOOK_LEFT_AND_DOUBLE_BLINK,
                        actionType = ActionType.VOLUME_DOWN,
                        cooldownSeconds = 1,
                        requiresConfirmation = false,
                        isEnabled = true
                    )
                )

                // Populate Default Actions for Video Profile (Profile 3)
                gestureDao.insertAction(
                    GestureAction(
                        profileId = 3,
                        gestureType = GestureType.DOUBLE_BLINK,
                        actionType = ActionType.MEDIA_PLAY_PAUSE,
                        cooldownSeconds = 2,
                        requiresConfirmation = false,
                        isEnabled = true
                    )
                )
                gestureDao.insertAction(
                    GestureAction(
                        profileId = 3,
                        gestureType = GestureType.LOOK_RIGHT,
                        actionType = ActionType.VOLUME_UP,
                        cooldownSeconds = 2,
                        requiresConfirmation = false,
                        isEnabled = true
                    )
                )
                gestureDao.insertAction(
                    GestureAction(
                        profileId = 3,
                        gestureType = GestureType.LOOK_LEFT,
                        actionType = ActionType.VOLUME_DOWN,
                        cooldownSeconds = 2,
                        requiresConfirmation = false,
                        isEnabled = true
                    )
                )

                // Populate Default Actions for Reading Profile (Profile 4)
                gestureDao.insertAction(
                    GestureAction(
                        profileId = 4,
                        gestureType = GestureType.LOOK_RIGHT_AND_BLINK,
                        actionType = ActionType.GO_BACK_HINT,
                        cooldownSeconds = 2,
                        requiresConfirmation = false,
                        isEnabled = true
                    )
                )

                // Default Calibration
                calibrationDao.saveCalibrationData(CalibrationData(id = 1))
            }
        }
    }
}
