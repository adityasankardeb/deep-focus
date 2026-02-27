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
    val isPaused: Boolean = true, // starts paused — timer only runs when video plays
    val isFinished: Boolean = false,
    val showEarlyExitDialog: Boolean = false
) {
    val formattedTime: String get() {
        val h = remainingSeconds / 3600
        val m = (remainingSeconds % 3600) / 60
        val s = remainingSeconds % 60
        return if (h > 0) String.format("%02d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }
    val progressFraction: Float get() =
        if (totalSeconds > 0) 1f - (remainingSeconds.toFloat() / totalSeconds.toFloat()) else 1f
}

class FocusViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()
    private var timerJob: Job? = null

    fun initializeTimer(durationSeconds: Int) {
        if (_uiState.value.isRunning || _uiState.value.totalSeconds > 0) return
        val total = durationSeconds.toLong()
        _uiState.update { it.copy(totalSeconds = total, remainingSeconds = total, isPaused = true) }
        startLoop()
    }

    private fun startLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true) }
            while (_uiState.value.remainingSeconds > 0) {
                delay(1000L)
                if (!_uiState.value.isPaused) {
                    _uiState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
                }
            }
            _uiState.update { it.copy(isRunning = false, isFinished = true) }
        }
    }

    fun pauseTimer() { _uiState.update { it.copy(isPaused = true) } }
    fun resumeTimer() { _uiState.update { it.copy(isPaused = false) } }
    fun onShowEarlyExitDialog() { _uiState.update { it.copy(showEarlyExitDialog = true) } }
    fun onDismissEarlyExitDialog() { _uiState.update { it.copy(showEarlyExitDialog = false) } }
    override fun onCleared() { super.onCleared(); timerJob?.cancel() }
}
