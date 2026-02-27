package com.aditya.deepfocus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    title: String,
    initialSeconds: Int,
    maxSeconds: Int,
    onConfirm: (h: Int, m: Int, s: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialH = initialSeconds / 3600
    val initialM = (initialSeconds % 3600) / 60
    val initialS = initialSeconds % 60

    // We use two NumberPicker-style drums: MM and SS
    // Hours shown only if video > 1 hour
    var hours by remember { mutableIntStateOf(initialH) }
    var minutes by remember { mutableIntStateOf(initialM) }
    var seconds by remember { mutableIntStateOf(initialS) }

    val maxH = maxSeconds / 3600
    val showHours = maxSeconds >= 3600

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Max: ${maxSeconds / 3600}h ${(maxSeconds % 3600) / 60}m ${maxSeconds % 60}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showHours) {
                        NumberDrum(
                            value = hours,
                            onValueChange = { hours = it },
                            range = 0..maxH,
                            label = "HH"
                        )
                        Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    NumberDrum(
                        value = minutes,
                        onValueChange = { minutes = it },
                        range = 0..59,
                        label = "MM"
                    )
                    Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    NumberDrum(
                        value = seconds,
                        onValueChange = { seconds = it },
                        range = 0..59,
                        label = "SS"
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Preview
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (showHours)
                            String.format("%02d:%02d:%02d", hours, minutes, seconds)
                        else
                            String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Cancel") }
                    Button(
                        onClick = { onConfirm(hours, minutes, seconds) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Set") }
                }
            }
        }
    }
}

@Composable
private fun NumberDrum(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.width(72.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Up button
                TextButton(
                    onClick = { if (value < range.last) onValueChange(value + 1) },
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text("▲", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }

                // Value display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        String.format("%02d", value),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Down button
                TextButton(
                    onClick = { if (value > range.first) onValueChange(value - 1) },
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text("▼", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
