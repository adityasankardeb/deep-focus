package com.aditya.deepfocus.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.aditya.deepfocus.ui.viewmodel.FocusViewModel

private fun buildYouTubeHtml(videoId: String, startSeconds: Int, endSeconds: Int) = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; }
  iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none; }
</style>
</head>
<body>
<iframe
  src="https://www.youtube.com/embed/${videoId}?autoplay=1&start=${startSeconds}&end=${endSeconds}&controls=1&playsinline=1&rel=0&modestbranding=1&enablejsapi=1&widgetid=1"
  allow="autoplay; fullscreen; encrypted-media; picture-in-picture"
  allowfullscreen
  frameborder="0">
</iframe>
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
    val durationSeconds = endSeconds - startSeconds

    LaunchedEffect(Unit) {
        viewModel.initializeTimer(durationSeconds)
        // Force landscape for video
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        if (nm.isNotificationPolicyAccessGranted) {
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

    // Early exit confirmation dialog
    if (uiState.showEarlyExitDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissEarlyExitDialog() },
            title = { Text("End Session Early?") },
            text = { Text("You still have ${uiState.formattedTime} remaining.\n\nEvery minute you stay locked in compounds. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onDismissEarlyExitDialog()
                        releaseLockdown(activity, nm, prevFilter)
                        onSessionComplete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("End Anyway") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissEarlyExitDialog() }) { Text("Keep Going!") }
            }
        )
    }

    // Full screen box — video takes everything, overlays on top
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── YouTube WebView (full screen) ─────────────────────────────────
        val html = remember(videoId, startSeconds, endSeconds) {
            buildYouTubeHtml(videoId, startSeconds, endSeconds)
        }

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
                        allowContentAccess = true
                        allowFileAccess = false
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        setSupportZoom(false)
                        displayZoomControls = false
                        userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView, request: WebResourceRequest
                        ): Boolean {
                            val host = request.url.host ?: ""
                            return !host.contains("youtube.com") &&
                                   !host.contains("googlevideo.com") &&
                                   !host.contains("ytimg.com") &&
                                   !host.contains("google.com")
                        }
                    }
                    loadDataWithBaseURL(
                        "https://www.youtube.com",
                        html,
                        "text/html",
                        "utf-8",
                        null
                    )
                }
            }
        )

        // ── Timer overlay — bottom-left corner ───────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier.wrapContentSize()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mini circular progress
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                        CircularProgressIndicator(
                            progress = { uiState.progressFraction },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 3.dp,
                            strokeCap = StrokeCap.Round,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = uiState.formattedTime,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // ── Small X button — top-right corner ────────────────────────────
        IconButton(
            onClick = { viewModel.onShowEarlyExitDialog() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .size(36.dp)
                .background(Color.Black.copy(alpha = 0.40f), shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "End session",
                tint = Color.White.copy(alpha = 0.60f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
