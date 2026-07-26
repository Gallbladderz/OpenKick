package com.gallbladderz.openkick.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gallbladderz.openkick.R
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: MainViewModel = koinViewModel()
) {
    
    val hideCategories by viewModel.hideCategories.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.content_settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
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
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.hide_nsfw_categories), fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(stringResource(R.string.hide_nsfw_description)) },
                    trailingContent = {
                        Switch(
                            checked = hideCategories,
                            onCheckedChange = { viewModel.toggleCategories(it) }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.blacklist_title), fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(stringResource(R.string.blacklist_description)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        
                    }
                )
            }
        }
    }
}