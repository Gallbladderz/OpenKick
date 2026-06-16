package com.gallbladderz.openkick.features.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val channels: List<SearchUiModel>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun clearResults() {
        _uiState.value = SearchUiState.Idle
    }

    fun setLoading() {
        _uiState.value = SearchUiState.Loading
    }

    fun processJson(jsonString: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                if (jsonString.startsWith("JS_ERROR")) {
                    _uiState.value = SearchUiState.Error("Ошибка API: $jsonString")
                    return@launch
                }
                if (jsonString.contains("\"empty\":true")) {
                    clearResults()
                    return@launch
                }

                val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString)
                val channels = mutableListOf<SearchUiModel>()

                fun extractChannels(element: JsonElement) {
                    if (element is JsonArray) {
                        element.forEach { extractChannels(it) }
                    } else if (element is JsonObject) {

                        if (element.containsKey("slug")) {
                            val slug = element["slug"]?.jsonPrimitive?.content ?: ""
                            val rawString = element.toString()

                            val isLive = rawString.contains("\"is_live\":true") ||
                                    rawString.contains("\"is_live\":\"true\"") ||
                                    (rawString.contains("\"livestream\":{") && !rawString.contains("\"livestream\":null"))

                            var pic = element["profile_pic"]?.jsonPrimitive?.content
                                ?: element["user"]?.jsonObject?.get("profile_pic")?.jsonPrimitive?.content
                                ?: ""

                            if (pic.isEmpty() || pic == "null") {
                                pic = Regex("https://[^\"]+\\.(webp|png|jpg|jpeg)").find(rawString)?.value ?: ""
                            }

                            pic = pic.replace("\\/", "/")

                            if (slug.isNotEmpty() && !slug.contains(" ")) {
                                channels.add(SearchUiModel(slug, pic, isLive))
                            }
                        }
                        element.values.forEach { extractChannels(it) }
                    }
                }

                if (jsonElement is JsonObject && jsonElement.containsKey("channels")) {
                    extractChannels(jsonElement["channels"]!!)
                } else {
                    extractChannels(jsonElement)
                }

                val uniqueChannels = channels.distinctBy { it.username }

                if (uniqueChannels.isEmpty()) {
                    _uiState.value = SearchUiState.Error("Ничего не найдено")
                } else {
                    _uiState.value = SearchUiState.Success(uniqueChannels)
                }

            } catch (e: Exception) {
                Log.e("OpenKick_Search", "Крэш парсинга поиска: ${e.message}", e)
                _uiState.value = SearchUiState.Error("Крэш парсинга: ${e.message}")
            }
        }
    }
}