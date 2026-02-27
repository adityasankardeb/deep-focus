package com.aditya.deepfocus.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DeepFocusDarkColorScheme = darkColorScheme(
    primary = Color(0xFF6EE7B7), onPrimary = Color(0xFF00251A),
    primaryContainer = Color(0xFF003728), onPrimaryContainer = Color(0xFFA7F3D0),
    background = Color(0xFF0A0F0D), onBackground = Color(0xFFE2F5EE),
    surface = Color(0xFF111815), onSurface = Color(0xFFDCF0E9),
    surfaceVariant = Color(0xFF1E2E27), onSurfaceVariant = Color(0xFF8BADA0),
    surfaceContainer = Color(0xFF162119), outline = Color(0xFF3D5A50),
    outlineVariant = Color(0xFF253D35), error = Color(0xFFFF7070),
    onError = Color(0xFF3B0000), errorContainer = Color(0xFF5C1010), onErrorContainer = Color(0xFFFFB4AB)
)

private val DeepFocusLightColorScheme = lightColorScheme(
    primary = Color(0xFF007050), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F0D8), onPrimaryContainer = Color(0xFF002116),
    background = Color(0xFFF4FBF7), onBackground = Color(0xFF0C1F17),
    surface = Color(0xFFF4FBF7), onSurface = Color(0xFF0C1F17),
    surfaceVariant = Color(0xFFDCEDE5), onSurfaceVariant = Color(0xFF3E5549),
    surfaceContainer = Color(0xFFE8F4EE), outline = Color(0xFF6E9483),
    outlineVariant = Color(0xFFBECFC8), error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF), errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002)
)

@Composable
fun DeepFocusTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { val context = LocalContext.current; if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) }
        darkTheme -> DeepFocusDarkColorScheme
        else -> DeepFocusLightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography(), content = content)
}
