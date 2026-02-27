package com.aditya.deepfocus.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aditya.deepfocus.R
import com.aditya.deepfocus.data.SessionRepository
import com.aditya.deepfocus.ui.components.SocialIconButton
import com.aditya.deepfocus.ui.components.TimePickerDialog
import com.aditya.deepfocus.ui.viewmodel.HomeViewModel
import com.aditya.deepfocus.ui.viewmodel.extractYouTubeId
import com.aditya.deepfocus.ui.viewmodel.toTimeString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartSession: (String, Int, Int) -> Unit,
    onViewHistory: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val notificationManager = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    val repo = remember { SessionRepository(context) }
    val streak = remember { repo.getCurrentStreak() }

    // Dial pickers
    if (uiState.showStartPicker) {
        TimePickerDialog(
            title = "Set Start Time",
            initialSeconds = uiState.startSeconds,
            maxSeconds = (uiState.endSeconds - 10).coerceAtLeast(0),
            onConfirm = { h, m, s -> viewModel.onStartTimeConfirmed(h, m, s) },
            onDismiss = { viewModel.onDismissStartPicker() }
        )
    }

    if (uiState.showEndPicker) {
        TimePickerDialog(
            title = "Set End Time",
            initialSeconds = uiState.endSeconds,
            maxSeconds = uiState.videoDurationSeconds,
            onConfirm = { h, m, s -> viewModel.onEndTimeConfirmed(h, m, s) },
            onDismiss = { viewModel.onDismissEndPicker() }
        )
    }

    if (uiState.showDndPermissionRationale) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissDndRationale() },
            icon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("DND Permission Required") },
            text = { Text("Deep Focus needs Do Not Disturb access to block notification sounds during your session.\n\nYour lecture audio will still play normally.") },
            confirmButton = { TextButton(onClick = { viewModel.onDismissDndRationale(); context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }) { Text("Open Settings") } },
            dismissButton = { TextButton(onClick = { viewModel.onDismissDndRationale() }) { Text("Cancel") } }
        )
    }

    if (uiState.showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissPrivacyDialog() },
            title = { Text("Privacy Policy") },
            text = { Text("This app requires Do Not Disturb and Screen Pinning permissions strictly for local focus sessions. No personal data is collected, stored, or transmitted.") },
            confirmButton = { TextButton(onClick = { viewModel.onDismissPrivacyDialog() }) { Text("Got It") } }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    if (streak > 0) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Whatshot, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Text("$streak day streak", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                    IconButton(onClick = onViewHistory) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(80.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Deep Focus", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("Lock in. No distractions. Just you and the lecture.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
            Spacer(Modifier.height(28.dp))

            // URL input card
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.youtubeUrl,
                        onValueChange = viewModel::onUrlChanged,
                        label = { Text("YouTube Lecture URL") },
                        placeholder = { Text("https://youtube.com/watch?v=...") },
                        leadingIcon = { Icon(Icons.Default.VideoLibrary, null) },
                        trailingIcon = {
                            if (uiState.youtubeUrl.isNotBlank() && extractYouTubeId(uiState.youtubeUrl) != null) {
                                if (uiState.isFetchingDuration) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    IconButton(onClick = { keyboard?.hide(); viewModel.fetchVideoDuration() }) {
                                        Icon(Icons.Default.Search, "Load video info", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        },
                        isError = uiState.urlError != null,
                        supportingText = { uiState.urlError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); viewModel.fetchVideoDuration() }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    if (uiState.videoDurationSeconds == 0 && !uiState.isFetchingDuration && uiState.urlError == null) {
                        Text(
                            if (uiState.youtubeUrl.isBlank()) "Paste a YouTube URL to get started"
                            else "Tap the 🔍 icon to load video info",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    uiState.fetchError?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
                }
            }

            // Section selector card — shows after video loaded
            AnimatedVisibility(visible = uiState.videoDurationSeconds > 0, enter = fadeIn() + expandVertically()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        // Video title
                        uiState.videoTitle?.let { title ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.PlayCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (uiState.isLiveStream) { Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.error) { Text("  LIVE  ", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold) } }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        Text("Select Section to Watch", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)

                        // Session summary pill
                        val sessionDuration = uiState.endSeconds - uiState.startSeconds
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("${uiState.startSeconds.toTimeString()}  →  ${uiState.endSeconds.toTimeString()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("Session: ${sessionDuration.toTimeString()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                }
                                Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            }
                        }

                        // Dial time buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Start time button
                            OutlinedButton(
                                onClick = { viewModel.onShowStartPicker() },
                                modifier = Modifier.weight(1f).height(64.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("▶ START", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(uiState.startSeconds.toTimeString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            // End time button
                            OutlinedButton(
                                onClick = { viewModel.onShowEndPicker() },
                                modifier = Modifier.weight(1f).height(64.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("⏹ END", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(uiState.endSeconds.toTimeString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }

                        // Quick presets
                        Text("Quick Presets", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "Full" to Pair(0, uiState.videoDurationSeconds),
                                "First Half" to Pair(0, uiState.videoDurationSeconds / 2),
                                "Second Half" to Pair(uiState.videoDurationSeconds / 2, uiState.videoDurationSeconds)
                            ).forEach { (label, range) ->
                                FilterChip(
                                    selected = uiState.startSeconds == range.first && uiState.endSeconds == range.second,
                                    onClick = { viewModel.applyPreset(range.first, range.second) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Text("Once started, Home & Recents are disabled until session ends.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val inputs = viewModel.validateAndGetInputs() ?: return@Button
                    if (!notificationManager.isNotificationPolicyAccessGranted) { viewModel.onShowDndRationale(); return@Button }
                    onStartSession(inputs.first, inputs.second, inputs.third)
                },
                enabled = uiState.videoDurationSeconds > 0 && !uiState.isFetchingDuration,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(58.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Start Deep Work", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(32.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))
            Text("Built by Aditya", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text("© 2026 All Rights Reserved Aditya Sankar Deb", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                SocialIconButton(R.drawable.ic_linkedin, "LinkedIn", "https://www.linkedin.com/in/aditya-sankar-deb-a276143b1", context)
                SocialIconButton(R.drawable.ic_instagram, "Instagram", "https://instagram.com/adityasankardeb_", context)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { viewModel.onShowPrivacyDialog() }) { Text("Privacy Policy", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
