package com.gallbladderz.openkick.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
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
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onLanguageSettingsClick: () -> Unit
) {
    val mainViewModel: MainViewModel = koinViewModel()
    val selectedLanguages by mainViewModel.selectedLanguages.collectAsStateWithLifecycle()

    
    val availableLanguages = mapOf(
        "ru" to "Русский",
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
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.login_or_register),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Аватар",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable { /* TODO: Открыть экран авторизации */ }
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
            SettingsListItem(
                headline = stringResource(R.string.theme_settings),
                supporting = stringResource(R.string.theme_settings_desc),
                icon = Icons.Default.Edit,
                onClick = { /* TODO */ }
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            SettingsGroupHeader("Приложение")
        }
        item {
            SettingsListItem(
                headline = stringResource(R.string.notifications),
                supporting = "UnifiedPush, подписки",
                icon = Icons.Default.Notifications,
                onClick = { /* TODO */ }
            )

            
            SettingsListItem(
                headline = stringResource(R.string.language_and_region),
                supporting = if (selectedLanguages.isEmpty()) "Все языки" else selectedLanguages.mapNotNull { availableLanguages[it] ?: it }.joinToString(", "),
                icon = Icons.Default.LocationOn,
                onClick = onLanguageSettingsClick
            )
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            SettingsGroupHeader("О проекте")
        }
        item {
            SettingsListItem(
                headline = stringResource(R.string.about_app),
                supporting = "Версия 0.1-alpha",
                icon = Icons.Default.Info,
                onClick = { /* TODO */ }
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
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsListItem(
    headline: String,
    supporting: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(headline) },
        supportingContent = { Text(supporting) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = headline,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onClick() }
    )
}