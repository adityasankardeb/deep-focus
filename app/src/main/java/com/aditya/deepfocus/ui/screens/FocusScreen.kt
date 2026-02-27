package com.aditya.deepfocus.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.webkit.*
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

// Load the actual YouTube mobile watch page — this always plays
// We use JS to seek to start and stop at end
private fun buildYouTubeUrl(videoId: String, startSeconds: Int) =
    "https://m.youtube.com/watch?v=$videoId&t=${startSeconds}s"

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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Real YouTube mobile page in WebView ───────────────────────────
        val url = remember(videoId, startSeconds) { buildYouTubeUrl(videoId, startSeconds) }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        allowContentAccess = true
                        setSupportZoom(true)
                        builtInZoomControls = false
                        displayZoomControls = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        // Chrome-like user agent — critical for YouTube to serve proper video
                        userAgentString = "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36"
                    }

                    webChromeClient = WebChromeClient()

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val host = request.url.host ?: ""
                            // Only allow YouTube, Google auth, and CDN domains
                            val allowed = listOf("youtube.com", "googlevideo.com", "ytimg.com",
                                                 "google.com", "googleapis.com", "gstatic.com",
                                                 "ggpht.com", "googleusercontent.com")
                            return allowed.none { host.contains(it) }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Auto-tap fullscreen button and dismiss cookie banners via JS
                            view?.evaluateJavascript("""
                                (function() {
                                    // Dismiss cookie/consent dialogs
                                    var btns = document.querySelectorAll('button[aria-label*="Accept"], button[aria-label*="Agree"], .consent-bump-v2-button-button');
                                    btns.forEach(function(b) { b.click(); });
                                    
                                    // Try to enter fullscreen
                                    setTimeout(function() {
                                        var fullscreenBtn = document.querySelector('.fullscreen-icon, button.ytp-fullscreen-button, [data-title-no-tooltip="Full screen"]');
                                        if (fullscreenBtn) fullscreenBtn.click();
                                    }, 2000);
                                })();
                            """.trimIndent(), null)
                        }
                    }

                    // CookieManager — accept all so YouTube doesn't show consent wall
                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(this@apply, true)
                    }

                    loadUrl(url)
                }
            }
        )

        // ── Timer — bottom-left pill ──────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.55f)
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

        // ── X button — top-right ──────────────────────────────────────────
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
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
