package com.aditya.deepfocus.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.webkit.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aditya.deepfocus.data.SessionRecord
import com.aditya.deepfocus.data.SessionRepository
import com.aditya.deepfocus.ui.viewmodel.FocusViewModel

private fun buildHtml(videoId: String, startSeconds: Int, endSeconds: Int) = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  *, html, body { margin:0; padding:0; background:#000; overflow:hidden; width:100%; height:100%; }
  #player { position:fixed; top:0; left:0; width:100%; height:100%; border:none; }
</style>
</head>
<body>
<div id="player"></div>
<script>
  var LOCKED_VIDEO_ID = '$videoId';
  var tag = document.createElement('script');
  tag.src = "https://www.youtube.com/iframe_api";
  document.head.appendChild(tag);
  var player;

  function onYouTubeIframeAPIReady() {
    player = new YT.Player('player', {
      videoId: LOCKED_VIDEO_ID,
      playerVars: {
        autoplay: 1,
        start: $startSeconds,
        end: $endSeconds,
        controls: 1,
        playsinline: 1,
        rel: 0,
        modestbranding: 1,
        iv_load_policy: 3,
        fs: 0,
        origin: 'https://www.youtube.com'
      },
      events: {
        onReady: function(e) {
          e.target.unMute();
          e.target.setVolume(100);
          e.target.playVideo();
        },
        onStateChange: function(e) {
          if (e.data === YT.PlayerState.ENDED) {
            Android.onVideoEnded();
          } else if (e.data === YT.PlayerState.PAUSED) {
            Android.onVideoPaused();
          } else if (e.data === YT.PlayerState.PLAYING) {
            Android.onVideoResumed();
          } else if (e.data === YT.PlayerState.BUFFERING) {
            Android.onVideoResumed();
          }
        },
        onVideoDataChange: function(e) {
          // If somehow a different video loads, reload the locked one
          if (player && player.getVideoData) {
            var data = player.getVideoData();
            if (data.video_id && data.video_id !== LOCKED_VIDEO_ID) {
              player.loadVideoById({ videoId: LOCKED_VIDEO_ID, startSeconds: $startSeconds });
            }
          }
        }
      }
    });
  }

  // Poll: enforce end time + enforce locked video
  setInterval(function() {
    if (!player) return;
    if (player.getCurrentTime && player.getCurrentTime() >= $endSeconds) {
      player.pauseVideo();
      Android.onVideoEnded();
    }
    if (player.getVideoData) {
      var data = player.getVideoData();
      if (data.video_id && data.video_id !== LOCKED_VIDEO_ID) {
        player.loadVideoById({ videoId: LOCKED_VIDEO_ID, startSeconds: $startSeconds });
        Android.onWrongVideoBlocked();
      }
    }
  }, 1000);
</script>
</body>
</html>
""".trimIndent()

private fun releaseLockdown(activity: Activity?, nm: NotificationManager, prevFilter: Int) {
    try { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED } catch (_: Exception) {}
    try { activity?.stopLockTask() } catch (_: Exception) {}
    try { if (nm.isNotificationPolicyAccessGranted) nm.setInterruptionFilter(prevFilter) } catch (_: Exception) {}
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FocusScreen(
    videoId: String,
    startSeconds: Int,
    endSeconds: Int,
    onSessionComplete: () -> Unit,
    viewModel: FocusViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val nm = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    val prevFilter = remember { nm.currentInterruptionFilter }
    val repo = remember { SessionRepository(context) }
    val durationSeconds = endSeconds - startSeconds

    // Show a brief "blocked" snackbar when user tries to navigate to another video
    var showBlockedMessage by remember { mutableStateOf(false) }
    LaunchedEffect(showBlockedMessage) {
        if (showBlockedMessage) {
            kotlinx.coroutines.delay(2500)
            showBlockedMessage = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initializeTimer(durationSeconds)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        if (nm.isNotificationPolicyAccessGranted) nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        activity?.startLockTask()
    }

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            repo.saveSession(SessionRecord(videoId, durationSeconds, System.currentTimeMillis(), true))
            releaseLockdown(activity, nm, prevFilter)
            onSessionComplete()
        }
    }

    if (uiState.showEarlyExitDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissEarlyExitDialog() },
            title = { Text("End Session Early?") },
            text = { Text("You still have ${uiState.formattedTime} remaining.\n\nEvery minute you stay locked in compounds. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onDismissEarlyExitDialog()
                        repo.saveSession(SessionRecord(videoId, durationSeconds - uiState.remainingSeconds.toInt(), System.currentTimeMillis(), false))
                        releaseLockdown(activity, nm, prevFilter)
                        onSessionComplete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("End Anyway") }
            },
            dismissButton = { TextButton(onClick = { viewModel.onDismissEarlyExitDialog() }) { Text("Keep Going!") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        val html = remember(videoId, startSeconds, endSeconds) { buildHtml(videoId, startSeconds, endSeconds) }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(false)
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    }
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val url = request.url.toString()
                            val host = request.url.host ?: ""
                            // Block navigation to any YouTube page that isn't the locked video
                            if (host.contains("youtube.com") || host.contains("youtu.be")) {
                                val hasLockedId = url.contains(videoId)
                                if (!hasLockedId) {
                                    // Block it and snap back
                                    (ctx as? Activity)?.runOnUiThread { showBlockedMessage = true }
                                    return true
                                }
                            }
                            // Block all non-YouTube domains entirely
                            val allowed = listOf("youtube.com", "googlevideo.com", "ytimg.com", "google.com", "googleapis.com", "gstatic.com")
                            return allowed.none { host.contains(it) }
                        }
                    }
                    addJavascriptInterface(object : Any() {
                        @JavascriptInterface
                        fun onVideoEnded() {
                            (ctx as? Activity)?.runOnUiThread { viewModel.onShowEarlyExitDialog() }
                        }
                        @JavascriptInterface
                        fun onVideoPaused() {
                            (ctx as? Activity)?.runOnUiThread { viewModel.pauseTimer() }
                        }
                        @JavascriptInterface
                        fun onVideoResumed() {
                            (ctx as? Activity)?.runOnUiThread { viewModel.resumeTimer() }
                        }
                        @JavascriptInterface
                        fun onWrongVideoBlocked() {
                            (ctx as? Activity)?.runOnUiThread { showBlockedMessage = true }
                        }
                    }, "Android")
                    loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
                }
            }
        )

        // Timer pill — shows PAUSED state when video is paused
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 20.dp)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (uiState.isPaused) Color(0xFF1A0A00).copy(alpha = 0.75f)
                        else Color.Black.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                        CircularProgressIndicator(
                            progress = { uiState.progressFraction },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 3.dp,
                            strokeCap = StrokeCap.Round,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            color = if (uiState.isPaused) Color(0xFFFFB347) else MaterialTheme.colorScheme.primary
                        )
                        if (uiState.isPaused) {
                            Icon(Icons.Default.Pause, null, tint = Color(0xFFFFB347), modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        text = if (uiState.isPaused) "${uiState.formattedTime} · PAUSED" else uiState.formattedTime,
                        color = if (uiState.isPaused) Color(0xFFFFB347) else Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // "Blocked" toast when user tries to navigate to another video
        AnimatedVisibility(
            visible = showBlockedMessage,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    "🔒 Locked to your selected video",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // X button
        IconButton(
            onClick = { viewModel.onShowEarlyExitDialog() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .size(36.dp)
                .background(Color.Black.copy(alpha = 0.40f), shape = CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "End session", tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
        }
    }
}
