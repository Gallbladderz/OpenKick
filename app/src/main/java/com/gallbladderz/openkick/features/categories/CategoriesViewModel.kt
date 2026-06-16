package com.gallbladderz.openkick.features.categories

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class CategoriesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<CategoriesUiState>(CategoriesUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun processJson(jsonString: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                if (jsonString.startsWith("JS_ERROR")) {
                    _uiState.value = CategoriesUiState.Error("Ошибка API: $jsonString")
                    return@launch
                }

                val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString)

                val categoriesArray = findCategoriesArray(jsonElement)
                    ?: throw Exception("В JSON нет массива категорий")

                val uiModels = categoriesArray.mapNotNull { element ->
                    try {
                        val obj = element.jsonObject
                        val id = obj["id"]?.jsonPrimitive?.content ?: "0"
                        val name = obj["name"]?.jsonPrimitive?.content ?: "Без названия"
                        val viewers = obj["viewers"]?.jsonPrimitive?.intOrNull ?: 0

                        val bannerElement = obj["banner"]
                        var bannerUrl = when {
                            bannerElement is JsonObject -> {
                                bannerElement["responsive"]?.jsonPrimitive?.content
                                    ?: bannerElement["url"]?.jsonPrimitive?.content
                                    ?: bannerElement["src"]?.jsonPrimitive?.content
                                    ?: ""
                            }
                            bannerElement?.jsonPrimitive?.isString == true -> {
                                bannerElement.jsonPrimitive.content
                            }
                            else -> {
                                val imgObj = obj["image"]?.jsonObject
                                imgObj?.get("url")?.jsonPrimitive?.content
                                    ?: imgObj?.get("src")?.jsonPrimitive?.content
                                    ?: ""
                            }
                        }

                        if (bannerUrl.contains(" ")) {
                            bannerUrl = bannerUrl.split(",").firstOrNull()?.trim()?.substringBefore(" ") ?: bannerUrl
                        }

                        if (bannerUrl.isEmpty()) {
                            val rawString = obj.toString()
                            bannerUrl = Regex("https://[^\"]+\\.(webp|png|jpg|jpeg)").find(rawString)?.value ?: ""
                        }

                        if (bannerUrl.startsWith("/")) {
                            bannerUrl = "https://kick.com$bannerUrl"
                        } else if (bannerUrl.isNotEmpty() && !bannerUrl.startsWith("http")) {
                            bannerUrl = "https://$bannerUrl"
                        }

                        Log.d("OpenKick_Image", "Категория: $name | чистый урл: $bannerUrl")

                        CategoryUiModel(id, name, viewers, bannerUrl)
                    } catch (e: Exception) {
                        null
                    }
                }
                    .filter { it.bannerUrl.isNotEmpty() }
                    .sortedByDescending { it.viewers }

                if (uiModels.isEmpty()) {
                    _uiState.value = CategoriesUiState.Error("Массив найден но он пустой или без картинок")
                } else {
                    _uiState.value = CategoriesUiState.Success(uiModels)
                }

            } catch (e: Exception) {
                Log.e("OpenKick_Categories", "Краш парсинга: ${e.message}", e)
                _uiState.value = CategoriesUiState.Error("Краш парсинга: ${e.message}")
            }
        }
    }

    private fun findCategoriesArray(element: JsonElement): JsonArray? {
        if (element is JsonArray && isCategoriesArray(element)) return element
        if (element is JsonObject) {
            val data = element["data"]
            if (data is JsonArray && isCategoriesArray(data)) return data

            for ((_, value) in element) {
                if (value is JsonArray && isCategoriesArray(value)) return value
                if (value is JsonObject) {
                    val found = findCategoriesArray(value)
                    if (found != null) return found
                }
            }
        }
        return null
    }

    private fun isCategoriesArray(array: JsonArray): Boolean {
        val first = array.firstOrNull()?.jsonObject ?: return false
        return first.containsKey("name") && (first.containsKey("banner") || first.containsKey("viewers") || first.containsKey("id"))
    }
}