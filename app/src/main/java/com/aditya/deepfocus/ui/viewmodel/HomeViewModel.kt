package com.aditya.deepfocus.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val youtubeUrl: String = "",
    val durationMinutes: String = "25",
    val urlError: String? = null,
    val durationError: String? = null,
    val showPrivacyDialog: Boolean = false,
    val showDndPermissionRationale: Boolean = false
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onUrlChanged(url: String) { _uiState.update { it.copy(youtubeUrl = url, urlError = null) } }
    fun onDurationChanged(duration: String) { _uiState.update { it.copy(durationMinutes = duration.filter { c -> c.isDigit() }, durationError = null) } }
    fun onShowPrivacyDialog() { _uiState.update { it.copy(showPrivacyDialog = true) } }
    fun onDismissPrivacyDialog() { _uiState.update { it.copy(showPrivacyDialog = false) } }
    fun onShowDndRationale() { _uiState.update { it.copy(showDndPermissionRationale = true) } }
    fun onDismissDndRationale() { _uiState.update { it.copy(showDndPermissionRationale = false) } }

    fun validateAndGetInputs(): Pair<String, Int>? {
        val state = _uiState.value
        var hasError = false
        val url = state.youtubeUrl.trim()
        if (url.isBlank() || (!url.contains("youtube.com") && !url.contains("youtu.be"))) {
            _uiState.update { it.copy(urlError = "Please enter a valid YouTube URL") }
            hasError = true
        }
        val minutes = state.durationMinutes.toIntOrNull()
        if (minutes == null || minutes <= 0 || minutes > 480) {
            _uiState.update { it.copy(durationError = "Enter a duration between 1 and 480 minutes") }
            hasError = true
        }
        if (hasError) return null
        return Pair(url, minutes!!)
    }
}
