package com.fittrack.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.fittrack.app.databinding.ActivityMainBinding
import com.fittrack.app.ui.premium.PremiumActivity
import com.fittrack.app.ui.workout.WorkoutLogActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupClickListeners() {
        // Start Workout Button
        binding.startWorkoutButton.setOnClickListener {
            startWorkoutSession()
        }

        // FAB for quick workout start
        binding.fab.setOnClickListener {
            startWorkoutSession()
        }

        // Premium Button
        binding.unlockPremiumButton.setOnClickListener {
            startActivity(Intent(this, PremiumActivity::class.java))
        }

        // Make cards clickable for detailed view
        binding.todayWorkoutCard.setOnClickListener {
            viewModel.todayWorkout.value?.let {
                startActivity(Intent(this, WorkoutLogActivity::class.java))
            }
        }

        binding.weeklyStatsCard.setOnClickListener {
            // TODO: Implement detailed stats view
            Snackbar.make(binding.root, "Detailed stats coming soon!", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe errors
        viewModel.error.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Observe today's workout
        viewModel.todayWorkout.observe(this) { workout ->
            if (workout != null) {
                binding.workoutStatusText.text = "Workout in progress\nDuration: ${
                    viewModel.formatDuration(workout.duration)
                }\nCalories: ${workout.caloriesBurned}"
                binding.startWorkoutButton.isEnabled = false
                binding.fab.hide()
            } else {
                binding.workoutStatusText.text = "No workout started yet"
                binding.startWorkoutButton.isEnabled = true
                binding.fab.show()
            }
        }

        // Observe weekly stats
        viewModel.weeklyStats.observe(this) { stats ->
            binding.weeklyStatsText.text = viewModel.formatStatsForDisplay(stats)
        }

        // Observe premium status
        viewModel.isPremium.observe(this) { isPremium ->
            binding.premiumCard.visibility = if (isPremium) View.GONE else View.VISIBLE
            // TODO: Update UI elements based on premium status
        }
    }

    private fun startWorkoutSession() {
        if (viewModel.hasActiveWorkout.value == true) {
            Snackbar.make(
                binding.root,
                "You already have an active workout today",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }
        
        startActivity(Intent(this, WorkoutLogActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }
}