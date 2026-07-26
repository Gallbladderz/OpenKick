package com.gallbladderz.openkick.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.gallbladderz.openkick.core.datastore.AppTheme

private val KickDarkColorScheme = darkColorScheme(
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

private val KickLightColorScheme = lightColorScheme(
    primary = KickGreenDark,
    onPrimary = Color.White,
    secondary = KickGreen,
    onSecondary = Color.Black,
    background = Color(0xFFF0F0F0),
    onBackground = Color(0xFF141416),
    surface = Color.White,
    onSurface = Color(0xFF141416),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF424242),
    error = KickError,
    onError = Color.White
)
private val CatppuccinMochaColorScheme = darkColorScheme(
    primary = CatppuccinMochaPrimary,
    onPrimary = CatppuccinMochaOnPrimary,
    background = CatppuccinMochaBackground,
    onBackground = CatppuccinMochaText,
    surface = CatppuccinMochaSurface,
    onSurface = CatppuccinMochaText,
    surfaceVariant = CatppuccinMochaSurface,
    onSurfaceVariant = CatppuccinMochaText,
    error = KickError,
    onError = Color.White
)

private val CatppuccinLatteColorScheme = lightColorScheme(
    primary = CatppuccinLattePrimary,
    onPrimary = CatppuccinLatteOnPrimary,
    background = CatppuccinLatteBackground,
    onBackground = CatppuccinLatteText,
    surface = CatppuccinLatteSurface,
    onSurface = CatppuccinLatteText,
    surfaceVariant = CatppuccinLatteSurface,
    onSurfaceVariant = CatppuccinLatteText,
    error = KickError,
    onError = Color.White
)

@Composable
fun OpenKickTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    useDynamicColors: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.DARK, AppTheme.CATPPUCCIN_MOCHA -> true
        AppTheme.LIGHT, AppTheme.CATPPUCCIN_LATTE -> false
    }

    val colorScheme = when {
        appTheme == AppTheme.CATPPUCCIN_MOCHA -> CatppuccinMochaColorScheme
        appTheme == AppTheme.CATPPUCCIN_LATTE -> CatppuccinLatteColorScheme
        useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> KickDarkColorScheme
        else -> KickLightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}