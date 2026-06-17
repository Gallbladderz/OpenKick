package com.gallbladderz.openkick.features.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val streams: List<StreamUiModel>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(private val repository: HomeRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        fetchLivestreams()
    }

    fun fetchLivestreams() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            repository.fetchLivestreams().collect { result ->
                result.onSuccess { responseBody ->
                    repository.parseStreams(responseBody).collect { parseResult ->
                        parseResult.onSuccess { streams ->
                            _uiState.value = HomeUiState.Success(streams)
                        }.onFailure { exception ->
                            _uiState.value = HomeUiState.Error(exception.message ?: "Ошибка парсинга")
                        }
                    }
                }.onFailure { exception ->
                    _uiState.value = HomeUiState.Error(exception.message ?: "Ошибка сети")
                }
            }
        }
    }
}
