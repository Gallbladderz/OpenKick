package com.gallbladderz.openkick.features.home

import android.util.Log
import com.gallbladderz.openkick.core.network.KickApiConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request

class HomeRepository(private val client: OkHttpClient) {

    suspend fun fetchLivestreams(page: Int = 1): Result<List<StreamUiModel>> = withContext(Dispatchers.IO) {
        
        val url = if (page <= 1) {
            "${KickApiConstants.KICK_MOBILE_API_BASE_URL}/livestreams/featured?language=ru"
        } else {
            
            "${KickApiConstants.KICK_MOBILE_API_BASE_URL}/livestreams/featured?language=ru&page=$page"
        }

        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return@withContext Result.failure(Exception("Кик зажал стримы: код ${response.code} по урлу $url"))
            }
            return@withContext parseStreams(responseBody)
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Сеть отвалилась (Стримы): ${e.message}", e))
        }
    }

    suspend fun fetchTopClips(cursor: String? = null): Result<Pair<List<ClipUiModel>, String?>> = withContext(Dispatchers.IO) {
        val url = if (cursor.isNullOrEmpty()) {
            "${KickApiConstants.KICK_API_V2_BASE_URL}/clips?sort=view&time=week"
        } else {
            "${KickApiConstants.KICK_API_V2_BASE_URL}/clips?sort=view&time=week&cursor=$cursor"
        }

        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return@withContext Result.failure(Exception("Кик послал нахер: код ${response.code} (Клипы)"))
            }
            return@withContext parseClips(responseBody)
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Ошибка сети (Клипы): ${e.message}", e))
        }
    }

    private fun parseStreams(jsonString: String): Result<List<StreamUiModel>> {
        return try {
            if (jsonString.startsWith("JS_ERROR")) return Result.failure(Exception("Скрипт подавился: $jsonString"))

            val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString)
            val streamsArray = findFirstJsonArray(jsonElement)
                ?: return Result.failure(Exception("Вообще не нашли массив в JSON."))

            val uiModels = streamsArray.mapNotNull { element ->
                try {
                    val rootObj = element.jsonObject
                    val streamObj = rootObj["livestream"]?.jsonObject ?: rootObj

                    val id = streamObj["id"]?.jsonPrimitive?.content ?: rootObj["id"]?.jsonPrimitive?.content ?: "0"
                    val title = streamObj["session_title"]?.jsonPrimitive?.content ?: streamObj["title"]?.jsonPrimitive?.content ?: "Без названия"
                    val viewers = streamObj["viewer_count"]?.jsonPrimitive?.intOrNull ?: streamObj["viewers"]?.jsonPrimitive?.intOrNull ?: rootObj["viewer_count"]?.jsonPrimitive?.intOrNull ?: 0

                    val channelObj = rootObj["channel"]?.jsonObject ?: streamObj["channel"]?.jsonObject
                    val streamerName = channelObj?.get("slug")?.jsonPrimitive?.content ?: channelObj?.get("username")?.jsonPrimitive?.content ?: "Unknown"

                    val categoryObj = streamObj["category"]?.jsonObject ?: rootObj["category"]?.jsonObject
                    val categoryName = categoryObj?.get("name")?.jsonPrimitive?.content ?: "No Category"

                    val thumbnail = streamObj["thumbnail"] ?: rootObj["thumbnail"]
                    val thumbnailUrl = when (thumbnail) {
                        is JsonObject -> thumbnail["url"]?.jsonPrimitive?.content ?: thumbnail["src"]?.jsonPrimitive?.content ?: ""
                        is JsonPrimitive -> thumbnail.content
                        else -> streamObj["thumbnail_url"]?.jsonPrimitive?.content ?: ""
                    }

                    StreamUiModel(id, streamerName, title, viewers, categoryName, thumbnailUrl)
                } catch (e: Exception) {
                    null
                }
            }

            if (uiModels.isEmpty()) {
                Result.failure(Exception("Массив стримов пустой."))
            } else {
                Result.success(uiModels)
            }
        } catch (e: Exception) {
            Log.e("OpenKick_API", "Краш парсинга стримов: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseClips(jsonString: String): Result<Pair<List<ClipUiModel>, String?>> {
        return try {
            val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString)
            val rootObj = jsonElement.jsonObject

            val clipsArray = rootObj["clips"]?.jsonArray ?: rootObj["data"]?.jsonArray
            ?: return Result.failure(Exception("Не нашли массив 'clips' в ответе"))

            
            val nextCursor = rootObj["next_cursor"]?.jsonPrimitive?.content
                ?: rootObj["cursor"]?.jsonPrimitive?.content
                ?: rootObj["pagination"]?.jsonObject?.get("next_cursor")?.jsonPrimitive?.content

            val uiModels = clipsArray.mapNotNull { element ->
                try {
                    val clipObj = element.jsonObject
                    val id = clipObj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val title = clipObj["title"]?.jsonPrimitive?.content ?: "Без названия"
                    val clipUrl = clipObj["clip_url"]?.jsonPrimitive?.content ?: ""
                    val thumbnailUrl = clipObj["thumbnail_url"]?.jsonPrimitive?.content ?: ""
                    val views = clipObj["views"]?.jsonPrimitive?.intOrNull ?: 0
                    val duration = clipObj["duration"]?.jsonPrimitive?.intOrNull ?: 0

                    val minutes = duration / 60
                    val seconds = duration % 60
                    val durationStr = String.format("%02d:%02d", minutes, seconds)

                    ClipUiModel(id, title, thumbnailUrl, clipUrl, views, durationStr)
                } catch (e: Exception) {
                    null
                }
            }

            if (uiModels.isEmpty()) {
                Result.failure(Exception("Массив клипов пуст."))
            } else {
                Result.success(Pair(uiModels, if (nextCursor.isNullOrBlank()) null else nextCursor))
            }
        } catch (e: Exception) {
            Log.e("OpenKick_API", "Краш парсинга клипов: ${e.message}", e)
            Result.failure(e)
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