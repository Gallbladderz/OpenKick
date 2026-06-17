package com.gallbladderz.openkick.features.home

import android.util.Log
import com.gallbladderz.openkick.core.network.KickApiConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class HomeRepository(private val client: OkHttpClient) {

    fun fetchLivestreams(): Flow<Result<String>> = flow {
        val request = Request.Builder()
            .url("${KickApiConstants.KICK_MOBILE_API_BASE_URL}/livestreams/featured?language=ru")
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                emit(Result.failure(Exception("Кик послал нахер: код ${response.code}")))
                return@flow
            }
            emit(Result.success(responseBody))
        } catch (e: IOException) {
            emit(Result.failure(Exception("Ошибка сети: ${e.message}", e)))
        }
    }

    fun parseStreams(jsonString: String): Flow<Result<List<StreamUiModel>>> = flow {
        try {
            if (jsonString.startsWith("JS_ERROR")) {
                throw Exception("Скрипт подавился: $jsonString")
            }

            val jsonElement = try {
                Json.parseToJsonElement(jsonString)
            } catch (e: Exception) {
                throw Exception("Это не JSON. Кусок: ${jsonString.take(150)}")
            }

            if (jsonElement is JsonObject) {
                Log.d("OpenKick_API", "Ключи в корне: ${jsonElement.keys}")
            }

            val streamsArray = findFirstJsonArray(jsonElement)
                ?: throw Exception("Вообще не нашли массив в JSON. Ключи: ${(jsonElement as? JsonObject)?.keys}")

            val uiModels = streamsArray.mapNotNull { element ->
                try {
                    val rootObj = element.jsonObject

                    val streamObj = rootObj["livestream"]?.jsonObject ?: rootObj

                    val id = streamObj["id"]?.jsonPrimitive?.content
                        ?: rootObj["id"]?.jsonPrimitive?.content ?: "0"

                    val title = streamObj["session_title"]?.jsonPrimitive?.content
                        ?: streamObj["title"]?.jsonPrimitive?.content ?: "Без названия"

                    val viewers = streamObj["viewer_count"]?.jsonPrimitive?.intOrNull
                        ?: streamObj["viewers"]?.jsonPrimitive?.intOrNull
                        ?: rootObj["viewer_count"]?.jsonPrimitive?.intOrNull
                        ?: 0

                    val channelObj = rootObj["channel"]?.jsonObject ?: streamObj["channel"]?.jsonObject
                    val streamerName = channelObj?.get("slug")?.jsonPrimitive?.content
                        ?: channelObj?.get("username")?.jsonPrimitive?.content ?: "Unknown"

                    val categoryObj = streamObj["category"]?.jsonObject ?: rootObj["category"]?.jsonObject
                    val categoryName = categoryObj?.get("name")?.jsonPrimitive?.content ?: "No Category"

                    val thumbnail = streamObj["thumbnail"] ?: rootObj["thumbnail"]
                    val thumbnailUrl = when (thumbnail) {
                        is JsonObject -> thumbnail["url"]?.jsonPrimitive?.content
                            ?: thumbnail["src"]?.jsonPrimitive?.content ?: ""
                        is JsonPrimitive -> thumbnail.content
                        else -> streamObj["thumbnail_url"]?.jsonPrimitive?.content ?: ""
                    }

                    StreamUiModel(id, streamerName, title, viewers, categoryName, thumbnailUrl)
                } catch (e: Exception) {
                    null
                }
            }

            if (uiModels.isEmpty()) {
                throw Exception("Массив нашелся но он пустой или ключи внутри стримов другие.")
            }

            emit(Result.success(uiModels))

        } catch (e: Exception) {
            Log.e("OpenKick_API", "Краш парсинга: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    private fun findFirstJsonArray(element: JsonElement): JsonArray? {
        if (element is JsonArray) return element
        if (element is JsonObject) {
            for ((_, value) in element) {
                if (value is JsonArray) return value
                if (value is JsonObject) {
                    val found = findFirstJsonArray(value)
                    if (found != null) return found
                }
            }
        }
        return null
    }
}
