/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.features.categories


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.domain.model.ClipUiModel
import com.gallbladderz.openkick.core.domain.model.StreamUiModel
import com.gallbladderz.openkick.core.ui.UiText
import com.gallbladderz.openkick.features.following.FollowingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CategoryDetailsUiState {
    data object Loading : CategoryDetailsUiState
    data class Success(
        val name: String,
        val bannerUrl: String,
        val viewers: Int,
        val tags: List<String>,
        val clips: List<ClipUiModel>,
        val streams: List<StreamUiModel>
    ) : CategoryDetailsUiState

    data class Error(val message: UiText) : CategoryDetailsUiState
}

class CategoryDetailsViewModel(
    private val repository: CategoriesRepository,
    private val followingRepository: FollowingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryDetailsUiState>(CategoryDetailsUiState.Loading)
    val uiState = _uiState.asStateFlow()
    val followErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)

    private var currentCategoryId: Int? = null
    private var streamsCursor: String? = null
    private var clipsCursor: String? = null
    private var isStreamsLoading = false
    private var isClipsLoading = false
    private var isStreamsEnd = false
    private var isClipsEnd = false
    private var currentSlug: String = ""
    private var loadJob: Job? = null

    private val _sort = MutableStateFlow("viewer_count_desc")
    val sort = _sort.asStateFlow()

    private val _selectedFilterLanguages = MutableStateFlow<Set<String>>(emptySet())
    val selectedFilterLanguages = _selectedFilterLanguages.asStateFlow()

    val followedSlugs: StateFlow<Set<String>> = followingRepository.followedCategorySlugs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

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

    fun updateFiltersAndRefresh(newSort: String, newLangs: Set<String>) {
        _sort.update { newSort }
        _selectedFilterLanguages.update { newLangs }
        if (currentSlug.isNotBlank()) {
            loadCategory(currentSlug)
        }
    }

    fun loadCategory(slug: String) {
        loadJob?.cancel()
        currentSlug = slug.trim().lowercase()
        _uiState.update { CategoryDetailsUiState.Loading }
        streamsCursor = null
        clipsCursor = null
        isStreamsLoading = false
        isClipsLoading = false
        isStreamsEnd = false
        isClipsEnd = false
        currentCategoryId = null

        loadJob = viewModelScope.launch {
            if (currentSlug.isBlank()) {
                _uiState.update { CategoryDetailsUiState.Error(UiText.StringResource(R.string.error_empty_slug)) }
                return@launch
            }

            try {
                val detailsResult = repository.fetchCategoryDetails(currentSlug)
                val details = detailsResult.getOrNull()
                if (details == null) {
                    _uiState.update {
                        CategoryDetailsUiState.Error(
                            detailsResult.exceptionOrNull()?.message?.let { message ->
                                UiText.DynamicString(
                                    message
                                )
                            }
                                ?: UiText.StringResource(R.string.unknown_error)
                        )
                    }
                    return@launch
                }
                currentCategoryId = details.id

                val clipsDeferred = async { repository.fetchCategoryClips(currentSlug) }
                val streamsDeferred = async {
                    repository.fetchCategoryStreams(
                        categoryId = details.id,
                        sort = _sort.value,
                        languages = selectedLanguages()
                    )
                }

                val clipsResult = clipsDeferred.await()
                val streamsResult = streamsDeferred.await()

                val parsedClips = clipsResult.getOrNull()?.first ?: emptyList()
                clipsCursor = clipsResult.getOrNull()?.second

                val parsedStreams = streamsResult.getOrNull()?.first ?: emptyList()
                streamsCursor = streamsResult.getOrNull()?.second

                _uiState.update {
                    CategoryDetailsUiState.Success(
                        name = details.name,
                        bannerUrl = details.bannerUrl,
                        viewers = details.viewers,
                        tags = details.tags,
                        clips = parsedClips,
                        streams = parsedStreams
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    CategoryDetailsUiState.Error(
                        UiText.StringResource(
                            R.string.network_error_msg,
                            e.message ?: ""
                        )
                    )
                }
            }
        }
    }

    fun loadMoreStreams() {
        if (isStreamsLoading || isStreamsEnd || _uiState.value !is CategoryDetailsUiState.Success) return

        val categoryId = currentCategoryId ?: return
        isStreamsLoading = true

        viewModelScope.launch {
            try {
                val result = repository.fetchCategoryStreams(
                    categoryId = categoryId,
                    cursor = streamsCursor,
                    sort = _sort.value,
                    languages = selectedLanguages()
                )

                if (result.isSuccess) {
                    val (newStreams, nextCursor) = result.getOrThrow()
                    streamsCursor = nextCursor
                    if (newStreams.isEmpty()) {
                        isStreamsEnd = true
                    } else {
                        isStreamsEnd = nextCursor.isNullOrBlank()
                        _uiState.update { state ->
                            if (state is CategoryDetailsUiState.Success) {
                                val merged = (state.streams + newStreams).distinctBy { it.id }
                                state.copy(streams = merged)
                            } else state
                        }
                    }
                } else {
                    isStreamsEnd = true
                }
            } finally {
                isStreamsLoading = false
            }
        }
    }

    fun loadMoreClips() {
        if (isClipsLoading || isClipsEnd || _uiState.value !is CategoryDetailsUiState.Success) return

        isClipsLoading = true
        viewModelScope.launch {
            try {
                val result = repository.fetchCategoryClips(currentSlug, clipsCursor)
                if (result.isSuccess) {
                    val (newClips, nextCursor) = result.getOrThrow()
                    clipsCursor = nextCursor
                    if (newClips.isEmpty()) {
                        isClipsEnd = true
                    } else {
                        isClipsEnd = nextCursor.isNullOrBlank()
                        _uiState.update { state ->
                            if (state is CategoryDetailsUiState.Success) {
                                val merged = (state.clips + newClips).distinctBy { it.id }
                                state.copy(clips = merged)
                            } else state
                        }
                    }
                } else {
                    isClipsEnd = true
                }
            } finally {
                isClipsLoading = false
            }
        }
    }

    private fun selectedLanguages(): List<String>? =
        _selectedFilterLanguages.value.takeIf { it.isNotEmpty() }?.toList()
}
