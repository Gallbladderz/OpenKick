/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.downloads

import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.data.local.DownloadEntity
import com.gallbladderz.openkick.core.data.local.DownloadMediaType
import com.gallbladderz.openkick.core.data.local.DownloadStatus
import com.gallbladderz.openkick.core.player.VodPlaybackProgress
import com.gallbladderz.openkick.core.player.PlayerExpandedState
import com.gallbladderz.openkick.core.player.vodProgressKey
import com.gallbladderz.openkick.core.ui.utils.formatTime
import com.gallbladderz.openkick.features.player.components.OfflineChatReplayPanel
import com.gallbladderz.openkick.features.player.components.RecordedInfoPanel
import com.gallbladderz.openkick.features.player.components.RecordedPlayerPlaceholder
import com.gallbladderz.openkick.features.player.components.RecordedPlayerScreen
import com.gallbladderz.openkick.features.player.components.RecordedShareButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadsScreen(onBack: () -> Unit, onPlay: (String) -> Unit) {
    val repository = koinInject<DownloadsRepository>()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloads by repository.downloads.collectAsStateWithLifecycle(initialValue = emptyList())
    var filter by rememberSaveable { mutableStateOf<DownloadMediaType?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    var sharing by remember { mutableStateOf<String?>(null) }
    val visible = downloads.filter { filter == null || it.mediaType == filter }
    val hasActive = downloads.any { it.status.isActive() }
    LaunchedEffect(hasActive) {
        do {
            repository.reconcile()
            if (hasActive) delay(2_000)
        } while (hasActive)
    }
    val saved = downloads.filter { it.status == DownloadStatus.COMPLETED }
    Scaffold(topBar = {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.downloads), style = MaterialTheme.typography.titleLarge)
                    if (downloads.isNotEmpty()) {
                        val count = pluralStringResource(R.plurals.download_file_count, saved.size, saved.size)
                        Text(
                            "$count · ${Formatter.formatShortFileSize(context, saved.sumOf { it.fileSizeBytes })}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button))
                }
            }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DownloadFilterChip(filter == null, { filter = null }, stringResource(R.string.download_all))
                DownloadFilterChip(filter == DownloadMediaType.CLIP, { filter = DownloadMediaType.CLIP },
                    stringResource(R.string.download_clips))
                DownloadFilterChip(filter == DownloadMediaType.VOD, { filter = DownloadMediaType.VOD },
                    stringResource(R.string.download_vods))
            }
            if (visible.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(32.dp))
                        Text(stringResource(R.string.download_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn {
                    items(visible, key = { it.id }, contentType = { "download" }) { entry ->
                        DownloadRow(
                            entry = entry,
                            onPlay = { onPlay(entry.id) },
                            onCancel = { scope.launch { runDownloadAction(context) { repository.cancel(entry.id) } } },
                            onRetry = { scope.launch { runDownloadAction(context) { repository.retry(entry.id) } } },
                            onDelete = { deleting = entry.id },
                            onShare = {
                                if (sharing == null) {
                                    sharing = entry.id
                                    scope.launch {
                                        try {
                                            runDownloadAction(context) {
                                                context.startActivity(Intent.createChooser(repository.share(entry), null))
                                            }
                                        } finally {
                                            sharing = null
                                        }
                                    }
                                }
                            },
                            sharing = sharing == entry.id
                        )
                        HorizontalDivider(
                            Modifier.padding(start = 16.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
    deleting?.let { id ->
        DownloadDeleteDialog(onDismiss = { deleting = null }) {
            deleting = null
            scope.launch { runDownloadAction(context) { repository.delete(id) } }
        }
    }
}

@Composable
private fun DownloadRow(
    entry: DownloadEntity, onPlay: () -> Unit, onCancel: () -> Unit, onRetry: () -> Unit,
    onDelete: () -> Unit, onShare: () -> Unit, sharing: Boolean
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val completed = entry.status == DownloadStatus.COMPLETED
    var menuOpen by remember { mutableStateOf(false) }
    val status = stringResource(when (entry.status) {
        DownloadStatus.QUEUED -> R.string.download_queued
        DownloadStatus.DOWNLOADING -> R.string.download_in_progress
        DownloadStatus.COMPLETED -> R.string.download_completed
        DownloadStatus.FAILED -> R.string.download_failed
        DownloadStatus.CANCELLED -> R.string.download_cancelled
    })
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = completed, onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DownloadPreview(entry)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (entry.channelName.isNotBlank()) {
                Text(entry.channelName, style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val size = if (completed) Formatter.formatShortFileSize(context, entry.fileSizeBytes) else null
            Text(
                listOfNotNull(entry.qualityLabel.takeIf(String::isNotBlank), size).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            when {
                entry.status.isActive() -> {
                    LinearProgressIndicator(progress = { entry.progress / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("$status · ${stringResource(R.string.download_percent, entry.progress)}",
                        style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                entry.status == DownloadStatus.FAILED -> {
                    Text(entry.errorMessage ?: status, style = MaterialTheme.typography.labelSmall,
                        color = colors.error, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                !completed -> Text(status, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp), enabled = !sharing) {
                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.download_more_actions))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                when {
                    entry.status.isActive() ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.download_cancel)) },
                            onClick = { menuOpen = false; onCancel() }
                        )
                    completed ->
                        DropdownMenuItem(
                            text = { Text(stringResource(entry.shareLabel())) },
                            onClick = { menuOpen = false; onShare() }
                        )
                    else ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.retry)) },
                            onClick = { menuOpen = false; onRetry() }
                        )
                }
                if (!entry.status.isActive()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.download_delete)) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadPreview(entry: DownloadEntity) {
    Box(
        Modifier.width(128.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(entry.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize())
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.55f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))))
        )
        Text(
            entry.mediaType.name,
            modifier = Modifier.align(Alignment.TopStart).padding(5.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
        entry.timeLabel()?.let { label ->
            Text(label, modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp),
                style = MaterialTheme.typography.labelSmall, color = Color.White,
                textAlign = TextAlign.End, maxLines = 1)
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun OfflineDownloadScreen(
    id: String,
    dragState: AnchoredDraggableState<PlayerExpandedState>,
    onBack: () -> Unit
) {
    val repository = koinInject<DownloadsRepository>()
    val items by repository.downloads.collectAsStateWithLifecycle(initialValue = null)
    val entry = items?.firstOrNull { it.id == id }
    val file = entry?.existingFile()
    if (file == null) {
        RecordedPlayerPlaceholder(
            dragState, entry?.title ?: stringResource(R.string.downloads), onBack,
            error = R.string.download_missing.takeIf { items != null }
        )
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val progress = koinInject<VodPlaybackProgress>()
    val progressKey = if (entry.mediaType == DownloadMediaType.VOD) {
        vodProgressKey(entry.sourceId, entry.startSec, entry.endSec)
    } else null
    val restoredPosition by produceState<Long?>(if (progressKey == null) 0L else null, progressKey) {
        value = if (progressKey == null) 0L else progress.position(progressKey)
    }
    val startPosition = restoredPosition
    if (startPosition == null) {
        RecordedPlayerPlaceholder(dragState, entry.title, onBack)
        return
    }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }
    val player = remember(file) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)), startPosition)
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player, progressKey) {
        val tracking = progressKey?.let { progress.track(player, it, scope) }
        onDispose {
            tracking?.close()
            player.release()
        }
    }
    RecordedPlayerScreen(
        player, dragState, entry.title, entry.channelName, onBack, offline = true,
        chat = { playback, modifier -> OfflineChatReplayPanel(entry, playback, modifier) }
    ) { playback ->
        RecordedInfoPanel(
            title = entry.title,
            channelName = entry.channelName,
            avatarUrl = entry.channelAvatarUrl.orEmpty(),
            metadata = listOfNotNull(
                entry.qualityLabel.takeIf(String::isNotBlank),
                Formatter.formatShortFileSize(context, entry.fileSizeBytes),
                entry.timeLabel() ?: playback.durationMs.takeIf { it > 0 }?.let(::formatTime)
            )
        ) {
            RecordedShareButton(
                description = entry.shareLabel(),
                enabled = !sharing,
                onShare = {
                    sharing = true
                    scope.launch {
                        try {
                            runDownloadAction(context) {
                                context.startActivity(Intent.createChooser(repository.share(entry), null))
                            }
                        } finally {
                            sharing = false
                        }
                    }
                }
            )
            IconButton(onClick = { deleting = true }, modifier = Modifier.size(40.dp), enabled = !sharing) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.download_delete),
                    tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
            }
        }
    }
    if (deleting) DownloadDeleteDialog(onDismiss = { deleting = false }) {
        deleting = false
        scope.launch {
            runDownloadAction(context) {
                player.pause()
                repository.delete(id)
                onBack()
            }
        }
    }
}

@Composable
private fun DownloadDeleteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.download_delete)) },
        text = { Text(stringResource(R.string.download_delete_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.download_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.download_close)) } }
    )
}

private fun DownloadEntity.shareLabel() =
    if (filePath?.endsWith(".m3u8") == true) R.string.download_hls_share else R.string.share_desc

private fun DownloadEntity.timeLabel(): String? =
    if (startSec != null && endSec != null) "${formatTime(startSec * 1000)} – ${formatTime(endSec * 1000)}"
    else durationSec?.takeIf { it > 0 }?.let { formatTime(it * 1000) }

private fun DownloadStatus.isActive() = this == DownloadStatus.QUEUED || this == DownloadStatus.DOWNLOADING
