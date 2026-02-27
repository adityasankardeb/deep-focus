package com.aditya.deepfocus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class HomeUiState(
    val youtubeUrl: String = "",
    val urlError: String? = null,
    val showPrivacyDialog: Boolean = false,
    val showDndPermissionRationale: Boolean = false,
    val isFetchingDuration: Boolean = false,
    val fetchError: String? = null,
    val videoTitle: String? = null,
    val videoDurationSeconds: Int = 0,
    val startSeconds: Int = 0,
    val endSeconds: Int = 0,
    val showStartPicker: Boolean = false,
    val showEndPicker: Boolean = false,
)

fun Int.toTimeString(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%02d:%02d", m, s)
}

fun extractYouTubeId(url: String): String? =
    Regex("(?:v=|youtu\\.be/|/embed/)([a-zA-Z0-9_-]{11})").find(url)?.groupValues?.get(1)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onUrlChanged(url: String) {
        _uiState.update {
            it.copy(youtubeUrl = url, urlError = null, fetchError = null,
                videoTitle = null, videoDurationSeconds = 0,
                startSeconds = 0, endSeconds = 0, isFetchingDuration = false)
        }
    }

    fun onShowPrivacyDialog() { _uiState.update { it.copy(showPrivacyDialog = true) } }
    fun onDismissPrivacyDialog() { _uiState.update { it.copy(showPrivacyDialog = false) } }
    fun onShowDndRationale() { _uiState.update { it.copy(showDndPermissionRationale = true) } }
    fun onDismissDndRationale() { _uiState.update { it.copy(showDndPermissionRationale = false) } }

    fun onShowStartPicker() { _uiState.update { it.copy(showStartPicker = true) } }
    fun onDismissStartPicker() { _uiState.update { it.copy(showStartPicker = false) } }
    fun onShowEndPicker() { _uiState.update { it.copy(showEndPicker = true) } }
    fun onDismissEndPicker() { _uiState.update { it.copy(showEndPicker = false) } }

    fun onStartTimeConfirmed(h: Int, m: Int, s: Int) {
        val total = h * 3600 + m * 60 + s
        val clamped = total.coerceAtMost(_uiState.value.endSeconds - 10).coerceAtLeast(0)
        _uiState.update { it.copy(startSeconds = clamped, showStartPicker = false) }
    }

    fun onEndTimeConfirmed(h: Int, m: Int, s: Int) {
        val total = h * 3600 + m * 60 + s
        val clamped = total.coerceAtLeast(_uiState.value.startSeconds + 10)
            .coerceAtMost(_uiState.value.videoDurationSeconds)
        _uiState.update { it.copy(endSeconds = clamped, showEndPicker = false) }
    }

    fun applyPreset(startSeconds: Int, endSeconds: Int) {
        _uiState.update { it.copy(startSeconds = startSeconds, endSeconds = endSeconds) }
    }

    fun fetchVideoDuration() {
        val url = _uiState.value.youtubeUrl.trim()
        val videoId = extractYouTubeId(url)
        if (videoId == null) {
            _uiState.update { it.copy(urlError = "Please enter a valid YouTube URL") }
            return
        }
        _uiState.update { it.copy(isFetchingDuration = true, fetchError = null, urlError = null) }
        viewModelScope.launch {
            try {
                val (title, duration) = withContext(Dispatchers.IO) { fetchVideoInfo(videoId) }
                _uiState.update {
                    it.copy(isFetchingDuration = false, videoTitle = title,
                        videoDurationSeconds = duration, startSeconds = 0,
                        endSeconds = duration, fetchError = null)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isFetchingDuration = false,
                        fetchError = "Could not fetch video info. Check your internet and URL.",
                        videoTitle = null, videoDurationSeconds = 0)
                }
            }
        }
    }

    private fun fetchVideoInfo(videoId: String): Pair<String, Int> {
        val title = try {
            val json = URL("https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json").readText()
            JSONObject(json).optString("title", "YouTube Video")
        } catch (e: Exception) { "YouTube Video" }
        val pageHtml = URL("https://www.youtube.com/watch?v=$videoId").readText()
        val durationMs = Regex(""""approxDurationMs":"(\d+)"""").find(pageHtml)?.groupValues?.get(1)?.toLongOrNull()
        if (durationMs != null) return Pair(title, (durationMs / 1000).toInt())
        val lengthSeconds = Regex(""""lengthSeconds":"(\d+)"""").find(pageHtml)?.groupValues?.get(1)?.toIntOrNull()
        if (lengthSeconds != null) return Pair(title, lengthSeconds)
        throw Exception("Duration not found")
    }

    fun validateAndGetInputs(): Triple<String, Int, Int>? {
        val state = _uiState.value
        val videoId = extractYouTubeId(state.youtubeUrl.trim())
        if (videoId == null) { _uiState.update { it.copy(urlError = "Please enter a valid YouTube URL") }; return null }
        if (state.videoDurationSeconds <= 0) { _uiState.update { it.copy(fetchError = "Please load the video info first") }; return null }
        if (state.endSeconds - state.startSeconds < 10) { _uiState.update { it.copy(fetchError = "Select at least 10 seconds") }; return null }
        return Triple(videoId, state.startSeconds, state.endSeconds)
    }
}
