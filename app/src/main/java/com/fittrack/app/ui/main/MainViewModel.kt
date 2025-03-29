package com.fittrack.app.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.model.Workout
import com.fittrack.app.data.model.WorkoutStats
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.ui.base.BaseViewModel
import com.fittrack.app.util.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val networkUtils: NetworkUtils
) : BaseViewModel() {

    private val _todayWorkout = MutableLiveData<Workout?>()
    val todayWorkout: LiveData<Workout?> = _todayWorkout

    private val _weeklyStats = MutableLiveData<WorkoutStats>()
    val weeklyStats: LiveData<WorkoutStats> = _weeklyStats

    val hasActiveWorkout = todayWorkout.map { it != null }

    private val _isPremium = MutableLiveData<Boolean>()
    val isPremium: LiveData<Boolean> = _isPremium

    init {
        loadTodayWorkout()
        loadWeeklyStats()
        checkPremiumStatus()
    }

    private fun loadTodayWorkout() {
        viewModelScope.launch {
            repository.getLatestWorkoutForToday().observeForever { workout ->
                _todayWorkout.value = workout
            }
        }
    }

    private fun loadWeeklyStats() {
        launchDataLoad {
            val calendar = Calendar.getInstance()
            
            // Set end date to end of today
            val endDate = calendar.clone() as Calendar
            endDate.set(Calendar.HOUR_OF_DAY, 23)
            endDate.set(Calendar.MINUTE, 59)
            endDate.set(Calendar.SECOND, 59)
            
            // Set start date to beginning of 7 days ago
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            
            val startDate = calendar.time

            repository.getWorkoutStats(startDate, endDate.time).fold(
                onSuccess = { stats ->
                    _weeklyStats.value = stats
                },
                onFailure = { error ->
                    _error.value = "Failed to load weekly stats: ${error.message}"
                }
            )
        }
    }

    fun startNewWorkout() {
        if (_todayWorkout.value != null) {
            _error.value = "You already have an active workout today"
            return
        }

        val newWorkout = Workout(
            type = "In Progress",
            duration = 0L,
            caloriesBurned = 0,
            timestamp = Date(),
            intensity = com.fittrack.app.data.model.WorkoutIntensity.MEDIUM
        )

        viewModelScope.launch {
            repository.insertWorkout(newWorkout).fold(
                onSuccess = { workoutId ->
                    loadTodayWorkout()
                },
                onFailure = { error ->
                    _error.value = "Failed to start workout: ${error.message}"
                }
            )
        }
    }

    private fun checkPremiumStatus() {
        // TODO: Implement premium status check with Google Play Billing
        _isPremium.value = false
    }

    fun refreshData() {
        loadTodayWorkout()
        loadWeeklyStats()
    }

    // Format stats for display
    fun formatStatsForDisplay(stats: WorkoutStats): String {
        return """
            Total Workouts: ${stats.totalWorkouts}
            Total Duration: ${formatDuration(stats.totalDuration)}
            Calories Burned: ${stats.totalCaloriesBurned}
            Average Intensity: ${stats.averageIntensity}
        """.trimIndent()
    }

    // Premium features
    fun unlockPremium() {
        // TODO: Implement premium unlock with Google Play Billing
        viewModelScope.launch {
            try {
                // Simulate premium purchase
                _isPremium.value = true
            } catch (e: Exception) {
                _error.value = "Failed to unlock premium: ${e.message}"
            }
        }
    }
}