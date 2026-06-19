package com.gallbladderz.openkick.features.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.data.local.FollowsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val repository: PlayerRepository,
    private val chatRepository: ChatRepository,
    private val followsRepository: FollowsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState = _uiState.asStateFlow()

    val chatMessages = chatRepository.chatMessages

    fun isStreamerFollowed(streamerName: String) = followsRepository.isStreamerFollowed(streamerName)

    fun toggleFollow(streamerName: String, isCurrentlyFollowed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            followsRepository.toggleStreamerFollow(streamerName, isCurrentlyFollowed)
        }
    }

    fun loadStreamInfo(streamerName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.fetchStreamInfo(streamerName).collect { result ->
                result.onSuccess { streamInfo ->
                    _uiState.value = PlayerUiState.Playing(
                        url = streamInfo.playbackUrl,
                        avatarUrl = streamInfo.avatarUrl,
                        viewers = streamInfo.viewers,
                        title = streamInfo.title
                    )
                    streamInfo.chatroomId?.let { chatroomId ->
                        chatRepository.connectToChat(chatroomId)
                    }
                }.onFailure { exception ->
                    _uiState.value = PlayerUiState.Error(exception.message ?: "Неизвестная ошибка")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.disconnect()
    }
}
