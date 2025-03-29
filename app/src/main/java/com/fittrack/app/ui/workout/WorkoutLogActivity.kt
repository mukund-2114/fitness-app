package com.fittrack.app.ui.workout

import android.os.Bundle
import android.os.SystemClock
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fittrack.app.data.model.WorkoutIntensity
import com.fittrack.app.databinding.ActivityWorkoutLogBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WorkoutLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutLogBinding
    private val viewModel: WorkoutLogViewModel by viewModels()
    private var chronometerBase: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupWorkoutTypeDropdown()
        setupIntensityRadioGroup()
        setupButtons()
        observeViewModel()
        
        // Start the workout immediately
        viewModel.startWorkout()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupWorkoutTypeDropdown() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            viewModel.workoutTypes
        )
        binding.workoutTypeDropdown.setAdapter(adapter)
        
        binding.workoutTypeDropdown.setOnItemClickListener { _, _, position, _ ->
            viewModel.updateWorkoutType(viewModel.workoutTypes[position])
        }
    }

    private fun setupIntensityRadioGroup() {
        binding.intensityRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val intensity = when (checkedId) {
                binding.lowIntensity.id -> WorkoutIntensity.LOW
                binding.mediumIntensity.id -> WorkoutIntensity.MEDIUM
                binding.highIntensity.id -> WorkoutIntensity.HIGH
                else -> WorkoutIntensity.MEDIUM
            }
            viewModel.updateIntensity(intensity)
        }
    }

    private fun setupButtons() {
        binding.pauseResumeButton.setOnClickListener {
            when (viewModel.workoutState.value) {
                WorkoutState.IN_PROGRESS -> {
                    viewModel.pauseWorkout()
                    binding.chronometer.stop()
                    binding.pauseResumeButton.text = "Resume"
                }
                WorkoutState.PAUSED -> {
                    viewModel.resumeWorkout()
                    binding.chronometer.base = SystemClock.elapsedRealtime() - 
                        (viewModel.duration.value ?: 0)
                    binding.chronometer.start()
                    binding.pauseResumeButton.text = "Pause"
                }
                else -> { /* Do nothing */ }
            }
        }

        binding.finishButton.setOnClickListener {
            showFinishWorkoutDialog()
        }

        binding.notesInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.updateNotes(binding.notesInput.text.toString())
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.workoutState.observe(this) { state ->
            when (state) {
                WorkoutState.IN_PROGRESS -> {
                    binding.chronometer.base = SystemClock.elapsedRealtime()
                    binding.chronometer.start()
                    binding.pauseResumeButton.isEnabled = true
                    binding.finishButton.isEnabled = true
                }
                WorkoutState.PAUSED -> {
                    binding.chronometer.stop()
                }
                WorkoutState.FINISHED -> {
                    finish()
                }
                else -> { /* Do nothing */ }
            }
        }

        viewModel.calories.observe(this) { calories ->
            binding.caloriesText.text = "$calories calories"
        }

        viewModel.workout.observe(this) { workout ->
            workout?.let {
                binding.workoutTypeDropdown.setText(it.type, false)
                
                val radioButton = when (it.intensity) {
                    WorkoutIntensity.LOW -> binding.lowIntensity
                    WorkoutIntensity.MEDIUM -> binding.mediumIntensity
                    WorkoutIntensity.HIGH -> binding.highIntensity
                }
                radioButton.isChecked = true

                it.notes?.let { notes ->
                    if (binding.notesInput.text.toString() != notes) {
                        binding.notesInput.setText(notes)
                    }
                }
            }
        }
    }

    private fun showFinishWorkoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Finish Workout")
            .setMessage("Are you sure you want to finish this workout?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.finishWorkout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                showExitConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit Workout")
            .setMessage("Are you sure you want to exit? Your progress will be saved.")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.finishWorkout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onBackPressed() {
        showExitConfirmationDialog()
    }
}