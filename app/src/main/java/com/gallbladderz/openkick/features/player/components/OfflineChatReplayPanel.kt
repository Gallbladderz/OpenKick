/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.data.local.DownloadEntity
import com.gallbladderz.openkick.core.data.local.DownloadMediaType
import com.gallbladderz.openkick.core.data.repository.ClipRepository
import com.gallbladderz.openkick.core.datastore.ChatSettings
import com.gallbladderz.openkick.core.datastore.SettingsRepository
import com.gallbladderz.openkick.core.domain.chat.ChatReplaySource
import com.gallbladderz.openkick.features.downloads.chatReplaySource
import com.gallbladderz.openkick.features.downloads.listOfflineChatChunkFiles
import com.gallbladderz.openkick.features.downloads.loadOfflineChatChunksForPosition
import com.gallbladderz.openkick.features.downloads.loadOfflineChatReplay
import com.gallbladderz.openkick.features.downloads.offlineChatChunkIndex
import com.gallbladderz.openkick.features.downloads.offlineChatChunksNeeded
import com.gallbladderz.openkick.features.downloads.offlineChatFileForVideo
import com.gallbladderz.openkick.features.downloads.visibleOfflineChatMessages
import com.gallbladderz.openkick.features.player.ReplayChatMessage
import com.gallbladderz.openkick.features.profile.StreamerProfileRepository
import java.io.File
import org.koin.compose.koinInject

private sealed interface OfflineChatState {
    data object Loading : OfflineChatState
    data class Saved(val messages: List<ReplayChatMessage>) : OfflineChatState
    /** Per-hour chunk files (`chat-XXXX.json`) in the download directory. */
    data class Chunked(val directory: File) : OfflineChatState
    data class Online(val source: ChatReplaySource, val channelSlug: String) : OfflineChatState
    /** No chat.json and the chat source could not be resolved (offline / fetch failed). */
    data object NotDownloaded : OfflineChatState
    /** The recording exists but Kick has no chat replay for it. */
    data object Unavailable : OfflineChatState
}

/**
 * Chat replay for a downloaded VOD/clip, shown under the offline player.
 *
 * Priority:
 * 1. Per-hour chunk files (`chat-XXXX.json`) saved next to the video at
 *    download time — works fully offline, only the hours around the current
 *    playback position are loaded into memory.
 * 2. Legacy single `chat.json` (downloads made by the first version of this
 *    feature) — loaded as before.
 * 3. Online fallback: the chat source stored on the download, or resolved once
 *    via fetchVideoPlayback/fetchClip (needs internet once). This is what makes
 *    downloads made before this feature — e.g. the user's BIZIGA_RR VOD — show
 *    chat again without re-downloading.
 * 4. A clear notice when neither is available.
 */
