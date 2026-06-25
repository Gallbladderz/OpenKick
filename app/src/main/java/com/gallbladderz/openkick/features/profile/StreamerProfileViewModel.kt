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

    fun loadProfile(slug: String) {
        _uiState.update { ProfileUiState.Loading }

        viewModelScope.launch {
            val profileResult = repository.fetchProfileInfo(slug)

            if (profileResult.isSuccess) {
                val profile = profileResult.getOrThrow()

                val videosDeferred = async { repository.fetchVideos(profile.channelId) }
                val clipsDeferred = async { repository.fetchClips(profile.slug) }
                val linksDeferred = async { repository.fetchChannelLinks(profile.slug) }

                // Твой репозиторий возвращает Flow, поэтому берем первое значение через first()
                val isFollowingDeferred = async { followsRepository.isStreamerFollowed(profile.slug).first() }

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

                // Дергаем твой правильный метод из репозитория
                followsRepository.toggleStreamerFollow(slug, currentlyFollowing)

                // Сразу меняем стейт на кнопке, чтобы юзер не ждал
                _uiState.update {
                    currentState.copy(isFollowing = !currentlyFollowing)
                }
            }
        }
    }
}