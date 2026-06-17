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
import java.io.IOException

sealed interface CategoriesUiState {
    data object Loading : CategoriesUiState
    data class Success(val categories: List<CategoryUiModel>) : CategoriesUiState
    data class Error(val message: String) : CategoriesUiState
}

class CategoriesViewModel(private val client: OkHttpClient) : ViewModel() {
    private val _uiState = MutableStateFlow<CategoriesUiState>(CategoriesUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        fetchCategories()
    }

    fun fetchCategories() {
        _uiState.value = CategoriesUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://kick.com/api/v1/subcategories?limit=100")
                .addHeader("User-Agent", "KickMobile/40.21.0 (com.kick.mobile; platform: android; build:60006889)")
                .addHeader("X-App-Platform", "Android")
                .addHeader("X-App-Version", "40.21.0")
                .addHeader("X-Kick-App", "mobile")
                .addHeader("Accept", "application/json")
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody == null) {
                    _uiState.value = CategoriesUiState.Error("Ошибка API Категорий: ${response.code}")
                    return@launch
                }

                parseCategoriesJson(responseBody)
            } catch (e: IOException) {
                _uiState.value = CategoriesUiState.Error("Сеть сдохла: ${e.message}")
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
                        val viewers = obj["viewers"]?.jsonPrimitive?.intOrNull ?: 0

                        var bannerUrl = obj["banner"]?.jsonObject?.get("responsive")?.jsonPrimitive?.content ?: ""

                        bannerUrl = bannerUrl.replace("\\/", "/")

                        if (bannerUrl.contains(" ")) {
                            bannerUrl = bannerUrl.split(",").firstOrNull()?.trim()?.substringBefore(" ") ?: bannerUrl
                        }

                        if (bannerUrl.startsWith("/")) bannerUrl = "https://kick.com$bannerUrl"

                        categoriesList.add(CategoryUiModel(id, name, viewers, bannerUrl))
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