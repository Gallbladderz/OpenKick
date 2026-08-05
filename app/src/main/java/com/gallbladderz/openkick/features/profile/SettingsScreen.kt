package com.gallbladderz.openkick.features.profile

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.datastore.AppTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsRoute(
    onLanguageSettingsClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onContentSettingsClick: () -> Unit,
    onThemeSettingsClick: () -> Unit,
    onAboutAppClick: () -> Unit,
    mainViewModel: MainViewModel = koinViewModel()
) {
    val selectedLanguages by mainViewModel.selectedLanguages.collectAsStateWithLifecycle()
    val appTheme by mainViewModel.appTheme.collectAsStateWithLifecycle()
    val useDynamicColors by mainViewModel.useDynamicColors.collectAsStateWithLifecycle()

    SettingsScreen(
        selectedLanguages = selectedLanguages,
        appTheme = appTheme,
        useDynamicColors = useDynamicColors,
        onLanguageSettingsClick = onLanguageSettingsClick,
        onNotificationSettingsClick = onNotificationSettingsClick,
        onContentSettingsClick = onContentSettingsClick,
        onThemeSettingsClick = onThemeSettingsClick,
        onAboutAppClick = onAboutAppClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedLanguages: Set<String>,
    appTheme: AppTheme,
    useDynamicColors: Boolean,
    onLanguageSettingsClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onContentSettingsClick: () -> Unit,
    onThemeSettingsClick: () -> Unit,
    onAboutAppClick: () -> Unit
) {

    val availableLanguages = mapOf(
        "ru" to stringResource(R.string.russian_lang),
        "en" to "English",
        "es" to "Español",
        "pt" to "Português",
        "de" to "Deutsch",
        "tr" to "Türkçe",
        "fr" to "Français",
        "or" to "Odia"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            SettingsGroupHeader("Контент")
        }

        item {
            SettingsListItem(
                headline = "Настройки контента",
                supporting = "Фильтр категорий, черный список стримеров",
                icon = Icons.Default.Visibility,
                onClick = onContentSettingsClick
            )
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            SettingsGroupHeader(stringResource(R.string.appearance))
        }

        item {
            val isDynamicColorsActive = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && useDynamicColors

            val supportingText = if (isDynamicColorsActive) {
                val baseState = when (appTheme) {
                    AppTheme.LIGHT, AppTheme.CATPPUCCIN_LATTE -> stringResource(R.string.light)
                    AppTheme.DARK, AppTheme.CATPPUCCIN_FRAPPE, AppTheme.CATPPUCCIN_MACCHIATO, AppTheme.CATPPUCCIN_MOCHA -> stringResource(R.string.dark)
                    else -> stringResource(R.string.system_default)
                }
                "Material You ($baseState)"
            } else {
                when (appTheme) {
                    AppTheme.SYSTEM -> stringResource(R.string.system_default)
                    AppTheme.LIGHT -> stringResource(R.string.light)
                    AppTheme.DARK -> stringResource(R.string.dark)
                    AppTheme.CATPPUCCIN_LATTE -> "Catppuccin Latte"
                    AppTheme.CATPPUCCIN_FRAPPE -> "Catppuccin Frappe"
                    AppTheme.CATPPUCCIN_MACCHIATO -> "Catppuccin Macchiato"
                    AppTheme.CATPPUCCIN_MOCHA -> "Catppuccin Mocha"
                }
            }

            SettingsListItem(
                headline = stringResource(R.string.theme_settings),
                supporting = supportingText,
                icon = Icons.Default.Edit,
                onClick = onThemeSettingsClick
            )
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            SettingsGroupHeader(stringResource(R.string.application_title))
        }

        item {
            SettingsListItem(
                headline = stringResource(R.string.notifications),
                supporting = "Настройки оповещений и фона",
                icon = Icons.Default.Notifications,
                onClick = onNotificationSettingsClick
            )

            SettingsListItem(
                headline = stringResource(R.string.language_and_region),
                supporting = if (selectedLanguages.isEmpty()) {
                    stringResource(R.string.all_languages)
                } else {
                    selectedLanguages
                        .mapNotNull { availableLanguages[it] ?: it }
                        .joinToString(", ")
                },
                icon = Icons.Default.LocationOn,
                onClick = onLanguageSettingsClick
            )
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            SettingsGroupHeader(stringResource(R.string.about_project))
        }

        item {
            SettingsListItem(
                headline = stringResource(R.string.about_app),
                supporting = null,
                icon = Icons.Default.Info,
                onClick = onAboutAppClick
            )
        }
    }
}

@Composable
fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(
            start = 16.dp,
            top = 16.dp,
            bottom = 8.dp
        )
    )
}

@Composable
fun SettingsListItem(
    headline: String,
    supporting: String?,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(headline) },
        supportingContent = if (supporting != null) { { Text(supporting) } } else null,
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = headline,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.clickable { onClick() }
    )
}
