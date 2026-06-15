package com.gallbladderz.openkick.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object NeedsCloudflareBypass : HomeUiState
    data object Loading : HomeUiState
    data class Success(val streams: List<StreamUiModel>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(private val repository: HomeRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.NeedsCloudflareBypass)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun processJson(jsonString: String) {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            repository.parseStreams(jsonString).collect { result ->
                result.fold(
                    onSuccess = { _uiState.value = HomeUiState.Success(it) },
                    onFailure = { _uiState.value = HomeUiState.Error(it.message ?: "Ошибка парсинга") }
                )
            }
        }
    }

    fun triggerBypassAgain() {
        _uiState.value = HomeUiState.NeedsCloudflareBypass
    }
}