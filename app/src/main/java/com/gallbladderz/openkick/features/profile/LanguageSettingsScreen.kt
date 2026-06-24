package com.gallbladderz.openkick.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallbladderz.openkick.R
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: MainViewModel = koinViewModel()
) {
    val selectedLanguages by viewModel.selectedLanguages.collectAsStateWithLifecycle()

    
    var appLanguage by remember { mutableStateOf("ru") }
    var showAppLangMenu by remember { mutableStateOf(false) }

    val availableStreamLanguages = mapOf(
        "ru" to "Русский",
        "en" to "English",
        "es" to "Español",
        "pt" to "Português",
        "de" to "Deutsch",
        "tr" to "Türkçe",
        "fr" to "Français",
        "or" to "Odia"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.language_and_region)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp)
        ) {
            
            item {
                Text(
                    text = "Язык интерфейса",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Язык приложения") },
                        supportingContent = { Text(if (appLanguage == "ru") "Русский" else "English") },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAppLangMenu = true }
                    )

                    DropdownMenu(
                        expanded = showAppLangMenu,
                        onDismissRequest = { showAppLangMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Русский") },
                            onClick = {
                                appLanguage = "ru"
                                showAppLangMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("English (English)") },
                            onClick = {
                                appLanguage = "en"
                                showAppLangMenu = false
                            }
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            }

            
            item {
                Text(
                    text = "Языки трансляций",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Показывать стримы на выбранных языках:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(availableStreamLanguages.toList()) { (code, name) ->
                val isChecked = selectedLanguages.contains(code)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = isChecked,
                            onValueChange = { checked ->
                                val newSelection = selectedLanguages.toMutableSet()
                                if (checked) newSelection.add(code) else newSelection.remove(code)
                                viewModel.updateSelectedLanguages(newSelection)
                            },
                            role = Role.Checkbox
                        )
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}