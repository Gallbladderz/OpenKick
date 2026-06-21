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

    init {
        fetchHomeData()
    }

    fun fetchHomeData() {
        _uiState.value = HomeUiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                
                val streamsDeferred = async { getStreamsAsync() }
                val clipsDeferred = async { getClipsAsync() }

                
                val streamsResult = streamsDeferred.await()
                val clipsResult = clipsDeferred.await()

                
                if (streamsResult.isSuccess && clipsResult.isSuccess) {
                    _uiState.value = HomeUiState.Success(
                        streams = streamsResult.getOrThrow(),
                        clips = clipsResult.getOrThrow()
                    )
                } else {
                    val ex = streamsResult.exceptionOrNull() ?: clipsResult.exceptionOrNull()
                    _uiState.value = HomeUiState.Error(ex?.message ?: "Ошибка при загрузке данных")
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Непредвиденная ошибка")
            }
        }
    }

    
    private suspend fun getStreamsAsync(): Result<List<StreamUiModel>> {
        var finalResult: Result<List<StreamUiModel>> = Result.failure(Exception("Not fetched"))

        repository.fetchLivestreams().collect { fetchRes ->
            fetchRes.onSuccess { body ->
                repository.parseStreams(body).collect { parseRes ->
                    finalResult = parseRes
                }
            }.onFailure {
                finalResult = Result.failure(it)
            }
        }
        return finalResult
    }

    
    private suspend fun getClipsAsync(): Result<List<ClipUiModel>> {
        var finalResult: Result<List<ClipUiModel>> = Result.failure(Exception("Not fetched"))

        repository.fetchTopClips().collect { fetchRes ->
            fetchRes.onSuccess { body ->
                repository.parseClips(body).collect { parseRes ->
                    finalResult = parseRes
                }
            }.onFailure {
                finalResult = Result.failure(it)
            }
        }
        return finalResult
    }
}