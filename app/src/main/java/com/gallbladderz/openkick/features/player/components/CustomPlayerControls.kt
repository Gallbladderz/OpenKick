/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.features.player.components


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.features.player.models.LivePlaybackMode
import com.gallbladderz.openkick.features.player.models.LivePlaybackState

@Composable
fun CustomPlayerControls(
    playWhenReady: Boolean,
    playbackState: Int,
    isFullscreen: Boolean,
    isLive: Boolean = true,
    currentPosition: Long = 0L,
    duration: Long = 0L,
    onSeek: (Long) -> Unit = {},
    onSeekToLive: () -> Unit = {},
    onPlayPause: () -> Unit,
    onFullscreen: () -> Unit,
    onSettings: (() -> Unit)?,
    onBack: (() -> Unit)? = null,
    onToggleChat: () -> Unit = {},
    livePlayer: Player? = null,
    livePlaybackState: LivePlaybackState = LivePlaybackState(),
    liveTimelineInteractions: MutableInteractionSource = remember { MutableInteractionSource() },
    onSeekLiveTimeline: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().fillMaxHeight(0.35f)
                .background(PlayerControlsTopScrim)
        )
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.4f)
                .background(PlayerControlsBottomScrim)
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Box(Modifier.weight(1f))
            if (isFullscreen && isLive) {
                IconButton(onClick = onToggleChat) {
                    Icon(
                        Icons.AutoMirrored.Filled.Message,
                        contentDescription = stringResource(R.string.toggle_chat_desc),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            if (onSettings != null) IconButton(onClick = onSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_desc),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (playbackState == Player.STATE_BUFFERING) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(50.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(
                    imageVector = if (playWhenReady) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.play_pause_desc),
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        val timelineModifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
        val fullscreenButton: @Composable () -> Unit = {
            IconButton(onClick = onFullscreen) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = stringResource(R.string.fullscreen_desc),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (isLive) {
            val isAtLiveEdge = livePlaybackState.mode == LivePlaybackMode.LIVE
            LiveStreamSeekBar(
                player = livePlayer,
                playback = livePlaybackState,
                onSeek = onSeekLiveTimeline,
                onSeekToLive = onSeekToLive,
                modifier = timelineModifier,
                interactionSource = liveTimelineInteractions,
                status = {
                    val dotColor = if (isAtLiveEdge) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.White.copy(alpha = 0.55f)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(enabled = !isAtLiveEdge, role = Role.Button, onClick = onSeekToLive)
                            .heightIn(min = 48.dp)
                    ) {
                        Box(Modifier.size(6.dp).background(dotColor, CircleShape))
                        Text(
                            text = stringResource(R.string.live),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            ) {
                fullscreenButton()
            }
        } else {
            PlayerTimeline(
                progress = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                positionMs = currentPosition,
                durationMs = duration,
                onValueChange = { value -> onSeek((value * duration).toLong()) },
                modifier = timelineModifier,
                enabled = duration > 0,
                interactionSource = liveTimelineInteractions
            ) {
                fullscreenButton()
            }
        }
    }
}

private val PlayerControlsTopScrim = Brush.verticalGradient(
    listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
)

private val PlayerControlsBottomScrim = Brush.verticalGradient(
    listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
)
