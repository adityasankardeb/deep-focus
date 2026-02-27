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
    val isLiveStream: Boolean = false,
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
            it.copy(
                youtubeUrl = url, urlError = null, fetchError = null,
                videoTitle = null, videoDurationSeconds = 0,
                startSeconds = 0, endSeconds = 0, isFetchingDuration = false,
                isLiveStream = false
            )
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
                val info = withContext(Dispatchers.IO) { fetchVideoInfo(videoId) }
                if (info.isLive) {
                    // Live stream — no start/end concept, session duration chosen by user
                    // We set a generous default duration of 2 hours for live streams
                    val defaultDuration = 7200
                    _uiState.update {
                        it.copy(
                            isFetchingDuration = false,
                            videoTitle = info.title,
                            videoDurationSeconds = defaultDuration,
                            startSeconds = 0,
                            endSeconds = defaultDuration,
                            isLiveStream = true,
                            fetchError = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isFetchingDuration = false,
                            videoTitle = info.title,
                            videoDurationSeconds = info.durationSeconds,
                            startSeconds = 0,
                            endSeconds = info.durationSeconds,
                            isLiveStream = false,
                            fetchError = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isFetchingDuration = false,
                        fetchError = "Could not fetch video info. Check your internet and URL.",
                        videoTitle = null, videoDurationSeconds = 0
                    )
                }
            }
        }
    }

    private data class VideoInfo(val title: String, val durationSeconds: Int, val isLive: Boolean)

    private fun fetchVideoInfo(videoId: String): VideoInfo {
        val title = try {
            val json = URL("https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json").readText()
            JSONObject(json).optString("title", "YouTube Video")
        } catch (e: Exception) { "YouTube Video" }

        val pageHtml = URL("https://www.youtube.com/watch?v=$videoId").readText()

        // Detect live stream
        val isLive = pageHtml.contains("\"isLive\":true") ||
                     pageHtml.contains("\"liveBroadcastDetails\"") ||
                     pageHtml.contains("\"isLiveContent\":true") ||
                     pageHtml.contains("BADGE_STYLE_TYPE_LIVE_NOW")

        if (isLive) return VideoInfo(title, 0, true)

        val durationMs = Regex(""""approxDurationMs":"(\d+)"""").find(pageHtml)?.groupValues?.get(1)?.toLongOrNull()
        if (durationMs != null) return VideoInfo(title, (durationMs / 1000).toInt(), false)

        val lengthSeconds = Regex(""""lengthSeconds":"(\d+)"""").find(pageHtml)?.groupValues?.get(1)?.toIntOrNull()
        if (lengthSeconds != null) return VideoInfo(title, lengthSeconds, false)

        throw Exception("Duration not found")
    }

    fun validateAndGetInputs(): Triple<String, Int, Int>? {
        val state = _uiState.value
        val videoId = extractYouTubeId(state.youtubeUrl.trim())
        if (videoId == null) {
            _uiState.update { it.copy(urlError = "Please enter a valid YouTube URL") }
            return null
        }
        if (state.videoDurationSeconds <= 0) {
            _uiState.update { it.copy(fetchError = "Please load the video info first") }
            return null
        }
        if (!state.isLiveStream && state.endSeconds - state.startSeconds < 10) {
            _uiState.update { it.copy(fetchError = "Select at least 10 seconds") }
            return null
        }
        return Triple(videoId, state.startSeconds, state.endSeconds)
    }
}
