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

    fun loadCategory(slug: String) {
        _uiState.update { CategoryDetailsUiState.Loading }

        viewModelScope.launch {
            val cleanSlug = slug.trim().lowercase()
            if (cleanSlug.isBlank()) {
                _uiState.update { CategoryDetailsUiState.Error("Error: empty slug!") }
                return@launch
            }

            try {
                val detailsDeferred = async { repository.fetchCategoryDetails(cleanSlug) }
                val clipsDeferred = async { repository.fetchCategoryClips(cleanSlug) }
                val streamsDeferred = async { repository.fetchCategoryStreams(cleanSlug) }

                val details = detailsDeferred.await()
                val parsedClips = clipsDeferred.await()
                val parsedStreams = streamsDeferred.await()

                var bannerUrl = details.banner?.srcset ?: ""
                if (bannerUrl.contains(" ")) {
                    bannerUrl = bannerUrl.split(",").firstOrNull()?.trim()?.substringBefore(" ") ?: bannerUrl
                }

                _uiState.update {
                    CategoryDetailsUiState.Success(
                        name = details.name,
                        bannerUrl = bannerUrl,
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
}