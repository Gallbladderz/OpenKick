package com.gallbladderz.openkick.features.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.ui.UiText
import com.gallbladderz.openkick.data.local.FollowsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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
    private val followsRepository: FollowsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoriesUiState>(CategoriesUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var currentPage = 1
    private var isLoadingMore = false
    private var isLastPage = false

    val followedSlugs: StateFlow<Set<String>> = followsRepository.getFollowedCategoriesSlugs()
        .map { it.toSet() }
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
                    isLoadingMore = false
                }.onFailure {
                    isLoadingMore = false
                }
            }
        }
    }
}