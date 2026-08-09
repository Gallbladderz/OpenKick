/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.ui.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(
        val channels: List<SearchUiModel>,
        val streams: List<SearchStreamUiModel>,
        val categories: List<SearchCategoryUiModel>
    ) : SearchUiState

    data class Error(val message: UiText) : SearchUiState
}

class SearchViewModel(private val repository: SearchRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun searchStreamer(query: String) {
        if (query.isBlank()) {
            _uiState.update { SearchUiState.Idle }
            return
        }

        _uiState.update { SearchUiState.Loading }
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            repository.searchStreamer(query).collect { result ->
                result.onSuccess { data ->
                    _uiState.update {
                        SearchUiState.Success(
                            data.channels,
                            data.streams,
                            data.categories
                        )
                    }
                }.onFailure { exception ->
                    _uiState.update {
                        SearchUiState.Error(exception.message?.let {
                            UiText.DynamicString(
                                it
                            )
                        } ?: UiText.StringResource(R.string.network_error))
                    }
                }
            }
        }
    }
}
