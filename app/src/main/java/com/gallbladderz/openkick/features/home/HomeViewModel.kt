package com.gallbladderz.openkick.features.home

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

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val streams: List<StreamUiModel>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(private val client: OkHttpClient = OkHttpClient()) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        fetchLivestreams()
    }

    fun fetchLivestreams() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://mobile.kick.com/api/v1/livestreams/featured?language=ru")
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
                    _uiState.value = HomeUiState.Error("Кик послал нахер: код ${response.code}")
                    return@launch
                }

                parseJson(responseBody)
            } catch (e: IOException) {
                Log.e("OpenKick_Home", "Интернет отвалился", e)
                _uiState.value = HomeUiState.Error("Ошибка сети: ${e.message}")
            }
        }
    }

    private fun parseJson(jsonString: String) {
        try {
            val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString)
            val livestreams = mutableListOf<StreamUiModel>()

            val streamsArray = jsonElement.jsonObject["data"]?.jsonObject?.get("livestreams")?.jsonArray

            if (streamsArray == null) {
                _uiState.value = HomeUiState.Error("Массив livestreams не найден в мобильном API!")
                return
            }

            for (element in streamsArray) {
                try {
                    val obj = element.jsonObject
                    val channel = obj["channel"]?.jsonObject
                    val category = obj["category"]?.jsonObject
                    val thumbnail = obj["thumbnail"]?.jsonObject

                    val id = obj["id"]?.jsonPrimitive?.content ?: "0"

                    // Название стрима (часто в мобильном апи это session_title)
                    val title = obj["session_title"]?.jsonPrimitive?.content
                        ?: obj["title"]?.jsonPrimitive?.content ?: "Без названия"

                    val streamerName = channel?.get("slug")?.jsonPrimitive?.content ?: "Аноним"

                    val viewers = obj["viewer_count"]?.jsonPrimitive?.intOrNull
                        ?: obj["viewers"]?.jsonPrimitive?.intOrNull ?: 0

                    val categoryName = category?.get("name")?.jsonPrimitive?.content ?: "Разговорные"

                    var thumbUrl = thumbnail?.get("src")?.jsonPrimitive?.content
                        ?: thumbnail?.get("url")?.jsonPrimitive?.content ?: ""

                    if (thumbUrl.startsWith("/")) thumbUrl = "https://kick.com$thumbUrl"

                    livestreams.add(
                        StreamUiModel(
                            id = id,
                            title = title,
                            streamerName = streamerName,
                            viewers = viewers,
                            category = categoryName,
                            thumbnailUrl = thumbUrl
                        )
                    )
                } catch (e: Exception) {
                }
            }

            if (livestreams.isEmpty()) {
                _uiState.value = HomeUiState.Error("Никого нет онлайн (или парсер ослеп)")
            } else {
                _uiState.value = HomeUiState.Success(livestreams)
            }

        } catch (e: Exception) {
            Log.e("OpenKick_Home", "Крэш парсинга: ${e.message}", e)
            _uiState.value = HomeUiState.Error("Крэш парсинга: ${e.message}")
        }
    }
}