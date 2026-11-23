/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.media3.common.C
import androidx.media3.common.Player
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.features.player.models.LivePlaybackMode
import com.gallbladderz.openkick.features.player.models.LivePlaybackState
import kotlinx.coroutines.delay

@Composable
internal fun LiveStreamSeekBar(
    player: Player?,
    playback: LivePlaybackState,
    onSeek: (Long) -> Unit,
    onSeekToLive: () -> Unit,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
    status: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    val timeline by produceState(LiveTimelineProgress(), player, playback) {
        if (player == null) {
            value = LiveTimelineProgress()
            return@produceState
        }
        while (true) {
            val mediaDuration = player.duration.coerceAtLeast(0)
            val now = System.currentTimeMillis()
            val fullDuration = playback.archiveStartTimeMs
                ?.let { (now - it).coerceAtLeast(0) }
            val duration = fullDuration ?: mediaDuration
            val position = when (playback.mode) {
                LivePlaybackMode.LIVE -> duration
                LivePlaybackMode.LIVE_WINDOW -> {
                    val offset = player.currentLiveOffset
                    if (fullDuration != null && offset != C.TIME_UNSET) {
                        duration - offset
                    } else {
                        duration - mediaDuration + player.currentPosition
                    }
                }
                LivePlaybackMode.ARCHIVE -> player.currentPosition
            }
            value = LiveTimelineProgress(
                positionMs = position.coerceIn(0, duration),
                durationMs = duration,
                uptimeMs = playback.startedAtMs?.let { (now - it).coerceAtLeast(0) },
                canSeek = duration > 0 && (fullDuration != null || player.isCurrentMediaItemSeekable)
            )
            delay(250)
        }
    }
    var drag by remember(player, playback.archiveStartTimeMs) { mutableStateOf<LiveTimelineDrag?>(null) }
    val duration = drag?.durationMs ?: timeline.durationMs
    val progress = drag?.fraction ?: if (duration > 0) timeline.positionMs.toFloat() / duration else 1f
    val label = stringResource(R.string.live_timeline)

    val showTotal = drag != null || playback.mode != LivePlaybackMode.LIVE
    val positionMs = when {
        !showTotal -> if (playback.archiveStartTimeMs != null) duration else timeline.uptimeMs
        duration > 0 -> drag?.let { (it.fraction * it.durationMs).toLong() } ?: timeline.positionMs
        else -> null
    }
    PlayerTimeline(
        progress = progress,
        positionMs = positionMs,
        durationMs = duration.takeIf { showTotal && it > 0 },
        onValueChange = { value ->
            drag = LiveTimelineDrag(value, drag?.durationMs ?: timeline.durationMs)
        },
        onValueChangeFinished = {
            drag?.let { selection ->
                if (selection.fraction == 1f) {
                    onSeekToLive()
                } else {
                    onSeek((selection.fraction * selection.durationMs).toLong())
                }
            }
            drag = null
        },
        modifier = modifier,
        sliderModifier = Modifier.semantics { contentDescription = label },
        enabled = timeline.canSeek,
        interactionSource = interactionSource,
        status = status,
        actions = actions
    )
}

private data class LiveTimelineProgress(
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val uptimeMs: Long? = null,
    val canSeek: Boolean = false
)

private data class LiveTimelineDrag(val fraction: Float, val durationMs: Long)
