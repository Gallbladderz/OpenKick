/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
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
    val hideSlots by viewModel.hideSlots.collectAsStateWithLifecycle()
    val hidePools by viewModel.hidePools.collectAsStateWithLifecycle()
    val hideCrypto by viewModel.hideCrypto.collectAsStateWithLifecycle()

    ContentSettingsScreen(
        hideSlots = hideSlots,
        hidePools = hidePools,
        hideCrypto = hideCrypto,
        onToggleSlots = { viewModel.toggleSlots(it) },
        onTogglePools = { viewModel.togglePools(it) },
        onToggleCrypto = { viewModel.toggleCrypto(it) },
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettingsScreen(
    hideSlots: Boolean,
    hidePools: Boolean,
    hideCrypto: Boolean,
    onToggleSlots: (Boolean) -> Unit,
    onTogglePools: (Boolean) -> Unit,
    onToggleCrypto: (Boolean) -> Unit,
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
                            stringResource(R.string.hide_slots_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    supportingContent = { Text(stringResource(R.string.hide_slots_desc)) },
                    trailingContent = {
                        Switch(
                            checked = hideSlots,
                            onCheckedChange = { onToggleSlots(it) }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            item {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.hide_pools_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    supportingContent = { Text(stringResource(R.string.hide_pools_desc)) },
                    trailingContent = {
                        Switch(
                            checked = hidePools,
                            onCheckedChange = { onTogglePools(it) }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            item {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.hide_crypto_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    supportingContent = { Text(stringResource(R.string.hide_crypto_desc)) },
                    trailingContent = {
                        Switch(
                            checked = hideCrypto,
                            onCheckedChange = { onToggleCrypto(it) }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}