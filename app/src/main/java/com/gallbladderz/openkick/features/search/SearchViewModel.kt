package com.gallbladderz.openkick.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val channels: List<SearchUiModel>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel(private val repository: SearchRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun searchStreamer(query: String) {
        if (query.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        _uiState.value = SearchUiState.Loading
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            repository.searchStreamer(query).collect { result ->
                result.onSuccess { channels ->
                    _uiState.value = SearchUiState.Success(channels)
                }.onFailure { exception ->
                    _uiState.value = SearchUiState.Error(exception.message ?: "Ошибка сети")
                }
            }
        }
    }
}