package com.aditya.deepfocus.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {

    val iconScale = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val ringScale = remember { Animatable(0.6f) }
    val ringAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Icon bounces in
        iconScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        // Ring expands and fades out
        ringAlpha.animateTo(1f, animationSpec = tween(200))
        ringScale.animateTo(1.6f, animationSpec = tween(600, easing = EaseOut))
        ringAlpha.animateTo(0f, animationSpec = tween(400))
        // Title fades in
        textAlpha.animateTo(1f, animationSpec = tween(500))
        delay(200)
        subtitleAlpha.animateTo(1f, animationSpec = tween(500))
        delay(900)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0F0D), Color(0xFF0D1F18))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                // Expanding ring
                Surface(
                    modifier = Modifier.size(120.dp).scale(ringScale.value),
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xFF6EE7B7).copy(alpha = ringAlpha.value * 0.3f)
                ) {}

                // Main icon box
                Surface(
                    modifier = Modifier.size(88.dp).scale(iconScale.value),
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFF003728)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF6EE7B7),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "Deep Focus",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = textAlpha.value),
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Lock in. No distractions.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF6EE7B7).copy(alpha = subtitleAlpha.value),
                textAlign = TextAlign.Center
            )
        }

        // Bottom branding
        Text(
            "by Aditya Sankar Deb",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = subtitleAlpha.value * 0.4f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
        )
    }
}
