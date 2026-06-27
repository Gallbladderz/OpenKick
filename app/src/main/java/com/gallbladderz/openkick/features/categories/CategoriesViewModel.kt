package com.gallbladderz.openkick.features.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.data.local.FollowsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CategoriesUiState {
    data object Loading : CategoriesUiState
    data class Success(val categories: List<CategoryUiModel>) : CategoriesUiState
    data class Error(val message: String) : CategoriesUiState
}

class CategoriesViewModel(
    private val repository: CategoriesRepository,
    private val followsRepository: FollowsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoriesUiState>(CategoriesUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var currentPage = 1
    private var isLoadingMore = false
    private var isLastPage = false

    init {
        fetchCategories()
    }

    fun isCategoryFollowed(slug: String) = followsRepository.isCategoryFollowed(slug)

    fun toggleCategoryFollow(slug: String, isCurrentlyFollowed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            followsRepository.toggleCategoryFollow(slug, isCurrentlyFollowed)
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
                        _uiState.update { CategoriesUiState.Error("Could not find games") }
                        isLastPage = true
                    } else {
                        
                        val sorted = categories.sortedByDescending { it.viewers }
                        _uiState.update { CategoriesUiState.Success(sorted) }
                    }
                }.onFailure { exception ->
                    _uiState.update { CategoriesUiState.Error(exception.message ?: "Network error") }
                }
            }
        }
    }

    fun loadMoreCategories() {
        if (isLoadingMore || isLastPage || _uiState.value !is CategoriesUiState.Success) return
        isLoadingMore = true
        currentPage++

        viewModelScope.launch {
            repository.fetchCategories(currentPage).collect { result ->
                result.onSuccess { newCategories ->
                    if (newCategories.isEmpty()) {
                        isLastPage = true
                    } else {
                        val currentState = _uiState.value as CategoriesUiState.Success
                        
                        val merged = (currentState.categories + newCategories.sortedByDescending { it.viewers })
                            .distinctBy { it.id }
                        _uiState.update { CategoriesUiState.Success(merged) }
                    }
                    isLoadingMore = false
                }.onFailure {
                    isLoadingMore = false
                }
            }
        }
    }
}