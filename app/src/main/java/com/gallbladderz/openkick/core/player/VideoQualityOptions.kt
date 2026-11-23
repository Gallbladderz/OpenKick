/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.gallbladderz.openkick.R
import kotlin.math.roundToInt

@UnstableApi
internal fun Tracks.toVideoQualityOptions(context: Context): List<VideoQuality> {
    val videoTracks = buildList {
        for (group in groups) {
            if (group.type != C.TRACK_TYPE_VIDEO) continue
            for (index in 0 until group.length) {
                val format = group.getTrackFormat(index)
                if (format.height > 0) add(VideoTrack(group.mediaTrackGroup, index, format))
            }
        }
    }
    val locale = context.resources.configuration.locales[0]
    val manualOptions = videoTracks
        .sortedWith(compareByDescending<VideoTrack> { it.format.height }
            .thenByDescending { it.format.frameRate }
            .thenByDescending { it.bitrate })
        .map { track ->
            val label = buildString {
                append(track.format.height)
                append('p')
                val fps = track.format.frameRate
                if (fps.isFinite() && fps > 0f) {
                    val roundedFps = fps.roundToInt()
                    if (roundedFps > 0) append(roundedFps)
                }
                if (track.bitrate >= 50_000) {
                    append(' ')
                    append(String.format(locale, "%.1f Mbps", track.bitrate / 1_000_000.0))
                }
            }
            VideoQuality(label, track.group, track.index)
        }
        .distinctBy { it.name }

    val auto = VideoQuality(context.getString(R.string.auto_quality), null, null)
    val audioOnly = VideoQuality(context.getString(R.string.audio_only), null, null, isAudioOnly = true)
    return listOf(auto) + manualOptions + audioOnly
}

@UnstableApi
private data class VideoTrack(val group: TrackGroup, val index: Int, val format: Format) {
    val bitrate: Int get() = format.averageBitrate.takeIf { it > 0 } ?: format.peakBitrate
}
