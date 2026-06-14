package com.gallbladderz.openkick.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: HomeRepository) : ViewModel() {
    private val _uiState = MutableStateFlow("Press the button to test Kick API...")
    val uiState: StateFlow<String> = _uiState

    fun loadData() {
        _uiState.value = "Fetching data..."
        viewModelScope.launch {
            repository.fetchTestStream().collect { result ->
                result.fold(
                    onSuccess = { _uiState.value = "Response:\n\n$it" },
                    onFailure = { _uiState.value = "Error:\n${it.message}" }
                )
            }
        }
    }
}