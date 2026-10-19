/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.features.categories


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.ui.UiText
import com.gallbladderz.openkick.features.following.FollowingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CategoriesUiState {
    data object Loading : CategoriesUiState
    data class Success(val categories: List<CategoryUiModel>) : CategoriesUiState
    data class Error(val message: UiText) : CategoriesUiState
}

class CategoriesViewModel(
    private val repository: CategoriesRepository,
    private val followingRepository: FollowingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoriesUiState>(CategoriesUiState.Loading)
    val uiState = _uiState.asStateFlow()
    val followErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)

    private var currentPage = 1
    private var isLoadingMore = false
    private var isLastPage = false

    val followedSlugs: StateFlow<Set<String>> = followingRepository.followedCategorySlugs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    init {
        fetchCategories()
    }

    fun toggleCategoryFollow(slug: String, isCurrentlyFollowed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                followingRepository.toggleCategoryFollow(slug, isCurrentlyFollowed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                followErrors.tryEmit(e.message ?: "Could not update category")
            }
        }
    }

    fun refreshFollowedCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                followingRepository.refreshCategories()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                followErrors.tryEmit(e.message ?: "Could not load followed categories")
            }
        }
    }

    fun fetchCategories() {
        currentPage = 1
        isLastPage = false
        _uiState.update { CategoriesUiState.Loading }

        viewModelScope.launch {
            repository.fetchCategories(currentPage).collect { result ->
                result.onSuccess { categories ->
                    if (categories.isEmpty()) {
                        _uiState.update { CategoriesUiState.Error(UiText.StringResource(R.string.could_not_find_games)) }
                        isLastPage = true
                    } else {

                        val sorted = categories.sortedByDescending { it.viewers }
                        _uiState.update { CategoriesUiState.Success(sorted) }
                    }
                }.onFailure { exception ->
                    _uiState.update {
                        CategoriesUiState.Error(
                            exception.message?.let { UiText.DynamicString(it) }
                                ?: UiText.StringResource(R.string.network_error)
                        )
                    }
                }
            }
        }
    }

    fun loadMoreCategories() {
        if (isLoadingMore || isLastPage || _uiState.value !is CategoriesUiState.Success) return
        isLoadingMore = true
        currentPage++

        viewModelScope.launch {
            try {
                repository.fetchCategories(currentPage).collect { result ->
                    result.onSuccess { newCategories ->
                        if (newCategories.isEmpty()) {
                            isLastPage = true
                        } else {
                            val currentState = _uiState.value as CategoriesUiState.Success
                            val merged =
                                (currentState.categories + newCategories.sortedByDescending { it.viewers })
                                    .distinctBy { it.id }
                            _uiState.update { CategoriesUiState.Success(merged) }
                        }
                    }.onFailure {
                        currentPage--
                    }
                }
            } finally {
                isLoadingMore = false
            }
        }
    }
}
