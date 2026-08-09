/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import com.gallbladderz.openkick.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


data class VideoQuality(
    val name: String,
    val trackGroup: TrackGroup?,
    val trackIndex: Int?,
    val isAudioOnly: Boolean = false
)

@UnstableApi
class PlayerManager(
    context: Context,
    private val dataSourceFactory: DataSource.Factory
) {

    private val appContext = context.applicationContext

    var player: ExoPlayer? = null
        private set


    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState = _playbackState.asStateFlow()
    private val _playWhenReady = MutableStateFlow(false)
    val playWhenReady = _playWhenReady.asStateFlow()

    private val _availableQualities = MutableStateFlow<List<VideoQuality>>(emptyList())
    val availableQualities = _availableQualities.asStateFlow()

    private val _selectedQuality = MutableStateFlow<VideoQuality?>(null)
    val selectedQuality = _selectedQuality.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPlaybackStateChanged(state: Int) {
            _playbackState.value = state
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            _playWhenReady.value = playWhenReady
        }

        override fun onTracksChanged(tracks: Tracks) {
            val parsedQualities = mutableListOf<VideoQuality>()

            for (group in tracks.groups) {
                if (group.type == C.TRACK_TYPE_VIDEO) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        if (format.height != Format.NO_VALUE) {
                            val fps = if (format.frameRate > 0) format.frameRate.toInt()
                                .toString() else ""
                            val name = "${format.height}p${if (fps == "60") "60" else ""}"
                            parsedQualities.add(VideoQuality(name, group.mediaTrackGroup, i))
                        }
                    }
                }
            }

            val sortedQualities = parsedQualities
                .distinctBy { it.name }
                .sortedByDescending { it.name.substringBefore("p").toIntOrNull() ?: 0 }

            _availableQualities.value = listOf(
                VideoQuality(appContext.getString(R.string.auto_quality), null, null),
                VideoQuality(
                    appContext.getString(R.string.audio_only),
                    null,
                    null,
                    isAudioOnly = true
                )
            ) + sortedQualities
        }
    }

    fun initializePlayer() {
        if (player == null) {
            player = ExoPlayer.Builder(appContext).build().apply {
                addListener(playerListener)
            }
        }
    }


    fun setQuality(quality: VideoQuality) {
        val currentPlayer = player ?: return

        _selectedQuality.value = quality

        val builder = currentPlayer.trackSelectionParameters.buildUpon()

        if (quality.isAudioOnly) {

            builder.setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            builder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        } else {

            builder.setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)

            if (quality.trackGroup == null || quality.trackIndex == null) {

                builder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            } else {

                builder.setOverrideForType(
                    TrackSelectionOverride(
                        quality.trackGroup,
                        listOf(quality.trackIndex)
                    )
                )
            }
        }

        currentPlayer.trackSelectionParameters = builder.build()


        currentPlayer.play()
    }


    fun seekToLiveEdge() {
        player?.seekToDefaultPosition()
    }

    fun play(videoUrl: String, title: String, streamerName: String, avatarUrl: String) {
        val currentPlayer = player ?: return
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(
                MediaItem.Builder().setUri(videoUrl).setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder().setTitle(title)
                        .setArtist(streamerName).setArtworkUri(android.net.Uri.parse(avatarUrl))
                        .build()
                ).build()
            )
        currentPlayer.setMediaSource(mediaSource)
        currentPlayer.prepare()
        currentPlayer.playWhenReady = true
    }


    fun pause() = player?.pause()

    fun resume() = player?.play()

    fun release() {
        player?.removeListener(playerListener)
        player?.release()
        player = null
        _isPlaying.value = false
        _playbackState.value = Player.STATE_IDLE
    }
}