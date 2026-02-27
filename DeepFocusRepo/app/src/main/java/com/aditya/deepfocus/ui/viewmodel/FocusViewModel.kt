package com.aditya.deepfocus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FocusUiState(
    val totalSeconds: Long = 0L,
    val remainingSeconds: Long = 0L,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val showEarlyExitDialog: Boolean = false
) {
    val formattedTime: String
        get() {
            val hours = remainingSeconds / 3600
            val minutes = (remainingSeconds % 3600) / 60
            val secs = remainingSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, secs)
            } else {
                String.format("%02d:%02d", minutes, secs)
            }
        }

    val progressFraction: Float
        get() = if (totalSeconds > 0) {
            1f - (remainingSeconds.toFloat() / totalSeconds.toFloat())
        } else 1f
}

class FocusViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun initializeTimer(durationMinutes: Int) {
        // Guard: only initialize if not already running (survives recomposition)
        if (_uiState.value.isRunning || _uiState.value.totalSeconds > 0) return

        val totalSeconds = durationMinutes * 60L
        _uiState.update {
            it.copy(
                totalSeconds = totalSeconds,
                remainingSeconds = totalSeconds,
                isRunning = false,
                isFinished = false
            )
        }
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true) }
            while (_uiState.value.remainingSeconds > 0) {
                delay(1000L)
                _uiState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
            }
            // Timer finished
            _uiState.update { it.copy(isRunning = false, isFinished = true) }
        }
    }

    fun onShowEarlyExitDialog() {
        _uiState.update { it.copy(showEarlyExitDialog = true) }
    }

    fun onDismissEarlyExitDialog() {
        _uiState.update { it.copy(showEarlyExitDialog = false) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
