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
        _uiState.value = CategoriesUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            repository.fetchCategories().collect { result ->
                result.onSuccess { responseBody ->
                    parseCategoriesJson(responseBody)
                }.onFailure { exception ->
                    _uiState.value = CategoriesUiState.Error(exception.message ?: "Ошибка сети")
                }
            }
        }
    }

    private fun parseCategoriesJson(jsonString: String) {
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
                    } catch (e: Exception) {
                    }
                }
            }

            if (categoriesList.isEmpty()) {
                _uiState.value = CategoriesUiState.Error("Не удалось найти игры в ответе сервера")
            } else {
                _uiState.value = CategoriesUiState.Success(categoriesList.sortedByDescending { it.viewers })
            }
        } catch (e: Exception) {
            Log.e("OpenKick_Categories", "Крэш парсинга игр: ${e.message}", e)
            _uiState.value = CategoriesUiState.Error("Крэш парсинга: ${e.message}")
        }
    }
}