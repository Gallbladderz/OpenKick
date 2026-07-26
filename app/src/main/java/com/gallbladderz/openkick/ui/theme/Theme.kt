package com.gallbladderz.openkick.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
    onError = Color.White,
    tertiary = KickGradientStart,
    onTertiary = KickGradientEnd
)

private val LightColorScheme = lightColorScheme(
    primary = KickGreen,
    onPrimary = Color.Black,
    secondary = KickGreenDark,
    onSecondary = Color.White,
    background = KickBackgroundLight,
    onBackground = KickTextPrimaryLight,
    surface = KickSurfaceLight,
    onSurface = KickTextPrimaryLight,
    surfaceVariant = KickSurfaceVariantLight,
    onSurfaceVariant = KickTextSecondaryLight,
    error = KickErrorLight,
    onError = Color.White,
    tertiary = KickGradientStart,
    onTertiary = KickGradientEnd
)

@Composable
fun OpenKickTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) KickColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}