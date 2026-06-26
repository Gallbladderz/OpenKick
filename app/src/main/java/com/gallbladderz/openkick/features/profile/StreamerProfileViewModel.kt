package com.gallbladderz.openkick.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.data.local.FollowsRepository
import com.gallbladderz.openkick.features.home.ClipUiModel
import com.gallbladderz.openkick.features.player.models.ChannelLink
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    data class Error(val message: String) : ProfileUiState
}

class StreamerProfileViewModel(
    private val repository: StreamerProfileRepository,
    private val followsRepository: FollowsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState = _uiState.asStateFlow()

    // Тот самый круглешок
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    // Запоминаем слаг, чтобы потом обновить нужный профиль
    private var currentSlug: String? = null

    fun loadProfile(slug: String) {
        currentSlug = slug
        _uiState.update { ProfileUiState.Loading }

        viewModelScope.launch {
            fetchData(slug)
        }
    }

    // Рефреш, который вызывается при свайпе
    fun refresh() {
        val slug = currentSlug ?: return
        if (_isRefreshing.value) return // защита от двойного свайпа

        _isRefreshing.value = true

        viewModelScope.launch {
            try {
                fetchData(slug)
            } finally {
                // Выключаем крутилку в любом случае
                _isRefreshing.value = false
            }
        }
    }

    // Вынес саму логику загрузки в отдельный метод, чтобы не дублировать код
    private suspend fun fetchData(slug: String) {
        val profileResult = repository.fetchProfileInfo(slug)

        if (profileResult.isSuccess) {
            val profile = profileResult.getOrThrow()

            val videosDeferred = viewModelScope.async { repository.fetchVideos(profile.channelId) }
            val clipsDeferred = viewModelScope.async { repository.fetchClips(profile.slug) }
            val linksDeferred = viewModelScope.async { repository.fetchChannelLinks(profile.slug) }
            val isFollowingDeferred = viewModelScope.async { followsRepository.isStreamerFollowed(profile.slug).first() }

            val videos = videosDeferred.await().getOrDefault(emptyList())
            val clips = clipsDeferred.await().getOrDefault(emptyList())
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
            // Если мы просто обновляли и упала ошибка, можно оставить старый стейт.
            // Но если это была первая загрузка - кидаем ошибку.
            if (_uiState.value is ProfileUiState.Loading) {
                _uiState.update {
                    ProfileUiState.Error(
                        profileResult.exceptionOrNull()?.message ?: "Load error"
                    )
                }
            }
        }
    }

    fun toggleFollow() {
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success) {
            viewModelScope.launch {
                val currentlyFollowing = currentState.isFollowing
                val slug = currentState.info.slug

                followsRepository.toggleStreamerFollow(slug, currentlyFollowing)

                // Мгновенный оптимистичный апдейт кнопки, чтобы юзер не ждал
                _uiState.update {
                    currentState.copy(isFollowing = !currentlyFollowing)
                }
            }
        }
    }
}