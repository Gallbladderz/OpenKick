package com.gallbladderz.openkick.features.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class SearchResponseDto(
    val channels: List<SearchChannelDto> = emptyList()
)

@Serializable
data class SearchChannelDto(
    val slug: String? = null,
    val is_live: Boolean? = null,
    val profile_pic: String? = null,
    val user: SearchUserDto? = null,
    val livestream: JsonElement? = null
)

@Serializable
data class SearchUserDto(
    val profile_pic: String? = null
)

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val channels: List<SearchUiModel>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

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
                    _uiState.value = SearchUiState.Error("Ошибка API: ${jsonString}")
                    return@launch
                }
                if (jsonString.contains("\"empty\":true")) {
                    clearResults()
                    return@launch
                }

                val response = jsonParser.decodeFromString<SearchResponseDto>(jsonString)

                val channels = response.channels.mapNotNull { dto ->
                    val slug = dto.slug
                    if (slug.isNullOrEmpty() || slug.contains(" ")) return@mapNotNull null

                    val isLive = dto.is_live == true || dto.livestream != null && dto.livestream.toString() != "null"
                    var pic = dto.profile_pic ?: dto.user?.profile_pic ?: ""

                    if (pic.isEmpty() || pic == "null") {
                        pic = ""
                    }
                    pic = pic.replace("\\/", "/")

                    SearchUiModel(slug, pic, isLive)
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
