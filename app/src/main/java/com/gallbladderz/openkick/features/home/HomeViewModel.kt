package com.gallbladderz.openkick.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Idle : HomeUiState
    data object Loading : HomeUiState
    data class Success(val data: KickChannelResponse) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(private val repository: HomeRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadData() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            repository.fetchTestStream().collect { result ->
                result.fold(
                    onSuccess = { _uiState.value = HomeUiState.Success(it) },
                    onFailure = { _uiState.value = HomeUiState.Error(it.message ?: "Unknown error") }
                )
            }
        }
    }
}