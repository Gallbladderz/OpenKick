package com.gallbladderz.openkick.features.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.currentCoroutineContext

@UnstableApi
class PlayerManager(
    private val context: Context,
    private val dataSourceFactory: DataSource.Factory
) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState = _playbackState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    val currentPosition = flow {
        while (currentCoroutineContext().isActive) {
            emit(player.currentPosition.coerceAtLeast(0L))
            delay(1000)
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _playbackState.value = playbackState
                if (playbackState == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0L)
                }
            }
        })
    }

    fun play(videoUrl: String) {
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUrl))

        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        player.play()
    }

    fun release() {
        player.release()
    }
}
