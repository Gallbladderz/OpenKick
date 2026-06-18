package com.gallbladderz.openkick.features.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            ListItem(
                headlineContent = {
                    Text(
                        text = "Войдите или зарегистрируйтесь",
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
            SettingsGroupHeader("Внешний вид")
        }
        item {
            SettingsListItem(
                headline = "Оформление",
                supporting = "Тема, цвета, настройка интерфейса",
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
                headline = "Уведомления",
                supporting = "UnifiedPush, подписки",
                icon = Icons.Default.Notifications,
                onClick = { /* TODO */ }
            )
            SettingsListItem(
                headline = "Язык и регион",
                supporting = "Язык приложения, фильтр стримов",
                icon = Icons.Default.LocationOn,
                onClick = { /* TODO */ }
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
                headline = "О приложении",
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
        modifier = Modifier.clickable { onClick() }
    )
}