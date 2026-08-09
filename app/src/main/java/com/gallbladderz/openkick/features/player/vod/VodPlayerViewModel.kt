/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.vod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.features.player.PlayerManager
import com.gallbladderz.openkick.features.profile.StreamerProfileRepository
import com.gallbladderz.openkick.features.profile.VideoUiModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class VodPlayerUiState {
    object Loading : VodPlayerUiState()
    data class Success(val metadata: VideoUiModel, val playbackUrl: String) : VodPlayerUiState()
    data class Error(val message: String) : VodPlayerUiState()
}

class VodPlayerViewModel(
    private val videoId: String,
    val playerManager: PlayerManager,
    private val repository: StreamerProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<VodPlayerUiState>(VodPlayerUiState.Loading)
    val uiState: StateFlow<VodPlayerUiState> = _uiState.asStateFlow()

    init {
        playerManager.initializePlayer()
        loadVod()
    }

    fun loadVod() {
        viewModelScope.launch {
            _uiState.update { VodPlayerUiState.Loading }

            val metadata = repository.getCachedVideo(videoId)

            if (metadata == null) {
                _uiState.update { VodPlayerUiState.Error("VOD metadata not found in cache") }
                return@launch
            }

            val urlDeferred = async { repository.fetchVideoPlaybackUrl(videoId) }
            val urlResult = urlDeferred.await()

            if (urlResult.isSuccess) {
                val url = urlResult.getOrThrow()

                _uiState.update { VodPlayerUiState.Success(metadata, url) }

                playerManager.play(
                    videoUrl = url,
                    title = metadata.title,
                    streamerName = metadata.channelUsername,
                    avatarUrl = ""
                )
            } else {
                val errorMessage = urlResult.exceptionOrNull()?.message
                    ?: "Unknown error loading VOD"
                _uiState.update { VodPlayerUiState.Error(errorMessage) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
