package com.streamdock.pro.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = PurpleGrey80,
    tertiary = StatusGreen,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = TextHigh,
    onSurface = TextHigh,
)

@Composable
fun StreamDockTheme(
    content: @Composable () -> Unit,
) {
    // La UI de Stream Dock Pro es siempre oscura (Obsidian #0F0F14).
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
