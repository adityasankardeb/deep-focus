package com.aditya.deepfocus.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aditya.deepfocus.ui.viewmodel.FocusViewModel

private fun buildYouTubeHtml(videoId: String, startSeconds: Int, endSeconds: Int) = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { background: #000; overflow: hidden; }
  #player { width: 100%; height: 100vh; }
</style>
</head>
<body>
<div id="player"></div>
<script>
  var tag = document.createElement('script');
  tag.src = "https://www.youtube.com/iframe_api";
  var firstScriptTag = document.getElementsByTagName('script')[0];
  firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

  var player;
  var endTime = $endSeconds;

  function onYouTubeIframeAPIReady() {
    player = new YT.Player('player', {
      videoId: '$videoId',
      playerVars: {
        'autoplay': 1,
        'playsinline': 1,
        'controls': 1,
        'rel': 0,
        'modestbranding': 1,
        'start': $startSeconds,
        'end': $endSeconds,
        'fs': 1,
        'enablejsapi': 1,
        'origin': 'https://www.youtube.com'
      },
      events: {
        'onReady': function(event) {
          event.target.setVolume(100);
          event.target.unMute();
          event.target.playVideo();
        },
        'onStateChange': function(event) {
          if (event.data == YT.PlayerState.ENDED) {
            Android.onVideoEnded();
          }
        }
      }
    });
  }

  setInterval(function() {
    if (player && player.getCurrentTime) {
      var current = player.getCurrentTime();
      if (current >= endTime) {
        player.pauseVideo();
        Android.onVideoEnded();
      }
    }
  }, 1000);
</script>
</body>
</html>
""".trimIndent()

private fun releaseLockdown(activity: Activity?, nm: NotificationManager, prevFilter: Int) {
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

    val durationSeconds = endSeconds - startSeconds

    LaunchedEffect(Unit) {
        viewModel.initializeTimer(durationSeconds)
        if (nm.isNotificationPolicyAccessGranted) {
            // INTERRUPTION_FILTER_PRIORITY allows media audio but blocks notification sounds
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
        activity?.startLockTask()
    }

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
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
                    onClick = { viewModel.onDismissEarlyExitDialog(); releaseLockdown(activity, nm, prevFilter); onSessionComplete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("End Anyway") }
            },
            dismissButton = { TextButton(onClick = { viewModel.onDismissEarlyExitDialog() }) { Text("Keep Going!") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── WebView Player ────────────────────────────────────────────────────
        val html = remember(videoId, startSeconds, endSeconds) {
            buildYouTubeHtml(videoId, startSeconds, endSeconds)
        }

        var webViewRef by remember { mutableStateOf<WebView?>(null) }

        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.50f)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        // Hardware acceleration is critical for video playback
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            allowContentAccess = true
                            allowFileAccess = true
                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {}
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val host = request.url.host ?: ""
                                return !host.contains("youtube.com") &&
                                       !host.contains("googlevideo.com") &&
                                       !host.contains("ytimg.com") &&
                                       !host.contains("google.com")
                            }
                        }
                        // JS Interface so the page can notify when video ends
                        addJavascriptInterface(object : Any() {
                            @android.webkit.JavascriptInterface
                            fun onVideoEnded() {
                                (ctx as? Activity)?.runOnUiThread {
                                    viewModel.onShowEarlyExitDialog()
                                }
                            }
                        }, "Android")
                        loadDataWithBaseURL(
                            "https://www.youtube.com",
                            html,
                            "text/html",
                            "utf-8",
                            null
                        )
                        webViewRef = this
                    }
                }
            )
        }

        // ── Timer Section ────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("FOCUS SESSION", style = MaterialTheme.typography.labelLarge, letterSpacing = 3.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text(uiState.formattedTime, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                CircularProgressIndicator(
                    progress = { uiState.progressFraction },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(Icons.Default.HourglassEmpty, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Stay locked in. Every minute counts.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            OutlinedButton(
                onClick = { viewModel.onShowEarlyExitDialog() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("End Session Early") }
        }
    }
}
