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
    private var currentSlug: String? = null

    fun loadCategory(slug: String) {
        _uiState.update { CategoryDetailsUiState.Loading }

        viewModelScope.launch {
            val cleanSlug = slug.trim().lowercase()
            if (cleanSlug.isBlank()) {
                _uiState.update { CategoryDetailsUiState.Error("Error: empty slug!") }
                return@launch
            }

            currentSlug = cleanSlug
            streamsCursor = null
            clipsCursor = null
            isStreamsLoading = false
            isClipsLoading = false
            isStreamsEnd = false
            isClipsEnd = false

            try {
                val detailsDeferred = async { repository.fetchCategoryDetails(cleanSlug) }
                val clipsDeferred = async { repository.fetchCategoryClips(cleanSlug) }
                val streamsDeferred = async { repository.fetchCategoryStreams(cleanSlug) }

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
        val slug = currentSlug ?: return
        if (isStreamsLoading || isStreamsEnd || _uiState.value !is CategoryDetailsUiState.Success) {
            return
        }

        isStreamsLoading = true

        viewModelScope.launch {
            val result = repository.fetchCategoryStreams(slug, streamsCursor)

            if (result.isSuccess) {
                val (newStreams, nextCursor) = result.getOrThrow()
                streamsCursor = nextCursor

                if (newStreams.isEmpty()) {
                    isStreamsEnd = true
                } else {
                    if (nextCursor.isNullOrBlank()) {
                        isStreamsEnd = true
                    }

                    val currentState = _uiState.value as CategoryDetailsUiState.Success
                    val merged = (currentState.streams + newStreams).distinctBy { it.id }
                    _uiState.value = currentState.copy(streams = merged)
                }
            } else {
                isStreamsEnd = true
            }
            isStreamsLoading = false
        }
    }

    fun loadMoreClips() {
        val slug = currentSlug ?: return
        if (isClipsLoading || isClipsEnd || _uiState.value !is CategoryDetailsUiState.Success) {
            return
        }

        isClipsLoading = true

        viewModelScope.launch {
            val result = repository.fetchCategoryClips(slug, clipsCursor)

            if (result.isSuccess) {
                val (newClips, nextCursor) = result.getOrThrow()
                clipsCursor = nextCursor

                if (newClips.isEmpty()) {
                    isClipsEnd = true
                } else {
                    if (nextCursor.isNullOrBlank()) {
                        isClipsEnd = true
                    }

                    val currentState = _uiState.value as CategoryDetailsUiState.Success
                    val merged = (currentState.clips + newClips).distinctBy { it.id }
                    _uiState.value = currentState.copy(clips = merged)
                }
            } else {
                isClipsEnd = true
            }
            isClipsLoading = false
        }
    }
}