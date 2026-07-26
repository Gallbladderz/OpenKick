package com.gallbladderz.openkick.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallbladderz.openkick.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun ContentSettingsRoute(
    onBackClick: () -> Unit,
    viewModel: MainViewModel = koinViewModel()
) {
    val hideCategories by viewModel.hideCategories.collectAsStateWithLifecycle()

    ContentSettingsScreen(
        hideCategories = hideCategories,
        onToggleCategories = { viewModel.toggleCategories(it) },
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettingsScreen(
    hideCategories: Boolean,
    onToggleCategories: (Boolean) -> Unit,
    onBackClick: () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.content_settings_title),
                        fontWeight = FontWeight.Bold
                    )
                },
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
                    headlineContent = {
                        Text(
                            stringResource(R.string.hide_nsfw_categories),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    supportingContent = { Text(stringResource(R.string.hide_nsfw_description)) },
                    trailingContent = {
                        Switch(
                            checked = hideCategories,
                            onCheckedChange = { onToggleCategories(it) }
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
                    headlineContent = {
                        Text(
                            stringResource(R.string.blacklist_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    supportingContent = { Text(stringResource(R.string.blacklist_description)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {

                    }
                )
            }
        }
    }
}