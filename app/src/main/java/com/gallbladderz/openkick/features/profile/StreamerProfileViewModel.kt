/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.domain.DomainError
import com.gallbladderz.openkick.core.ui.UiText
import com.gallbladderz.openkick.data.local.FollowsRepository
import com.gallbladderz.openkick.features.home.ClipUiModel
import com.gallbladderz.openkick.features.player.ClipRepository
import com.gallbladderz.openkick.features.player.models.ChannelLink
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(
        val info: ProfileInfoUi,
        val videos: List<VideoUiModel>,
        val clips: List<ClipUiModel>,
        val links: List<ChannelLink>,
        val isFollowing: Boolean
    ) : ProfileUiState

    data class Error(val message: UiText) : ProfileUiState
}

class StreamerProfileViewModel(
    private val repository: StreamerProfileRepository,
    private val followsRepository: FollowsRepository,
    private val clipRepository: ClipRepository
) : ViewModel() {

    private var clipsCursor: String? = null
    private var isClipsLoading = false
    private var isClipsEnd = false

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private var currentSlug: String? = null

    fun loadProfile(slug: String) {
        currentSlug = slug
        _uiState.update { ProfileUiState.Loading }

        viewModelScope.launch {
            fetchData(slug)
        }
    }

    fun refresh() {
        val slug = currentSlug ?: return
        if (_isRefreshing.value) return

        _isRefreshing.update { true }

        viewModelScope.launch {
            try {
                fetchData(slug)
            } finally {
                _isRefreshing.update { false }
            }
        }
    }

    private suspend fun fetchData(slug: String) {
        val profileResult = repository.fetchProfileInfo(slug)

        if (profileResult.isSuccess) {
            val profile = profileResult.getOrThrow()

            val videosDeferred = viewModelScope.async { repository.fetchVideos(profile.slug) }
            val clipsDeferred = viewModelScope.async { repository.fetchClips(profile.slug) }
            val linksDeferred = viewModelScope.async { repository.fetchChannelLinks(profile.slug) }
            val isFollowingDeferred =
                viewModelScope.async { followsRepository.isStreamerFollowed(profile.slug).first() }

            val videos = videosDeferred.await().getOrDefault(emptyList())
            val clipsResult = clipsDeferred.await().getOrNull()
            val clips = clipsResult?.first ?: emptyList()
            clipsCursor = clipsResult?.second
            isClipsLoading = false
            isClipsEnd = false
            val links = linksDeferred.await().getOrDefault(emptyList())
            val isFollowing = isFollowingDeferred.await()

            _uiState.update {
                ProfileUiState.Success(
                    info = profile,
                    videos = videos,
                    clips = clips,
                    links = links,
                    isFollowing = isFollowing
                )
            }
        } else {
            if (_uiState.value is ProfileUiState.Loading) {
                _uiState.update {
                    ProfileUiState.Error(
                        profileResult.exceptionOrNull()
                            ?.let {
                                if (it is DomainError) UiText.DynamicString(
                                    it.message ?: ""
                                ) else UiText.StringResource(R.string.load_error)
                            }
                            ?: UiText.StringResource(R.string.load_error)
                    )
                }
            }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is ProfileUiState.Success) {
                val currentlyFollowing = currentState.isFollowing
                val slug = currentState.info.slug

                followsRepository.toggleStreamerFollow(slug, currentlyFollowing)

                _uiState.update { state ->
                    if (state is ProfileUiState.Success) {
                        state.copy(isFollowing = !currentlyFollowing)
                    } else state
                }
            }
        }
    }

    fun loadMoreClips() {
        if (_uiState.value !is ProfileUiState.Success) return
        val slug = currentSlug ?: return

        if (isClipsLoading || isClipsEnd) return
        isClipsLoading = true

        viewModelScope.launch {
            val result = repository.fetchClips(slug, clipsCursor)
            if (result.isSuccess) {
                val (newClips, nextCursor) = result.getOrThrow()
                clipsCursor = nextCursor
                if (newClips.isEmpty()) {
                    isClipsEnd = true
                } else {
                    if (nextCursor.isNullOrBlank()) isClipsEnd = true
                    _uiState.update { state ->
                        if (state is ProfileUiState.Success) {
                            val merged = (state.clips + newClips).distinctBy { it.id }
                            state.copy(clips = merged)
                        } else state
                    }
                }
            } else {
                isClipsEnd = true
            }
            isClipsLoading = false
        }
    }

}