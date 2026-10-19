/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.features.player


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.auth.AppMode
import com.gallbladderz.openkick.core.auth.SessionManager
import com.gallbladderz.openkick.core.domain.chat.ChatMessage
import com.gallbladderz.openkick.core.domain.chat.ChatMode
import com.gallbladderz.openkick.core.domain.model.ChannelLink
import com.gallbladderz.openkick.core.player.PlayerManager
import com.gallbladderz.openkick.core.player.VideoQuality
import com.gallbladderz.openkick.core.ui.UiText
import com.gallbladderz.openkick.features.following.FollowingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@UnstableApi
class PlayerViewModel(
    private val repository: PlayerRepository,
    private val chatRepository: ChatRepository,
    private val followingRepository: FollowingRepository,
    val playerManager: PlayerManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _channelLinks = MutableStateFlow<List<ChannelLink>>(emptyList())
    val channelLinks = _channelLinks.asStateFlow()

    val chatMessages = chatRepository.chatMessages
    val chatSendState = chatRepository.chatSendState
    val chatEmoteSets = chatRepository.chatEmoteSets
    private val _chatMode = MutableStateFlow(ChatMode())
    val chatMode = _chatMode.asStateFlow()

    val isAuthenticated: StateFlow<Boolean> =
        sessionManager.mode.map { it == AppMode.AUTHENTICATED }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var currentStreamerName: String? = null

    init {
        viewModelScope.launch {
            isAuthenticated.collect { authenticated ->
                if (authenticated) currentStreamerName?.let(chatRepository::loadEmotes)
                else chatRepository.clearEmotes()
            }
        }
    }

    val isPlaying = playerManager.isPlaying
    val playbackState = playerManager.playbackState
    val availableQualities = playerManager.availableQualities
    val selectedQuality = playerManager.selectedQuality

    private var currentPlaybackUrl: String? = null
    private var streamInfoJob: Job? = null
    private var channelLinksJob: Job? = null

    fun play() = playerManager.resume()

    fun pause() = playerManager.pause()

    fun setVideoQuality(quality: VideoQuality) {
        playerManager.setQuality(quality)
    }

    fun disconnectChat() {
        streamInfoJob?.cancel()
        channelLinksJob?.cancel()
        currentPlaybackUrl = null
        currentStreamerName = null
        _uiState.update { PlayerUiState.Loading }
        _channelLinks.update { emptyList() }
        _chatMode.update { ChatMode() }
        chatRepository.disconnect()
    }

    fun sendChatMessage(content: String, replyTo: ChatMessage? = null) {
        chatRepository.sendMessage(content, replyTo)
    }

    fun isStreamerFollowed(streamerName: String) =
        followingRepository.isStreamerFollowed(streamerName)

    fun toggleFollow(
        streamerName: String,
        isCurrentlyFollowed: Boolean
    ) {
        viewModelScope.launch {
            runCatching {
                followingRepository.toggleStreamerFollow(
                    streamerName,
                    isCurrentlyFollowed
                )
            }.onFailure {
                Log.e("PlayerVM", "Failed to toggle follow", it)
            }
        }
    }

    fun loadStreamInfo(streamerName: String) {
        streamInfoJob?.cancel()
        currentStreamerName = streamerName
        _chatMode.update { ChatMode() }
        chatRepository.setChatMode(ChatMode())
        streamInfoJob = viewModelScope.launch {
            repository.fetchStreamInfo(streamerName)
                .flowOn(Dispatchers.IO)
                .collect { result ->
                    if (currentStreamerName != streamerName) return@collect

                    result.onSuccess { streamInfo ->

                        _uiState.update {
                            PlayerUiState.Playing(
                                url = streamInfo.playbackUrl,
                                avatarUrl = streamInfo.avatarUrl,
                                viewers = streamInfo.viewers,
                                title = streamInfo.title,
                                categoryName = streamInfo.categoryName,
                                categorySlug = streamInfo.categorySlug,
                                isVerified = streamInfo.isVerified
                            )
                        }

                        loadChannelLinks(streamerName)
                        chatRepository.loadEmotes(streamerName)
                        chatRepository.setChannelSubscriberBadges(streamInfo.subscriberBadges)
                        chatRepository.setChatMode(streamInfo.chatMode)
                        _chatMode.update { streamInfo.chatMode }

                        if (currentPlaybackUrl != streamInfo.playbackUrl) {
                            currentPlaybackUrl = streamInfo.playbackUrl

                            playerManager.play(
                                streamInfo.playbackUrl,
                                streamInfo.title,
                                streamerName,
                                streamInfo.avatarUrl
                            )
                        }

                        if (!streamInfo.chatroomId.isNullOrBlank()) {
                            chatRepository.connectToChat(streamInfo.chatroomId)
                        } else {
                            chatRepository.reportSystemMessage(
                                UiText.StringResource(R.string.chatroom_id_missing, streamerName)
                            )
                        }
                    }
                }
        }
    }

    fun loadChannelLinks(streamerName: String) {
        channelLinksJob?.cancel()
        channelLinksJob = viewModelScope.launch {
            repository.fetchChannelLinks(streamerName)
                .onSuccess { links ->
                    if (currentStreamerName != streamerName) return@onSuccess
                    _channelLinks.update { links }
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
        playerManager.release()
        super.onCleared()
        chatRepository.disconnect()

    }
}
