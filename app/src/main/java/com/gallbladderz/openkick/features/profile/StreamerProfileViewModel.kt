package com.gallbladderz.openkick.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.features.home.ClipUiModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(
        val info: ProfileInfoUi,
        val videos: List<VideoUiModel>,
        val clips: List<ClipUiModel>
    ) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

class StreamerProfileViewModel(private val repository: StreamerProfileRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun loadProfile(slug: String) {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            val profileResult = repository.fetchProfileInfo(slug)

            if (profileResult.isSuccess) {
                val profile = profileResult.getOrThrow()
                
                val videosDeferred = async { repository.fetchVideos(profile.channelId) }
                val clipsDeferred = async { repository.fetchClips(profile.slug) }

                val videos = videosDeferred.await().getOrDefault(emptyList())
                val clips = clipsDeferred.await().getOrDefault(emptyList())

                _uiState.value = ProfileUiState.Success(profile, videos, clips)
            } else {
                _uiState.value = ProfileUiState.Error(profileResult.exceptionOrNull()?.message ?: "Ошибка загрузки")
            }
        }
    }
}