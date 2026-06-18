package com.gallbladderz.openkick.features.categories

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request

sealed interface CategoryDetailsUiState {
    data object Loading : CategoryDetailsUiState
    data class Success(
        val name: String,
        val bannerUrl: String,
        val viewers: Int,
        val tags: List<String>
    ) : CategoryDetailsUiState
    data class Error(val message: String) : CategoryDetailsUiState
}

class CategoryDetailsViewModel(
    private val okHttpClient: OkHttpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryDetailsUiState>(CategoryDetailsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun loadCategory(slug: String) {
        _uiState.value = CategoryDetailsUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {

            Log.d("OpenKick_Category", "Пытаемся загрузить слаг: '$slug'")

            if (slug.isBlank()) {
                _uiState.value = CategoryDetailsUiState.Error("Ошибка: slug пустой!")
                return@launch
            }

            val cleanSlug = slug.trim().lowercase()

            val request = Request.Builder()
                .url("https://kick.com/api/v1/subcategories/$cleanSlug")
                .build()

            try {
                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string()

                Log.d("OpenKick_Category", "Код: ${response.code} | Ответ: ${body?.take(200)}")

                if (!response.isSuccessful || body == null) {
                    _uiState.value = CategoryDetailsUiState.Error("Ошибка загрузки: ${response.code}")
                    return@launch
                }

                parseCategoryJson(body)
            } catch (e: Exception) {
                _uiState.value = CategoryDetailsUiState.Error("Ошибка сети: ${e.message}")
            }
        }
    }

    private fun parseCategoryJson(jsonString: String) {
        try {
            val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString).jsonObject

            val name = jsonElement["name"]?.jsonPrimitive?.content ?: "Категория"
            val viewers = jsonElement["viewers"]?.jsonPrimitive?.intOrNull ?: 0

            val tagsArray = jsonElement["tags"]?.jsonArray
            val tags = tagsArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()

            var bannerUrl = jsonElement["banner"]?.jsonObject?.get("srcset")?.jsonPrimitive?.content ?: ""
            bannerUrl = bannerUrl.replace("\\/", "/")
            if (bannerUrl.contains(" ")) {
                bannerUrl = bannerUrl.split(",").firstOrNull()?.trim()?.substringBefore(" ") ?: bannerUrl
            }

            _uiState.value = CategoryDetailsUiState.Success(name, bannerUrl, viewers, tags)
        } catch (e: Exception) {
            _uiState.value = CategoryDetailsUiState.Error("Ошибка парсинга JSON")
        }
    }
}