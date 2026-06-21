package com.gallbladderz.openkick.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val KickColorScheme = darkColorScheme(
    primary = KickGreen,
    onPrimary = Color.Black,
    secondary = KickGreenDark,
    onSecondary = Color.White,
    background = KickBackground,
    onBackground = KickTextPrimary,
    surface = KickSurface,
    onSurface = KickTextPrimary,
    surfaceVariant = KickSurfaceVariant,
    onSurfaceVariant = KickTextSecondary,
    error = KickError,
    onError = Color.White
)

@Composable
fun OpenKickTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = KickColorScheme.background.toArgb()
            window.navigationBarColor = KickColorScheme.background.toArgb()

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = KickColorScheme,
        typography = Typography,
        content = content
    )
}