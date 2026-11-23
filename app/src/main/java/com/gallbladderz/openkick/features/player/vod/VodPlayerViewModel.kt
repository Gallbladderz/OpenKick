/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.features.player.vod


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.domain.DomainError
import com.gallbladderz.openkick.core.player.PlayerManager
import com.gallbladderz.openkick.core.player.VodPlaybackProgress
import com.gallbladderz.openkick.core.player.vodProgressKey
import com.gallbladderz.openkick.features.profile.StreamerProfileRepository
import com.gallbladderz.openkick.features.profile.VideoUiModel
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

@UnstableApi
class VodPlayerViewModel(
    private val videoId: String,
    private val channelSlug: String?,
    context: Context,
    dataSourceFactory: DataSource.Factory,
    private val repository: StreamerProfileRepository,
    private val playbackProgress: VodPlaybackProgress
) : ViewModel() {

    val playerManager = PlayerManager(context, dataSourceFactory)
    private val defaultTitle = context.getString(R.string.vod_desc)
    private val loadError = context.getString(R.string.load_error)
    private val networkError = context.getString(R.string.network_error)
    private val progressKey = vodProgressKey(videoId)
    private var progressTracking: AutoCloseable? = null

    private val _uiState = MutableStateFlow<VodPlayerUiState>(VodPlayerUiState.Loading)
    val uiState: StateFlow<VodPlayerUiState> = _uiState.asStateFlow()

    init {
        playerManager.initializePlayer()
        loadVod()
    }

    private fun loadVod() {
        viewModelScope.launch {
            _uiState.update { VodPlayerUiState.Loading }

            repository.fetchVideoPlayback(videoId, defaultTitle, channelSlug).onSuccess { metadata ->
                playerManager.play(
                    videoUrl = metadata.videoUrl,
                    title = metadata.title,
                    streamerName = metadata.channelUsername,
                    avatarUrl = "",
                    isLive = false,
                    startPositionMs = playbackProgress.position(progressKey)
                )
                progressTracking = playerManager.player?.let {
                    playbackProgress.track(it, progressKey, viewModelScope)
                }
                _uiState.update { VodPlayerUiState.Success(metadata, metadata.videoUrl) }
            }.onFailure { error ->
                val errorMessage = if (error is DomainError.NetworkError) networkError else loadError
                _uiState.update { VodPlayerUiState.Error(errorMessage) }
            }
        }
    }

    override fun onCleared() {
        progressTracking?.close()
        playerManager.release()
        super.onCleared()
    }
}
