package com.gallbladderz.openkick.features.categories

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.data.local.FollowsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

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
        _uiState.value = CategoriesUiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            repository.fetchCategories(currentPage).collect { result ->
                result.onSuccess { responseBody ->
                    parseCategoriesJson(responseBody, isAppend = false)
                }.onFailure { exception ->
                    _uiState.value = CategoriesUiState.Error(exception.message ?: "Ошибка сети")
                }
            }
        }
    }

    fun loadMoreCategories() {
        if (isLoadingMore || isLastPage || _uiState.value !is CategoriesUiState.Success) return
        isLoadingMore = true
        currentPage++

        viewModelScope.launch(Dispatchers.IO) {
            repository.fetchCategories(currentPage).collect { result ->
                result.onSuccess { responseBody ->
                    parseCategoriesJson(responseBody, isAppend = true)
                }.onFailure {
                    isLoadingMore = false
                }
            }
        }
    }

    private fun parseCategoriesJson(jsonString: String, isAppend: Boolean) {
        try {
            val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString)
            val categoriesList = mutableListOf<CategoryUiModel>()

            val dataArray = jsonElement.jsonObject["data"]?.jsonArray

            if (dataArray != null) {
                for (element in dataArray) {
                    try {
                        val obj = element.jsonObject
                        val id = obj["id"]?.jsonPrimitive?.content ?: "0"
                        val name = obj["name"]?.jsonPrimitive?.content ?: "Без названия"
                        val slug = obj["slug"]?.jsonPrimitive?.content ?: ""
                        val viewers = obj["viewers"]?.jsonPrimitive?.intOrNull ?: 0

                        val tagsArray = obj["tags"]?.jsonArray
                        val tags = tagsArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()

                        var bannerUrl = obj["banner"]?.jsonObject?.get("responsive")?.jsonPrimitive?.content ?: ""
                        bannerUrl = bannerUrl.replace("\\/", "/")
                        if (bannerUrl.contains(" ")) {
                            bannerUrl = bannerUrl.split(",").firstOrNull()?.trim()?.substringBefore(" ") ?: bannerUrl
                        }
                        if (bannerUrl.startsWith("/")) bannerUrl = "https://kick.com$bannerUrl"

                        categoriesList.add(CategoryUiModel(id, name, slug, viewers, bannerUrl, tags))
                    } catch (e: Exception) {}
                }
            }

            if (categoriesList.isEmpty()) {
                if (!isAppend) {
                    _uiState.value = CategoriesUiState.Error("Не удалось найти игры")
                }
                isLastPage = true
            } else {
                val currentList = if (isAppend && _uiState.value is CategoriesUiState.Success) {
                    (_uiState.value as CategoriesUiState.Success).categories
                } else emptyList()

                
                val newList = (currentList + categoriesList.sortedByDescending { it.viewers }).distinctBy { it.id }
                _uiState.value = CategoriesUiState.Success(newList)
            }
            isLoadingMore = false
        } catch (e: Exception) {
            Log.e("OpenKick_Categories", "Крэш парсинга игр: ${e.message}", e)
            if (!isAppend) _uiState.value = CategoriesUiState.Error("Крэш парсинга: ${e.message}")
            isLoadingMore = false
        }
    }
}