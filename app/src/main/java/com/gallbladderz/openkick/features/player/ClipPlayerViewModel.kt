/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.features.player


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.gallbladderz.openkick.core.data.repository.ClipRepository
import com.gallbladderz.openkick.core.domain.model.ClipUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClipPlayerViewModel(
    private val clipRepository: ClipRepository,
    context: Context
) : ViewModel() {

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
    private var isReleased = false

    private val _avatarUrl = MutableStateFlow("")
    val avatarUrl = _avatarUrl.asStateFlow()

    private val _activeClip = MutableStateFlow<ClipUiModel?>(null)
    val activeClip = _activeClip.asStateFlow()

    fun loadClip(clipId: String) {
        if (isReleased) return
        viewModelScope.launch {
            val result = clipRepository.fetchClip(clipId)
            result.onSuccess { clip ->
                if (isReleased) return@onSuccess
                _activeClip.update { clip }
                _avatarUrl.update { clip.streamerAvatarUrl }
                player.setMediaItem(MediaItem.fromUri(clip.videoUrl))
                player.prepare()
                player.playWhenReady = true
            }.onFailure {

            }
        }
    }

    fun stopAndRelease() {
        if (isReleased) return
        isReleased = true
        player.stop()
        player.clearMediaItems()
        player.release()
    }

    override fun onCleared() {
        stopAndRelease()
        super.onCleared()
    }
}
