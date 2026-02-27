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

// JS injected after page load to hide YouTube header, search, nav — everything except the player
private val HIDE_CHROME_JS = """
(function injectHideCSS() {
    var id = 'df-hide-chrome';
    if (document.getElementById(id)) return;
    var s = document.createElement('style');
    s.id = id;
    s.textContent = [
        'ytm-mobile-topbar-renderer',
        'ytm-pivot-bar-renderer',
        '.mobile-topbar-renderer',
        '#appbar',
        'ytm-searchbox',
        '.searchbox',
        'ytm-slim-owner-renderer',
        'ytm-watch-metadata',
        'ytm-item-section-renderer',
        'ytm-section-list-renderer',
        'ytm-comments-entry-point-header-renderer',
        '.related-chips-bar-renderer',
        'ytm-reel-shelf-renderer',
        '.watch-below-the-fold',
        '.player-controls-background',
        'ytm-app-related-endpoint-renderer'
    ].join(',') + '{display:none!important;visibility:hidden!important;height:0!important;overflow:hidden!important;}' +
    'video,ytm-watch,.watch-player,#player-container-id,#player,.slim-video-player-container{width:100vw!important;max-width:100vw!important;}' +
    'body,html{overflow:hidden!important;background:#000!important;}';
    document.head.appendChild(s);
})();
""".trimIndent()

// JS to dismiss consent banners + re-hide elements that YouTube re-renders dynamically
private val POLL_JS = """
(function startPoll() {
    function hideAll() {
        ['ytm-mobile-topbar-renderer','ytm-pivot-bar-renderer','#appbar','.mobile-topbar-renderer']
            .forEach(function(sel) {
                document.querySelectorAll(sel).forEach(function(el){ el.style.display='none'; });
            });
        // Dismiss consent banners
        document.querySelectorAll('button[aria-label*="Accept"],button[aria-label*="Agree"],.consent-bump-v2-button-button')
            .forEach(function(b){ b.click(); });
    }
    hideAll();
    setInterval(hideAll, 1500);
})();
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
        if (nm.isNotificationPolicyAccessGranted)
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
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
                        repo.saveSession(SessionRecord(videoId,
                            durationSeconds - uiState.remainingSeconds.toInt(),
                            System.currentTimeMillis(), false))
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

        // Build URL — real mobile YouTube watch page, NOT embed
        // This bypasses the embedding restriction entirely
        val url = remember(videoId, startSeconds) {
            "https://m.youtube.com/watch?v=$videoId&t=${startSeconds}s"
        }

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
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        // Mobile Chrome UA — YouTube serves working video player
                        userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36"
                    }

                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    webChromeClient = object : WebChromeClient() {
                        // Required for video to go fullscreen inside WebView
                        private var customView: android.view.View? = null
                        private var customViewCallback: CustomViewCallback? = null

                        override fun onShowCustomView(view: android.view.View, callback: CustomViewCallback) {
                            customView = view
                            customViewCallback = callback
                            (ctx as? Activity)?.window?.decorView?.let { decor ->
                                (decor as? android.view.ViewGroup)?.addView(view)
                            }
                            (ctx as? Activity)?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                        }

                        override fun onHideCustomView() {
                            (ctx as? Activity)?.window?.decorView?.let { decor ->
                                (decor as? android.view.ViewGroup)?.removeView(customView)
                            }
                            customViewCallback?.onCustomViewHidden()
                            customView = null
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val reqUrl = request.url.toString()
                            val host = request.url.host ?: ""

                            // Block navigation to any different video
                            if (host.contains("youtube.com") || host.contains("youtu.be")) {
                                if (!reqUrl.contains(videoId)) {
                                    (ctx as? Activity)?.runOnUiThread { showBlockedMessage = true }
                                    return true // block it
                                }
                                return false // allow same video navigation (quality change, etc)
                            }

                            // Allow YouTube CDN domains
                            val allowed = listOf("youtube.com", "googlevideo.com", "ytimg.com",
                                "google.com", "googleapis.com", "gstatic.com",
                                "ggpht.com", "googleusercontent.com")
                            return allowed.none { host.contains(it) }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Inject CSS to hide header/search/nav
                            view?.evaluateJavascript(HIDE_CHROME_JS, null)
                            // Start polling to re-hide dynamic elements + dismiss banners
                            view?.evaluateJavascript(POLL_JS, null)

                            // Also set up a JS interval to detect play/pause state
                            // YouTube mobile uses HTML5 video element directly
                            view?.evaluateJavascript("""
                                (function() {
                                    function attachVideoListeners() {
                                        var video = document.querySelector('video');
                                        if (!video) { setTimeout(attachVideoListeners, 500); return; }
                                        video.addEventListener('play', function() { Android.onVideoResumed(); });
                                        video.addEventListener('pause', function() { Android.onVideoPaused(); });
                                        video.addEventListener('playing', function() { Android.onVideoResumed(); });
                                        video.addEventListener('waiting', function() { Android.onVideoResumed(); });
                                        
                                        // Enforce end time for non-live videos
                                        if (${endSeconds} > 0) {
                                            setInterval(function() {
                                                if (video.currentTime >= ${endSeconds}) {
                                                    video.pause();
                                                    Android.onVideoEnded();
                                                }
                                            }, 1000);
                                        }
                                    }
                                    attachVideoListeners();
                                })();
                            """.trimIndent(), null)
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
                    }, "Android")

                    loadUrl(url)
                }
            }
        )

        // Timer pill — bottom left, orange when paused
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 20.dp)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (uiState.isPaused) Color(0xFF2A1500).copy(alpha = 0.85f)
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
                            color = if (uiState.isPaused) Color(0xFFFFB347)
                                    else MaterialTheme.colorScheme.primary
                        )
                        if (uiState.isPaused) {
                            Icon(Icons.Default.Pause, null, tint = Color(0xFFFFB347), modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        text = if (uiState.isPaused) "${uiState.formattedTime} · PAUSED"
                               else uiState.formattedTime,
                        color = if (uiState.isPaused) Color(0xFFFFB347) else Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Blocked toast
        AnimatedVisibility(
            visible = showBlockedMessage,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp)
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
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
            Icon(Icons.Default.Close, contentDescription = "End session",
                tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
        }
    }
}
