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
import com.shifthackz.catppuccin.compose.CatppuccinMaterial
import com.shifthackz.catppuccin.compose.colorScheme
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
@Composable
fun OpenKickTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    useDynamicColors: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT, AppTheme.CATPPUCCIN_LATTE -> false
        AppTheme.DARK, AppTheme.CATPPUCCIN_FRAPPE, AppTheme.CATPPUCCIN_MACCHIATO, AppTheme.CATPPUCCIN_MOCHA -> true
    }

    val colorScheme = when {
        appTheme == AppTheme.CATPPUCCIN_LATTE -> CatppuccinMaterial.Latte().colorScheme()
        appTheme == AppTheme.CATPPUCCIN_FRAPPE -> CatppuccinMaterial.Frappe().colorScheme()
        appTheme == AppTheme.CATPPUCCIN_MACCHIATO -> CatppuccinMaterial.Macchiato().colorScheme()
        appTheme == AppTheme.CATPPUCCIN_MOCHA -> CatppuccinMaterial.Mocha().colorScheme()
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
            window.decorView.setBackgroundColor(colorScheme.background.toArgb())

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