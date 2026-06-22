package com.gallbladderz.openkick.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val streams: List<StreamUiModel>,
        val clips: List<ClipUiModel>
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(private val repository: HomeRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var streamsPage = 1
    private var clipsCursor: String? = null 
    private var isStreamsLoading = false
    private var isClipsLoading = false
    private var isStreamsEnd = false
    private var isClipsEnd = false

    init {
        fetchHomeData()
    }

    fun fetchHomeData() {
        _uiState.value = HomeUiState.Loading
        streamsPage = 1
        clipsCursor = null
        isStreamsEnd = false
        isClipsEnd = false

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val streamsDeferred = async { repository.fetchLivestreams(streamsPage) }
                val clipsDeferred = async { repository.fetchTopClips(clipsCursor) }

                val streamsResult = streamsDeferred.await()
                val clipsResult = clipsDeferred.await()

                
                val streamsList = streamsResult.getOrDefault(emptyList())
                val clipsPair = clipsResult.getOrNull()

                val clipsList = clipsPair?.first ?: emptyList()
                clipsCursor = clipsPair?.second

                
                if (streamsResult.isFailure && clipsResult.isFailure) {
                    val ex = streamsResult.exceptionOrNull() ?: clipsResult.exceptionOrNull()
                    _uiState.value = HomeUiState.Error(ex?.message ?: "Полный провал, ничего не загрузилось")
                } else {
                    _uiState.value = HomeUiState.Success(
                        streams = streamsList,
                        clips = clipsList
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Непредвиденная ошибка")
            }
        }
    }

    fun loadMoreStreams() {
        if (isStreamsLoading || isStreamsEnd || _uiState.value !is HomeUiState.Success) return
        isStreamsLoading = true
        streamsPage++

        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.fetchLivestreams(streamsPage)
            if (result.isSuccess) {
                val newStreams = result.getOrThrow()
                if (newStreams.isEmpty()) {
                    isStreamsEnd = true
                } else {
                    val currentState = _uiState.value as HomeUiState.Success
                    val merged = (currentState.streams + newStreams).distinctBy { it.id }
                    _uiState.value = currentState.copy(streams = merged)
                }
            } else {
                isStreamsEnd = true
            }
            isStreamsLoading = false
        }
    }

    fun loadMoreClips() {
        if (isClipsLoading || isClipsEnd || _uiState.value !is HomeUiState.Success) return
        isClipsLoading = true

        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.fetchTopClips(clipsCursor)
            if (result.isSuccess) {
                val (newClips, nextCursor) = result.getOrThrow()
                if (newClips.isEmpty()) {
                    isClipsEnd = true
                } else {
                    clipsCursor = nextCursor
                    if (nextCursor.isNullOrBlank()) {
                        isClipsEnd = true
                    }
                    val currentState = _uiState.value as HomeUiState.Success
                    val merged = (currentState.clips + newClips).distinctBy { it.id }
                    _uiState.value = currentState.copy(clips = merged)
                }
            } else {
                isClipsEnd = true
            }
            isClipsLoading = false
        }
    }
}