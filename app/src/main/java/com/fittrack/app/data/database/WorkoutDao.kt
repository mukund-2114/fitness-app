package com.fittrack.app.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.fittrack.app.data.model.Workout
import com.fittrack.app.data.model.WorkoutStats
import java.util.Date

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: Workout): Long

    @Update
    suspend fun updateWorkout(workout: Workout)

    @Delete
    suspend fun deleteWorkout(workout: Workout)

    @Query("SELECT * FROM workouts ORDER BY timestamp DESC")
    fun getAllWorkouts(): LiveData<List<Workout>>

    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: Long): Workout?

    @Query("SELECT * FROM workouts WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getWorkoutsBetweenDates(startDate: Date, endDate: Date): LiveData<List<Workout>>

    @Query("""
        SELECT 
            COUNT(*) as totalWorkouts,
            SUM(duration) as totalDuration,
            SUM(caloriesBurned) as totalCaloriesBurned,
            AVG(CASE 
                WHEN intensity = 'HIGH' THEN 3
                WHEN intensity = 'MEDIUM' THEN 2
                ELSE 1
            END) as averageIntensityValue
        FROM workouts
        WHERE timestamp BETWEEN :startDate AND :endDate
    """)
    suspend fun getWorkoutStats(startDate: Date, endDate: Date): WorkoutStats

    @Query("SELECT * FROM workouts WHERE timestamp >= :startOfDay ORDER BY timestamp DESC LIMIT 1")
    fun getLatestWorkoutForToday(startOfDay: Date): LiveData<Workout?>

    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts()
}