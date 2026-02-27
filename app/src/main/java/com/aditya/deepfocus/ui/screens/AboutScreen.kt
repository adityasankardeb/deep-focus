package com.aditya.deepfocus.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f, targetValue = 1.06f, label = "scale",
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About & Partner With Us", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Hero banner ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF003728), Color(0xFF0D1F18))))
                    .padding(vertical = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(100.dp).scale(pulseScale),
                            shape = CircleShape,
                            color = Color(0xFF6EE7B7).copy(alpha = 0.10f)
                        ) {}
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = Color(0xFF003728)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF6EE7B7),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Deep Focus",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Lock in. No distractions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6EE7B7)
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF6EE7B7).copy(alpha = 0.15f)
                    ) {
                        Text(
                            "  v1.0  ·  Built for Indian Students  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6EE7B7),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── What is Deep Focus ────────────────────────────────────────
            AboutCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                    Text("What is Deep Focus?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Deep Focus is a distraction-blocking study app built specifically for Indian exam students — " +
                    "JEE, NEET, BITSAT, UPSC, and beyond.\n\n" +
                    "A student pastes any YouTube lecture URL, sets the section they want to watch, and the app " +
                    "locks the phone until they finish. No notifications. No switching apps. No YouTube recommendations " +
                    "pulling them away. Just the lecture.\n\n" +
                    "It works with every channel including live streams — even channels that block embedding in third-party players.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Stats ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AboutStat(modifier = Modifier.weight(1f), value = "100%", label = "Ad-free")
                AboutStat(modifier = Modifier.weight(1f), value = "0", label = "Data collected")
                AboutStat(modifier = Modifier.weight(1f), value = "∞", label = "Channels")
            }

            Spacer(Modifier.height(16.dp))

            // ── Business pitch card ───────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                            Icon(
                                Icons.Default.Business, null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(8.dp).size(16.dp)
                            )
                        }
                        Text(
                            "For Edtech Companies & Investors",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "If you're reading this inside the app — this is exactly the kind of focused, " +
                        "purpose-built tool your students need.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "The biggest drop-off in online education isn't content quality — it's distraction. " +
                        "A student opens a lecture and quits 12 minutes in because Instagram sent a notification. " +
                        "Deep Focus solves that at the OS level.\n\n" +
                        "Built by a student, for students, from scratch — Kotlin, Jetpack Compose, system-level " +
                        "Android APIs. Clean codebase, zero paid SDK dependencies, ready to white-label or " +
                        "integrate into an existing platform in days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))

                    Text("What's on the table:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(10.dp))

                    listOf(
                        "Full source code acquisition" to
                            "Own the codebase outright. White-label it, integrate it, ship it as your own.",
                        "Licensing deal" to
                            "Pay per active user or a flat monthly fee. I retain ownership, you get usage rights.",
                        "Integration contract" to
                            "Hire me to build this directly into your platform or app as a feature."
                    ).forEach { (title, desc) ->
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp).padding(top = 2.dp))
                            Column {
                                Text(title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold)
                                Text(desc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Email CTA
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:debaditya125@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Deep Focus — Partnership Inquiry")
                                putExtra(Intent.EXTRA_TEXT,
                                    "Hi Aditya,\n\nI came across Deep Focus and I'm interested in discussing a potential partnership.\n\nCompany/Organization: \nWhat we're looking for: \n\nLet's connect.")
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Email Me to Discuss",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(10.dp))

                    // LinkedIn CTA
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://www.linkedin.com/in/aditya-sankar-deb-a276143b1"))
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Connect on LinkedIn",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Built by ──────────────────────────────────────────────────
            AboutCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Person, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                    Text("Built by",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Aditya Sankar Deb — student developer from India. Built Deep Focus as a personal tool " +
                    "for BITSAT prep, then realised every student in the country needs it.\n\n" +
                    "Open to partnerships, acquisitions, freelance contracts, and full-time roles in " +
                    "product/Android development.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AboutContactChip(icon = Icons.Default.Email, label = "Email") {
                        val i = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:debaditya125@gmail.com")
                        }
                        try { context.startActivity(i) } catch (_: Exception) {}
                    }
                    AboutContactChip(icon = Icons.Default.OpenInNew, label = "LinkedIn") {
                        val i = Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.linkedin.com/in/aditya-sankar-deb-a276143b1"))
                        try { context.startActivity(i) } catch (_: Exception) {}
                    }
                    AboutContactChip(icon = Icons.Default.Code, label = "GitHub") {
                        val i = Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/adityasankardeb/deep-focus"))
                        try { context.startActivity(i) } catch (_: Exception) {}
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "© 2026 Aditya Sankar Deb · All rights reserved",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AboutCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun AboutStat(modifier: Modifier = Modifier, value: String, label: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text(label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AboutContactChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp))
            Text(label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}
