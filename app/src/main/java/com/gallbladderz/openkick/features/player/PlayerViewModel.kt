package com.gallbladderz.openkick.features.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.gallbladderz.openkick.data.local.FollowsRepository
import com.gallbladderz.openkick.features.player.models.ChannelLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

@UnstableApi
class PlayerViewModel(
    private val repository: PlayerRepository,
    private val chatRepository: ChatRepository,
    private val followsRepository: FollowsRepository,
    val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _channelLinks = MutableStateFlow<List<ChannelLink>>(emptyList())
    val channelLinks = _channelLinks.asStateFlow()

    val chatMessages = chatRepository.chatMessages

    val isPlaying = playerManager.isPlaying
    val playbackState = playerManager.playbackState
    val availableQualities = playerManager.availableQualities
    val selectedQuality = playerManager.selectedQuality

    private var currentPlaybackUrl: String? = null

    fun play() = playerManager.resume()

    fun pause() = playerManager.pause()

    fun setVideoQuality(quality: VideoQuality) {
        playerManager.setQuality(quality)
    }

    fun isStreamerFollowed(streamerName: String) =
        followsRepository.isStreamerFollowed(streamerName)

    fun toggleFollow(
        streamerName: String,
        isCurrentlyFollowed: Boolean
    ) {
        viewModelScope.launch {
            followsRepository.toggleStreamerFollow(
                streamerName,
                isCurrentlyFollowed
            )
        }
    }

    fun loadStreamInfo(streamerName: String) {
        viewModelScope.launch {
            repository.fetchStreamInfo(streamerName)
                .flowOn(Dispatchers.IO)
                .collect { result ->

                    result.onSuccess { streamInfo ->

                        _uiState.update {
                            PlayerUiState.Playing(
                                url = streamInfo.playbackUrl,
                                avatarUrl = streamInfo.avatarUrl,
                                viewers = streamInfo.viewers,
                                title = streamInfo.title
                            )
                        }

                        loadChannelLinks(streamerName)

                        if (currentPlaybackUrl != streamInfo.playbackUrl) {
                            currentPlaybackUrl = streamInfo.playbackUrl

                            playerManager.play(streamInfo.playbackUrl)

                            streamInfo.chatroomId?.let { chatroomId ->
                                chatRepository.connectToChat(chatroomId)
                            }
                        }
                    }.onFailure { exception ->
                        _uiState.update {
                            PlayerUiState.Error(
                                exception.message ?: "Unknown error"
                            )
                        }
                    }
                }
        }
    }

    fun loadChannelLinks(streamerName: String) {
        viewModelScope.launch {
            repository.fetchChannelLinks(streamerName)
                .onSuccess { links ->
                    _channelLinks.value = links
                }
                .onFailure {
                    Log.e(
                        "PlayerVM",
                        "Panels did not load",
                        it
                    )
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.disconnect()
        playerManager.release()
    }
}