package com.fittrack.app.ui.workout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.model.Workout
import com.fittrack.app.data.model.WorkoutIntensity
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class WorkoutLogViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : BaseViewModel() {

    private var timerJob: Job? = null
    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private var isPaused = false

    private val _workout = MutableLiveData<Workout>()
    val workout: LiveData<Workout> = _workout

    private val _workoutState = MutableLiveData<WorkoutState>()
    val workoutState: LiveData<WorkoutState> = _workoutState

    private val _duration = MutableLiveData<Long>()
    val duration: LiveData<Long> = _duration

    private val _calories = MutableLiveData(0)
    val calories: LiveData<Int> = _calories

    // Available workout types
    val workoutTypes = listOf(
        "Running",
        "Walking",
        "Cycling",
        "Swimming",
        "Weight Training",
        "Yoga",
        "HIIT",
        "Other"
    )

    init {
        _workoutState.value = WorkoutState.NOT_STARTED
    }

    fun startWorkout(type: String = "Other", intensity: WorkoutIntensity = WorkoutIntensity.MEDIUM) {
        if (_workoutState.value == WorkoutState.NOT_STARTED) {
            viewModelScope.launch {
                startTime = System.currentTimeMillis()
                _workout.value = Workout(
                    type = type,
                    duration = 0,
                    caloriesBurned = 0,
                    timestamp = Date(),
                    intensity = intensity
                )

                repository.insertWorkout(_workout.value!!).fold(
                    onSuccess = {
                        startTimer()
                        _workoutState.value = WorkoutState.IN_PROGRESS
                    },
                    onFailure = { error ->
                        _error.value = "Failed to start workout: ${error.message}"
                    }
                )
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                if (!isPaused) {
                    elapsedTime = System.currentTimeMillis() - startTime
                    _duration.value = elapsedTime
                    updateCalories()
                }
                delay(1000) // Update every second
            }
        }
    }

    private fun updateCalories() {
        _workout.value?.let { workout ->
            val minutes = elapsedTime / 1000 / 60
            _calories.value = when (workout.intensity) {
                WorkoutIntensity.LOW -> (minutes * 4).toInt() // 4 calories per minute
                WorkoutIntensity.MEDIUM -> (minutes * 8).toInt() // 8 calories per minute
                WorkoutIntensity.HIGH -> (minutes * 12).toInt() // 12 calories per minute
            }
        }
    }

    fun pauseWorkout() {
        isPaused = true
        _workoutState.value = WorkoutState.PAUSED
    }

    fun resumeWorkout() {
        isPaused = false
        startTime = System.currentTimeMillis() - elapsedTime
        _workoutState.value = WorkoutState.IN_PROGRESS
    }

    fun updateWorkoutType(type: String) {
        _workout.value = _workout.value?.copy(type = type)
        saveWorkoutUpdate()
    }

    fun updateIntensity(intensity: WorkoutIntensity) {
        _workout.value = _workout.value?.copy(intensity = intensity)
        saveWorkoutUpdate()
    }

    fun updateNotes(notes: String) {
        _workout.value = _workout.value?.copy(notes = notes)
        saveWorkoutUpdate()
    }

    private fun saveWorkoutUpdate() {
        _workout.value?.let { workout ->
            viewModelScope.launch {
                repository.updateWorkout(workout).fold(
                    onFailure = { error ->
                        _error.value = "Failed to update workout: ${error.message}"
                    }
                )
            }
        }
    }

    fun finishWorkout() {
        viewModelScope.launch {
            _workout.value?.let { workout ->
                val updatedWorkout = workout.copy(
                    duration = elapsedTime,
                    caloriesBurned = _calories.value ?: 0
                )
                
                repository.updateWorkout(updatedWorkout).fold(
                    onSuccess = {
                        timerJob?.cancel()
                        _workoutState.value = WorkoutState.FINISHED
                    },
                    onFailure = { error ->
                        _error.value = "Failed to finish workout: ${error.message}"
                    }
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

enum class WorkoutState {
    NOT_STARTED,
    IN_PROGRESS,
    PAUSED,
    FINISHED
}