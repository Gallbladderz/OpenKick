/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.downloads

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.data.local.DownloadStatus
import com.gallbladderz.openkick.core.player.VideoQuality
import com.gallbladderz.openkick.core.ui.utils.formatTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
internal fun ClipDownloadAction(source: DownloadSource) {
    val repository = koinInject<DownloadsRepository>()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val items by repository.downloads.collectAsStateWithLifecycle(initialValue = emptyList())
    val item = items.firstOrNull { it.mediaType == source.mediaType && it.sourceId == source.id }
    val active = item?.status == DownloadStatus.DOWNLOADING || item?.status == DownloadStatus.QUEUED
    LaunchedEffect(active) {
        if (active) {
            while (true) {
                repository.reconcile()
                kotlinx.coroutines.delay(2_000)
            }
        }
    }
    IconButton(modifier = Modifier.size(40.dp), onClick = {
        scope.launch {
            runDownloadAction(context) {
                when (item?.status) {
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                        repository.retry(item.id)
                        context.feedback(R.string.download_queued_message)
                    }
                    DownloadStatus.COMPLETED -> context.feedback(R.string.download_already_completed)
                    DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> context.feedback(R.string.download_already_active)
                    null -> context.feedback(repository.enqueue(source).enqueueMessage())
                }
            }
        }
    }) {
        DownloadActionIcon(item?.status)
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VodDownloadAction(
    source: DownloadSource,
    playbackUrl: String,
    durationMs: Long,
    positionMs: Long,
    currentQuality: VideoQuality?
) {
    val engine = koinInject<DownloadEngine>()
    val repository = koinInject<DownloadsRepository>()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entries by repository.downloads.collectAsStateWithLifecycle(initialValue = emptyList())
    val matching = entries.filter { it.mediaType == source.mediaType && it.sourceId == source.id }
    val status = matching.firstOrNull {
        it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
    }?.status ?: matching.firstOrNull { it.status == DownloadStatus.COMPLETED }?.status
    var visible by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { visible = true }, modifier = Modifier.size(40.dp)) {
        DownloadActionIcon(status)
    }
    if (!visible) return

    var qualities by remember(playbackUrl) { mutableStateOf<List<DownloadQuality>?>(null) }
    var loadingFailed by remember(playbackUrl) { mutableStateOf(false) }
    var qualityKey by rememberSaveable { mutableStateOf<String?>(null) }
    var custom by rememberSaveable { mutableStateOf(false) }
    var from by rememberSaveable { mutableStateOf("00:00") }
    var to by rememberSaveable { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val durationSec = (durationMs / 1000).coerceAtLeast(0)
    LaunchedEffect(playbackUrl) {
        try {
            val options = engine.qualities(playbackUrl)
            val format = currentQuality?.trackIndex?.let { currentQuality.trackGroup?.getFormat(it) }
            if (options.none { it.key == qualityKey }) {
                qualityKey = options.firstOrNull { it.height == format?.height && it.fps == format.frameRate.toInt() }
                    ?.key ?: options.firstOrNull { it.height == format?.height }?.key ?: options.firstOrNull()?.key
            }
            qualities = options
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadingFailed = true
        }
    }
    LaunchedEffect(durationSec) {
        if (to.isBlank() && durationSec > 0) to = formatTime(durationSec * 1000)
    }
    val start = parseDownloadTime(from)
    val end = parseDownloadTime(to)
    val valid = start != null && end != null && start < end && end <= durationSec
    val supportsRange = android.net.Uri.parse(playbackUrl).path.orEmpty().endsWith(".m3u8", true)

    ModalBottomSheet(
        onDismissRequest = { visible = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier.navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.download_setup), style = MaterialTheme.typography.titleLarge)
                when {
                    loadingFailed -> Text(stringResource(R.string.download_quality_unavailable))
                    qualities == null -> CircularProgressIndicator()
                    qualities.orEmpty().isEmpty() -> Text(stringResource(R.string.download_quality_original))
                    else -> Column(Modifier.selectableGroup()) {
                        qualities.orEmpty().forEach { quality ->
                            val label = quality.label.ifBlank { stringResource(R.string.download_quality_original) }
                            Row(
                                Modifier.fillMaxWidth().selectable(
                                    selected = qualityKey == quality.key,
                                    role = Role.RadioButton,
                                    onClick = { qualityKey = quality.key }
                                ).heightIn(min = 48.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(selected = qualityKey == quality.key, onClick = null)
                                Text(label)
                            }
                        }
                    }
                }
                if (durationSec > 0) Text(stringResource(R.string.download_duration, formatTime(durationSec * 1000)))
                if (supportsRange) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DownloadFilterChip(!custom, { custom = false }, stringResource(R.string.download_entire))
                        DownloadFilterChip(custom, { custom = true }, stringResource(R.string.download_custom),
                            enabled = durationSec > 0)
                    }
                }
                if (custom) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(from, { from = it }, Modifier.weight(1f), singleLine = true,
                            label = { Text(stringResource(R.string.download_from)) }, isError = start == null)
                        OutlinedTextField(to, { to = it }, Modifier.weight(1f), singleLine = true,
                            label = { Text(stringResource(R.string.download_to)) }, isError = end == null)
                    }
                    TextButton(onClick = { from = formatTime(positionMs.coerceAtLeast(0)) }) {
                        Text(stringResource(R.string.download_current_position))
                    }
                    Text(
                        stringResource(when {
                            start == null || end == null -> R.string.download_time_error
                            !valid -> R.string.download_range_error
                            else -> R.string.download_hls_range_hint
                        }),
                        color = if (valid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                    )
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = qualities != null && !submitting && (!custom || valid),
                onClick = {
                    submitting = true
                    scope.launch {
                        runDownloadAction(context) {
                            val quality = qualities.orEmpty().firstOrNull { it.key == qualityKey }
                            val status = repository.enqueue(source, quality, start.takeIf { custom }, end.takeIf { custom })
                            context.feedback(status.enqueueMessage())
                            visible = false
                        }
                        submitting = false
                    }
                }
            ) { Text(stringResource(R.string.download_action)) }
        }
    }
}

internal suspend fun runDownloadAction(context: Context, action: suspend () -> Unit) {
    try {
        action()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        context.feedback(R.string.download_file_error)
    }
}

private fun DownloadStatus.enqueueMessage() = when (this) {
    DownloadStatus.COMPLETED -> R.string.download_already_completed
    DownloadStatus.DOWNLOADING -> R.string.download_already_active
    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> R.string.download_failed
    DownloadStatus.QUEUED -> R.string.download_queued_message
}

private fun Context.feedback(message: Int) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

@Composable
private fun DownloadActionIcon(status: DownloadStatus?) {
    val color = MaterialTheme.colorScheme.onSurface
    when (status) {
        DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> {
            val label = stringResource(R.string.download_in_progress)
            CircularProgressIndicator(
                Modifier.size(20.dp).semantics { contentDescription = label },
                color = color,
                strokeWidth = 2.dp
            )
        }
        DownloadStatus.COMPLETED -> Icon(Icons.Rounded.DownloadDone, stringResource(R.string.download_completed),
            tint = color, modifier = Modifier.size(22.dp))
        else -> Icon(Icons.Rounded.Download, stringResource(R.string.download_action),
            tint = color, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun DownloadFilterChip(selected: Boolean, onClick: () -> Unit, label: String, enabled: Boolean = true) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        enabled = enabled,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
