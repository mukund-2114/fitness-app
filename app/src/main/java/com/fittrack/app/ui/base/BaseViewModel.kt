package com.fittrack.app.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

abstract class BaseViewModel : ViewModel() {
    protected val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    protected val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    protected fun launchDataLoad(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                block()
            } catch (error: Exception) {
                _error.value = error.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    protected fun formatDate(date: Date): String {
        return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
    }

    protected fun formatDuration(durationInMillis: Long): String {
        val hours = durationInMillis / (1000 * 60 * 60)
        val minutes = (durationInMillis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (durationInMillis % (1000 * 60)) / 1000

        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun clearError() {
        _error.value = null
    }
}