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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.gallbladderz.openkick.core.datastore.ChatSettings
import com.gallbladderz.openkick.core.datastore.SettingsRepository
import com.gallbladderz.openkick.core.domain.chat.ChatReplaySource
import com.gallbladderz.openkick.core.network.api.KickApiService
import com.gallbladderz.openkick.features.player.CHAT_REPLAY_WINDOW_MS
import com.gallbladderz.openkick.features.player.ReplayChatMessage
import com.gallbladderz.openkick.features.player.fetchChatReplayWindow
import kotlinx.coroutines.CancellationException
import org.koin.compose.koinInject

@Composable
internal fun ChatReplayPanel(
    source: ChatReplaySource?,
    playback: RecordedPlayback,
    modifier: Modifier = Modifier,
    channelSlug: String = ""
) {
    Column(modifier.fillMaxWidth()) {
        // In the fullscreen floating panel the overlay header already
        // says "Chat replay" — a second title here would waste vertical
        // space over the video.
        if (!LocalChatReadableOverVideo.current) {
            Text(
                stringResource(R.string.chat_replay),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        if (source == null) {
            ChatReplayNotice(R.string.chat_replay_unavailable)
        } else {
            ChatReplayMessages(source, playback, channelSlug)
        }
    }
}

@Composable
private fun ChatReplayMessages(source: ChatReplaySource, playback: RecordedPlayback, channelSlug: String) {
    val api = koinInject<KickApiService>()
    val settingsRepository = koinInject<SettingsRepository>()
    val settings by settingsRepository.chatSettingsFlow.collectAsStateWithLifecycle(ChatSettings(sevenTvEmotes = false))
    var kickUserId by remember(source.channelId, channelSlug) { mutableStateOf<Long?>(null) }
    LaunchedEffect(settings.sevenTvEmotes, source.channelId, channelSlug) {
        if (settings.sevenTvEmotes && kickUserId == null && channelSlug.isNotBlank()) {
            kickUserId = try {
                api.getChannelStreamInfo(channelSlug).takeIf { it.id == source.channelId }?.kickUserId
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
        }
    }
    val duration = playback.durationMs
    val position = playback.positionMs.coerceAtLeast(0)
    val window = if (duration > 0) minOf(position, duration - 1) / CHAT_REPLAY_WINDOW_MS else 0
    val windows = remember(source) { mutableStateMapOf<Long, List<ReplayChatMessage>>() }
    var failedWindow by remember(source) { mutableStateOf<Long?>(null) }
    var retry by remember(source) { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    var follow by remember(source) { mutableStateOf(true) }

    LaunchedEffect(source, window, duration, retry) {
        if (duration <= 0) return@LaunchedEffect
        failedWindow = null
        windows.keys.filter { it !in (window - 2)..(window + 2) }.forEach(windows::remove)
        for (index in listOf(window, window + 1, window - 1)) {
            val start = index * CHAT_REPLAY_WINDOW_MS
            if (start !in 0 until duration || index in windows) continue
            try {
                windows[index] = fetchChatReplayWindow(
                    source,
                    start,
                    minOf(start + CHAT_REPLAY_WINDOW_MS, duration)
                ) { timestamp -> api.getChatReplay(source.channelId, timestamp.toString()).data }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (index == window) {
                    failedWindow = index
                    break
                }
            }
        }
    }

    val messages = (window - 2..window).flatMap { windows[it].orEmpty() }
        .filter { it.offsetMs <= position }
        .distinctBy { it.message.id }
        .takeLast(100)
        .asReversed()
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to listState.firstVisibleItemIndex }
            .collect { (scrolling, index) ->
                if (scrolling) follow = index < 2
            }
    }
    LaunchedEffect(messages.firstOrNull()?.message?.id, follow) {
        if (follow && messages.isNotEmpty()) listState.scrollToItem(0)
    }

    SevenTvChat(settings.sevenTvEmotes, kickUserId) {
        if (failedWindow == window) {
            TextButton(onClick = { retry++ }, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(stringResource(R.string.chat_replay_retry))
            }
        }
        if (messages.isNotEmpty()) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                verticalArrangement = Arrangement.Bottom,
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(messages, key = { it.message.id }, contentType = { "replay_message" }) { entry ->
                    ChatMessageItem(entry.message, settings, settings.animatedEmotes, canReply = false)
                }
            }
        } else if (window in windows) {
            ChatReplayNotice(R.string.chat_replay_empty)
        } else if (failedWindow != window) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun ChatReplayNotice(message: Int) {
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
