package com.fittrack.app.data.database

import android.content.Context
import androidx.room.*
import com.fittrack.app.data.model.Workout
import com.fittrack.app.data.model.WorkoutIntensity
import java.util.Date

@Database(
    entities = [Workout::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fittrack_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Type converters for Room database
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromWorkoutIntensity(intensity: WorkoutIntensity): String {
        return intensity.name
    }

    @TypeConverter
    fun toWorkoutIntensity(intensity: String): WorkoutIntensity {
        return try {
            WorkoutIntensity.valueOf(intensity)
        } catch (e: IllegalArgumentException) {
            WorkoutIntensity.MEDIUM // Default value if conversion fails
        }
    }
}