@Composable
internal fun OfflineChatReplayPanel(
    entry: DownloadEntity,
    playback: RecordedPlayback,
    modifier: Modifier = Modifier
) {
    val videos = koinInject<StreamerProfileRepository>()
    val clips = koinInject<ClipRepository>()
    var state by remember(entry.id, entry.filePath) {
        mutableStateOf<OfflineChatState>(OfflineChatState.Loading)
    }
    LaunchedEffect(entry.id, entry.filePath) {
        state = OfflineChatState.Loading
        val videoFile = entry.filePath?.let(::File)
        val directory = videoFile?.parentFile
        val file = videoFile?.let(::offlineChatFileForVideo)
        val saved = file?.let { loadOfflineChatReplay(it) }
        if (saved != null) {
            state = OfflineChatState.Saved(saved)
            return@LaunchedEffect
        }
        if (directory != null && listOfflineChatChunkFiles(directory).isNotEmpty()) {
            state = OfflineChatState.Chunked(directory)
            return@LaunchedEffect
        }
        val stored = entry.chatReplaySource()
        if (stored != null) {
            // channelName is the streamer username; used only for the 7TV lookup,
            // which fails gracefully if it is not a valid slug.
            state = OfflineChatState.Online(stored, entry.channelName)
            return@LaunchedEffect
        }
        state = try {
            when (entry.mediaType) {
                DownloadMediaType.VOD -> {
                    val video = videos.fetchVideoPlayback(entry.sourceId, entry.title).getOrNull()
                    if (video == null) OfflineChatState.NotDownloaded
                    else video.chatReplay?.let { OfflineChatState.Online(it, video.channelSlug) }
                        ?: OfflineChatState.Unavailable
                }
                DownloadMediaType.CLIP -> {
                    val clip = clips.fetchClip(entry.sourceId).getOrNull()
                    if (clip == null) OfflineChatState.NotDownloaded
                    else clip.chatReplay?.let { OfflineChatState.Online(it, clip.streamerDisplayName) }
                        ?: OfflineChatState.Unavailable
                }
            }
        } catch (_: Exception) {
            OfflineChatState.NotDownloaded
        }
    }

    Column(modifier.fillMaxWidth()) {
        // In the fullscreen floating panel the overlay header already
        // says "Chat replay" — a second title here would waste vertical
        // space over the video.
        if (!LocalChatReadableOverVideo.current) {
            Text(
                stringResource(
                    if (state is OfflineChatState.Saved || state is OfflineChatState.Chunked) R.string.chat_replay_saved
                    else R.string.chat_replay
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        when (val current = state) {
            OfflineChatState.Loading -> Box(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            }
            is OfflineChatState.Saved -> OfflineChatMessages(current.messages, playback)
            is OfflineChatState.Chunked -> OfflineChunkedChatMessages(current.directory, playback)
            is OfflineChatState.Online -> ChatReplayPanel(
                current.source, playback, Modifier, channelSlug = current.channelSlug
            )
            OfflineChatState.NotDownloaded ->
                OfflineChatNotice(R.string.chat_replay_offline_not_downloaded)
            OfflineChatState.Unavailable ->
                OfflineChatNotice(R.string.chat_replay_unavailable)
        }
    }
}

/**
 * Chunked offline chat: only the hour containing [playback] position plus
 * its neighbours are kept in memory. Seeking to another hour (e.g. 5h or
 * 9h59m) loads that hour's chunk and evicts distant ones.
 */
@Composable
private fun OfflineChunkedChatMessages(directory: File, playback: RecordedPlayback) {
    val chunks = remember(directory) {
        androidx.compose.runtime.mutableStateMapOf<Int, List<ReplayChatMessage>>()
    }
    val currentChunk = offlineChatChunkIndex(playback.positionMs)
    LaunchedEffect(directory, currentChunk) {
        val needed = offlineChatChunksNeeded(playback.positionMs)
        val loaded = loadOfflineChatChunksForPosition(directory, playback.positionMs)
        chunks.keys.filter { it !in needed }.forEach(chunks::remove)
        chunks.putAll(loaded)
    }
    val messages = chunks.values.flatten().sortedBy { it.offsetMs }
    if (chunks.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        }
    } else {
        OfflineChatMessages(messages, playback)
    }
}

@Composable
private fun OfflineChatMessages(messages: List<ReplayChatMessage>, playback: RecordedPlayback) {
    val settingsRepository = koinInject<SettingsRepository>()
    val settings by settingsRepository.chatSettingsFlow
        .collectAsStateWithLifecycle(ChatSettings(sevenTvEmotes = false))
    // 7TV channel emotes need the Kick user id (an online lookup); offline only
    // the global set is available, so pass null here.
    val visible = remember(messages, playback.positionMs) {
        visibleOfflineChatMessages(messages, playback.positionMs).asReversed()
    }
    val listState = rememberLazyListState()
    var follow by remember(messages) { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to listState.firstVisibleItemIndex }
            .collect { (scrolling, index) ->
                if (scrolling) follow = index < 2
            }
    }
    LaunchedEffect(visible.firstOrNull()?.message?.id, follow) {
        if (follow && visible.isNotEmpty()) listState.scrollToItem(0)
    }

    SevenTvChat(settings.sevenTvEmotes, null) {
        if (visible.isNotEmpty()) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                verticalArrangement = Arrangement.Bottom,
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(visible, key = { it.message.id }, contentType = { "replay_message" }) { entry ->
                    ChatMessageItem(entry.message, settings, settings.animatedEmotes, canReply = false)
                }
            }
        } else {
            OfflineChatNotice(R.string.chat_replay_empty)
        }
    }
}

@Composable
private fun OfflineChatNotice(message: Int) {
    val overVideo = LocalChatReadableOverVideo.current
    Text(
        stringResource(message),
        color = if (overVideo) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall.copy(
            shadow = if (overVideo) ChatOverVideoShadow else null
        ),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
