package com.fittrack.app.data.repository

import androidx.lifecycle.LiveData
import com.fittrack.app.data.database.WorkoutDao
import com.fittrack.app.data.model.Workout
import com.fittrack.app.data.model.WorkoutStats
import com.fittrack.app.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val networkUtils: NetworkUtils
) {
    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    val allWorkouts: LiveData<List<Workout>> = workoutDao.getAllWorkouts()

    suspend fun insertWorkout(workout: Workout): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val id = workoutDao.insertWorkout(workout)
            // Sync with backend if network is available (to be implemented)
            if (networkUtils.isNetworkAvailable()) {
                // TODO: Implement backend sync
            }
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWorkout(workout: Workout): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            workoutDao.updateWorkout(workout)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWorkout(workout: Workout): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            workoutDao.deleteWorkout(workout)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getWorkoutsBetweenDates(startDate: Date, endDate: Date): LiveData<List<Workout>> {
        return workoutDao.getWorkoutsBetweenDates(startDate, endDate)
    }

    suspend fun getWorkoutStats(startDate: Date, endDate: Date): Result<WorkoutStats> = 
        withContext(Dispatchers.IO) {
            try {
                val stats = workoutDao.getWorkoutStats(startDate, endDate)
                Result.success(stats)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun getLatestWorkoutForToday(): LiveData<Workout?> {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        return workoutDao.getLatestWorkoutForToday(startOfDay)
    }

    suspend fun getWorkoutById(workoutId: Long): Result<Workout?> = withContext(Dispatchers.IO) {
        try {
            val workout = workoutDao.getWorkoutById(workoutId)
            Result.success(workout)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // For testing or user account deletion
    suspend fun clearAllWorkouts(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            workoutDao.deleteAllWorkouts()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}