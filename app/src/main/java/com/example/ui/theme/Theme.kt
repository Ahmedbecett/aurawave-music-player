package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF005143),
    onPrimaryContainer = Color(0xFF6FFFE4),
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3B185F),
    onSecondaryContainer = Color(0xFFE0AAFF),
    tertiary = NeonPink,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    outlineVariant = Color(0xFF38405E)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007A69),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF86F8E2),
    secondary = Color(0xFF7B2CBF),
    onSecondary = Color.White,
    tertiary = Color(0xFFB5179E),
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF141724),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141724),
    surfaceVariant = Color(0xFFE8ECF5),
    onSurfaceVariant = Color(0xFF5B647A),
    outline = Color(0xFFD0D7E5)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to stunning music dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
