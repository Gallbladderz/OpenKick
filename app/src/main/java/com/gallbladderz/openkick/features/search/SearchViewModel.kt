package com.gallbladderz.openkick.features.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val channels: List<SearchUiModel>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel(private val client: OkHttpClient) : ViewModel() {
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun searchStreamer(query: String) {
        if (query.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        _uiState.value = SearchUiState.Loading
        searchJob?.cancel()

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://search.kick.com/api/v1/search/enriched?query=${android.net.Uri.encode(query)}")
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
                    _uiState.value = SearchUiState.Error("Поиск упал: ${response.code}")
                    return@launch
                }

                parseSearchJson(responseBody)
            } catch (e: IOException) {
                _uiState.value = SearchUiState.Error("Ошибка сети")
            }
        }
    }

    private fun parseSearchJson(jsonString: String) {
        try {
            val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString)
            val channelsList = mutableListOf<SearchUiModel>()

            val dataObj = jsonElement.jsonObject["data"]?.jsonObject
            val channelsArray = dataObj?.get("channels")?.jsonArray

            if (channelsArray != null) {
                for (element in channelsArray) {
                    val obj = element.jsonObject
                    val slug = obj["slug"]?.jsonPrimitive?.content ?: continue

                    val isLive = obj["is_live"]?.jsonPrimitive?.booleanOrNull == true ||
                            (obj.containsKey("livestream") && obj["livestream"] !is JsonNull)

                    var pic = obj["profile_pic"]?.jsonPrimitive?.content ?: ""
                    pic = pic.replace("\\/", "/")

                    channelsList.add(SearchUiModel(slug, pic, isLive))
                }
            }

            if (channelsList.isEmpty()) {
                _uiState.value = SearchUiState.Error("Ничего не найдено")
            } else {
                _uiState.value = SearchUiState.Success(channelsList)
            }
        } catch (e: Exception) {
            Log.e("OpenKick_Search", "Ошибка парсинга: ${e.message}")
            _uiState.value = SearchUiState.Error("Ошибка обработки данных")
        }
    }
}