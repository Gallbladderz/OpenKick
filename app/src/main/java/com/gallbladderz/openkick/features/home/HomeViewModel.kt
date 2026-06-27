package com.gallbladderz.openkick.features.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.core.datastore.SettingsRepository
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

class HomeViewModel(
    private val repository: HomeRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState = _uiState.asStateFlow()

    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private var streamsCursor: String? = null
    private var clipsCursor: String? = null
    private var isStreamsLoading = false
    private var isClipsLoading = false
    private var isStreamsEnd = false
    private var isClipsEnd = false

    private var currentLanguages: Set<String>? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.selectedLanguagesFlow.collect { langs ->
                if (currentLanguages != langs) {
                    currentLanguages = langs
                    fetchHomeData()
                }
            }
        }
    }

    fun fetchHomeData() {
        val langs = currentLanguages ?: return

        _uiState.value = HomeUiState.Loading
        streamsCursor = null
        clipsCursor = null
        isStreamsEnd = false
        isClipsEnd = false

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val streamsDeferred = async {
                    repository.fetchLivestreams(cursor = null, languages = langs)
                }
                val clipsDeferred = async { repository.fetchTopClips(null) }

                val streamsResult = streamsDeferred.await()
                val clipsResult = clipsDeferred.await()

                val streamsPair = streamsResult.getOrNull()
                val streamsList = streamsPair?.first ?: emptyList()
                streamsCursor = streamsPair?.second

                val clipsPair = clipsResult.getOrNull()
                val clipsList = clipsPair?.first ?: emptyList()
                clipsCursor = clipsPair?.second

                if (streamsResult.isFailure && clipsResult.isFailure) {
                    val ex = streamsResult.exceptionOrNull() ?: clipsResult.exceptionOrNull()
                    _uiState.value = HomeUiState.Error(ex?.message ?: "Total failure, nothing loaded")
                } else {
                    _uiState.value = HomeUiState.Success(
                        streams = streamsList,
                        clips = clipsList
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unexpected error")
            }
        }
    }

    
    fun refresh() {
        val langs = currentLanguages ?: return
        if (_isRefreshing.value) return 

        _isRefreshing.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val streamsDeferred = async {
                    repository.fetchLivestreams(cursor = null, languages = langs)
                }
                val clipsDeferred = async { repository.fetchTopClips(null) }

                val streamsResult = streamsDeferred.await()
                val clipsResult = clipsDeferred.await()

                val streamsPair = streamsResult.getOrNull()
                val clipsPair = clipsResult.getOrNull()

                if (streamsPair != null || clipsPair != null) {
                    streamsCursor = streamsPair?.second ?: streamsCursor
                    clipsCursor = clipsPair?.second ?: clipsCursor

                    isStreamsEnd = false
                    isClipsEnd = false

                    _uiState.value = HomeUiState.Success(
                        streams = streamsPair?.first ?: emptyList(),
                        clips = clipsPair?.first ?: emptyList()
                    )
                }
            } finally {
                
                _isRefreshing.value = false
            }
        }
    }

    fun loadMoreStreams() {
        val langs = currentLanguages ?: return

        if (isStreamsLoading || isStreamsEnd || _uiState.value !is HomeUiState.Success) {
            return
        }

        isStreamsLoading = true

        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.fetchLivestreams(cursor = streamsCursor, languages = langs)

            if (result.isSuccess) {
                val (newStreams, nextCursor) = result.getOrThrow()
                streamsCursor = nextCursor

                if (newStreams.isEmpty()) {
                    isStreamsEnd = true
                } else {
                    if (nextCursor.isNullOrBlank()) {
                        isStreamsEnd = true
                    }
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