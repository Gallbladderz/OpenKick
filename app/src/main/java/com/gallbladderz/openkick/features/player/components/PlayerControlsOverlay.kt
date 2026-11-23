/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.features.player.components


import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.Player
import com.gallbladderz.openkick.features.player.models.LivePlaybackState

@Composable
fun PlayerControlsOverlay(
    playWhenReady: Boolean,
    playbackState: Int,
    isFullscreen: Boolean,
    livePlaybackState: LivePlaybackState,
    player: Player?,
    liveTimelineInteractions: MutableInteractionSource,
    onSeekLiveTimeline: (Long) -> Unit,
    onSeekToLiveEdge: () -> Unit,
    onPlayerManagerResume: () -> Unit,
    onPlayerManagerPause: () -> Unit,
    onFullscreenToggle: () -> Unit,
    onShowSettings: () -> Unit,
    onBackClick: () -> Unit,
    onToggleChat: () -> Unit = {}
) {
    CustomPlayerControls(
        playWhenReady = playWhenReady,
        playbackState = playbackState,
        isFullscreen = isFullscreen,
        livePlaybackState = livePlaybackState,
        livePlayer = player,
        liveTimelineInteractions = liveTimelineInteractions,
        onSeekLiveTimeline = onSeekLiveTimeline,
        onSeekToLive = onSeekToLiveEdge,
        onPlayPause = {
            if (playWhenReady) onPlayerManagerPause() else onPlayerManagerResume()
        },
        onFullscreen = onFullscreenToggle,
        onSettings = onShowSettings,
        onBack = { if (isFullscreen) onFullscreenToggle() else onBackClick() },
        onToggleChat = onToggleChat,
        modifier = Modifier.fillMaxSize()
    )
}
