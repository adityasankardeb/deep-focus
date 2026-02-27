package com.aditya.deepfocus.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aditya.deepfocus.R
import com.aditya.deepfocus.ui.components.SocialIconButton
import com.aditya.deepfocus.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onStartSession: (String, Int) -> Unit, viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationManager = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    if (uiState.showDndPermissionRationale) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissDndRationale() },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("DND Permission Required") },
            text = { Text("Deep Focus needs Do Not Disturb access to silence all notifications during your study session.\n\nYou'll be taken to Settings. Please grant the permission and return.") },
            confirmButton = { TextButton(onClick = { viewModel.onDismissDndRationale(); context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }) { Text("Open Settings") } },
            dismissButton = { TextButton(onClick = { viewModel.onDismissDndRationale() }) { Text("Cancel") } }
        )
    }

    if (uiState.showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissPrivacyDialog() },
            title = { Text("Privacy Policy") },
            text = { Text("This app requires Do Not Disturb and Screen Pinning permissions strictly for local focus sessions. These permissions are used only while a session is active and are immediately restored upon session completion.\n\nNo personal data is collected, stored, or transmitted. No accounts, analytics, or third-party SDKs are used.") },
            confirmButton = { TextButton(onClick = { viewModel.onDismissPrivacyDialog() }) { Text("Got It") } }
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(48.dp))
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(80.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(40.dp)) }
            }
            Spacer(Modifier.height(20.dp))
            Text("Deep Focus", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("Lock in. No distractions. Just you and the lecture.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
            Spacer(Modifier.height(36.dp))
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(value = uiState.youtubeUrl, onValueChange = viewModel::onUrlChanged, label = { Text("YouTube Lecture URL") }, placeholder = { Text("https://youtube.com/watch?v=...") }, leadingIcon = { Icon(Icons.Default.VideoLibrary, null) }, isError = uiState.urlError != null, supportingText = { uiState.urlError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                    OutlinedTextField(value = uiState.durationMinutes, onValueChange = viewModel::onDurationChanged, label = { Text("Duration (Minutes)") }, placeholder = { Text("25") }, leadingIcon = { Icon(Icons.Default.Timer, null) }, isError = uiState.durationError != null, supportingText = { if (uiState.durationError != null) Text(uiState.durationError!!, color = MaterialTheme.colorScheme.error) else Text("Max 480 minutes", color = MaterialTheme.colorScheme.onSurfaceVariant) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Text("Once started, Home and Recents buttons will be disabled until the timer ends.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { val inputs = viewModel.validateAndGetInputs() ?: return@Button; if (!notificationManager.isNotificationPolicyAccessGranted) { viewModel.onShowDndRationale(); return@Button }; onStartSession(inputs.first, inputs.second) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(58.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(10.dp)); Text("Start Deep Work", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
