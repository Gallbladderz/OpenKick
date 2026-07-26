package com.gallbladderz.openkick.features.profile

import androidx.compose.foundation.background
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
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.gallbladderz.openkick.features.notifications.StreamKeepaliveService
import android.content.Context
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: MainViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val backgroundKeepalive by viewModel.backgroundKeepalive.collectAsStateWithLifecycle()

    val powerManager = remember {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    var isIgnoringBattery by remember {
        mutableStateOf(
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringBattery =
                    powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications)) },
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
                            "Включить уведомления",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    supportingContent = {
                        Text(stringResource(R.string.receive_stream_alerts))
                    },
                    trailingContent = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = {
                                viewModel.toggleNotifications(it)
                            }
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
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
                            "Фоновое соединение",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    supportingContent = {
                        Text(
                            "Держать процесс в фоне для моментальных уведомлений. Может жрать батарею как не в себя."
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = backgroundKeepalive,
                            enabled = notificationsEnabled,
                            onCheckedChange = { isEnabled ->
                                viewModel.toggleBackgroundKeepalive(isEnabled)

                                val intent = Intent(
                                    context,
                                    StreamKeepaliveService::class.java
                                )

                                if (isEnabled) {
                                    ContextCompat.startForegroundService(
                                        context,
                                        intent
                                    )
                                } else {
                                    context.stopService(intent)
                                }
                            }
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
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
                    headlineContent = { Text(stringResource(R.string.battery_saving), fontWeight = FontWeight.Bold) },
                    supportingContent = {
                        Text(
                            if (isIgnoringBattery) stringResource(R.string.battery_opt_disabled)
                            else stringResource(R.string.battery_opt_enable)
                        )
                    },
                    trailingContent = {
                        if (isIgnoringBattery) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.done_desc),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            ) {
                                Text(stringResource(R.string.disable))
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}