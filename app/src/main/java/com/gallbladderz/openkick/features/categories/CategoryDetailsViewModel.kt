package com.gallbladderz.openkick.features.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.features.home.ClipUiModel
import com.gallbladderz.openkick.features.home.StreamUiModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    data class Error(val message: String) : CategoryDetailsUiState
}

class CategoryDetailsViewModel(
    private val repository: CategoriesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryDetailsUiState>(CategoryDetailsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var streamsCursor: String? = null
    private var clipsCursor: String? = null
    private var isStreamsLoading = false
    private var isClipsLoading = false
    private var isStreamsEnd = false
    private var isClipsEnd = false
    private var currentSlug: String = ""

    fun loadCategory(slug: String) {
        currentSlug = slug.trim().lowercase()
        _uiState.update { CategoryDetailsUiState.Loading }
        streamsCursor = null
        clipsCursor = null
        isStreamsLoading = false
        isClipsLoading = false
        isStreamsEnd = false
        isClipsEnd = false

        viewModelScope.launch {
            if (currentSlug.isBlank()) {
                _uiState.update { CategoryDetailsUiState.Error("Error: empty slug!") }
                return@launch
            }

            try {
                val detailsDeferred = async { repository.fetchCategoryDetails(currentSlug) }
                val clipsDeferred = async { repository.fetchCategoryClips(currentSlug) }
                val streamsDeferred = async { repository.fetchCategoryStreams(currentSlug) }

                val detailsResult = detailsDeferred.await()
                val clipsResult = clipsDeferred.await()
                val streamsResult = streamsDeferred.await()

                if (detailsResult.isFailure) {
                    _uiState.update {
                        CategoryDetailsUiState.Error(
                            detailsResult.exceptionOrNull()?.message ?: "Unknown error"
                        )
                    }
                    return@launch
                }

                val details = detailsResult.getOrNull()!!

                val clipsPair = clipsResult.getOrNull()
                val parsedClips = clipsPair?.first ?: emptyList()
                clipsCursor = clipsPair?.second

                val streamsPair = streamsResult.getOrNull()
                val parsedStreams = streamsPair?.first ?: emptyList()
                streamsCursor = streamsPair?.second

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
            } catch (e: Exception) {
                _uiState.update { CategoryDetailsUiState.Error("Network error: ${e.message}") }
            }
        }
    }

    fun loadMoreStreams() {
        if (isStreamsLoading || isStreamsEnd || _uiState.value !is CategoryDetailsUiState.Success) return

        isStreamsLoading = true
        viewModelScope.launch {
            val result = repository.fetchCategoryStreams(currentSlug, streamsCursor)
            if (result.isSuccess) {
                val (newStreams, nextCursor) = result.getOrThrow()
                streamsCursor = nextCursor

                if (newStreams.isEmpty()) {
                    isStreamsEnd = true
                } else {
                    if (nextCursor.isNullOrBlank()) {
                        isStreamsEnd = true
                    }
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
            isStreamsLoading = false
        }
    }

    fun loadMoreClips() {
        if (isClipsLoading || isClipsEnd || _uiState.value !is CategoryDetailsUiState.Success) return

        isClipsLoading = true
        viewModelScope.launch {
            val result = repository.fetchCategoryClips(currentSlug, clipsCursor)
            if (result.isSuccess) {
                val (newClips, nextCursor) = result.getOrThrow()
                clipsCursor = nextCursor

                if (newClips.isEmpty()) {
                    isClipsEnd = true
                } else {
                    if (nextCursor.isNullOrBlank()) {
                        isClipsEnd = true
                    }
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
            isClipsLoading = false
        }
    }
}