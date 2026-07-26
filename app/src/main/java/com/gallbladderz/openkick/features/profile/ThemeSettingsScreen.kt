package com.gallbladderz.openkick.features.profile

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.datastore.AppTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun ThemeSettingsRoute(
    onBackClick: () -> Unit,
    mainViewModel: MainViewModel = koinViewModel()
) {
    val appTheme by mainViewModel.appTheme.collectAsStateWithLifecycle()
    val useDynamicColors by mainViewModel.useDynamicColors.collectAsStateWithLifecycle()

    ThemeSettingsScreen(
        appTheme = appTheme,
        useDynamicColors = useDynamicColors,
        onUpdateTheme = { mainViewModel.updateAppTheme(it) },
        onUpdateDynamicColors = { mainViewModel.updateUseDynamicColors(it) },
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    appTheme: AppTheme,
    useDynamicColors: Boolean,
    onUpdateTheme: (AppTheme) -> Unit,
    onUpdateDynamicColors: (Boolean) -> Unit,
    onBackClick: () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    text = "Base Theme",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            val themes = listOf(
                AppTheme.SYSTEM to "System Default",
                AppTheme.LIGHT to "Light",
                AppTheme.DARK to "Dark",
                AppTheme.CATPPUCCIN_MOCHA to "Catppuccin Mocha",
                AppTheme.CATPPUCCIN_LATTE to "Catppuccin Latte"
            )

            themes.forEach { (theme, label) ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                                .clickable { onUpdateTheme(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = appTheme == theme,
                                onClick = { onUpdateTheme(theme) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = "Dynamic Colors",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                                .clickable { onUpdateDynamicColors(!useDynamicColors) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Use Material You",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Extracts colors from your wallpaper",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useDynamicColors,
                            onCheckedChange = { onUpdateDynamicColors(it) }
                        )
                    }
                }
            }
        }
    }
}
