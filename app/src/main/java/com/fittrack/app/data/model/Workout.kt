package com.fittrack.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val type: String,
    val duration: Long, // in milliseconds
    val caloriesBurned: Int,
    val timestamp: Date,
    
    // Additional workout details
    val notes: String? = null,
    val distance: Float? = null, // in kilometers
    val intensity: WorkoutIntensity = WorkoutIntensity.MEDIUM
)

enum class WorkoutIntensity {
    LOW,
    MEDIUM,
    HIGH
}

// Extension function to calculate calories burned based on duration and intensity
fun Workout.calculateCalories(): Int {
    return when (intensity) {
        WorkoutIntensity.LOW -> (duration / 1000 / 60 * 4).toInt() // 4 calories per minute
        WorkoutIntensity.MEDIUM -> (duration / 1000 / 60 * 8).toInt() // 8 calories per minute
        WorkoutIntensity.HIGH -> (duration / 1000 / 60 * 12).toInt() // 12 calories per minute
    }
}

// Data class for workout statistics
data class WorkoutStats(
    val totalWorkouts: Int,
    val totalDuration: Long,
    val totalCaloriesBurned: Int,
    val averageIntensity: WorkoutIntensity
)