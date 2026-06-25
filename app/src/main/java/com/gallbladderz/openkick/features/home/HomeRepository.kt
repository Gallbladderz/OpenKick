package com.gallbladderz.openkick.features.home

import android.util.Log
import com.gallbladderz.openkick.core.network.KickApiConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request

class HomeRepository(private val client: OkHttpClient) {

    suspend fun fetchLivestreams(
        cursor: String? = null,
        languages: Set<String> = emptySet()
    ): Result<Pair<List<StreamUiModel>, String?>> = withContext(Dispatchers.IO) {

        val langQuery = languages.joinToString("") {
            "&language=$it"
        }

        val baseUrl =
            "${KickApiConstants.KICK_MOBILE_API_BASE_URL}/livestreams?limit=24&sort=viewer_count_desc"

        val url = if (cursor.isNullOrEmpty()) {
            "$baseUrl$langQuery"
        } else {
            "$baseUrl&after=$cursor$langQuery"
        }

        Log.d("CURSOR_URL", url)

        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()

            Log.d("STREAMS_PAGINATION", "cursor=$cursor")

            val responseBody = response.body?.string()
            Log.d("RAW_RESPONSE_LENGTH", responseBody?.length.toString())
            responseBody?.takeLast(2000)?.let {
                Log.d("RAW_RESPONSE_END", it)
            }

            if (!response.isSuccessful || responseBody == null) {
                return@withContext Result.failure(
                    Exception("Kick hid streams: code ${response.code} for url $url")
                )
            }

            val result = parseStreams(responseBody)

            result.onSuccess { (uiModels, nextCursor) ->
                Log.d(
                    "STREAM_IDS",
                    "cursor=$cursor nextCursor=$nextCursor firstIds=${uiModels.take(10).map { it.id }}"
                )
            }

            return@withContext result

        } catch (e: Exception) {
            return@withContext Result.failure(
                Exception("Network dropped (Streams): ${e.message}", e)
            )
        }
    }

    suspend fun fetchTopClips(cursor: String? = null): Result<Pair<List<ClipUiModel>, String?>> = withContext(Dispatchers.IO) {
        val url = if (cursor.isNullOrEmpty()) {
            "${KickApiConstants.KICK_API_V2_BASE_URL}/clips?sort=view&time=week"
        } else {
            "${KickApiConstants.KICK_API_V2_BASE_URL}/clips?sort=view&time=week&cursor=$cursor"
        }


        Log.d("CURSOR_URL", url)

        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return@withContext Result.failure(Exception("Kick rejected: code ${response.code} (Clips)"))
            }
            return@withContext parseClips(responseBody)
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Network error (Clips): ${e.message}", e))
        }
    }

    private fun parseStreams(jsonString: String): Result<Pair<List<StreamUiModel>, String?>> {
        return try {
            if (jsonString.startsWith("JS_ERROR")) return Result.failure(Exception("Script choked: $jsonString"))

            val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString)
            val rootObj = jsonElement.jsonObject

            val nextCursor = rootObj["data"]
                ?.jsonObject
                ?.get("pagination")
                ?.jsonObject
                ?.get("next_cursor")
                ?.jsonPrimitive
                ?.content

            Log.d("CURSOR_TEST", "parsedNextCursor=$nextCursor")

            val streamsArray = rootObj["data"]
                ?.jsonObject
                ?.get("livestreams")
                ?.jsonArray
                ?: return Result.failure(Exception("data.livestreams not found"))

            val uiModels = streamsArray.mapNotNull { element ->
                try {
                    val streamObjRoot = element.jsonObject
                    val streamObj = streamObjRoot["livestream"]?.jsonObject ?: streamObjRoot

                    val id = streamObj["id"]?.jsonPrimitive?.content ?: streamObjRoot["id"]?.jsonPrimitive?.content ?: "0"
                    val title = streamObj["session_title"]?.jsonPrimitive?.content ?: streamObj["title"]?.jsonPrimitive?.content ?: "Untitled"
                    val viewers = streamObj["viewer_count"]?.jsonPrimitive?.intOrNull ?: streamObj["viewers"]?.jsonPrimitive?.intOrNull ?: streamObjRoot["viewer_count"]?.jsonPrimitive?.intOrNull ?: 0

                    val channelObj = streamObjRoot["channel"]?.jsonObject ?: streamObj["channel"]?.jsonObject
                    val streamerName = channelObj?.get("slug")?.jsonPrimitive?.content ?: channelObj?.get("username")?.jsonPrimitive?.content ?: "Unknown"

                    val categoryObj = streamObj["category"]?.jsonObject ?: streamObjRoot["category"]?.jsonObject
                    val categoryName = categoryObj?.get("name")?.jsonPrimitive?.content ?: "No Category"

                    val thumbnail = streamObj["thumbnail"] ?: streamObjRoot["thumbnail"]
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
                Result.success(Pair(emptyList(), null))
            } else {
                Result.success(Pair(uiModels, if (nextCursor.isNullOrBlank()) null else nextCursor))
            }
        } catch (e: Exception) {
            Log.e("OpenKick_API", "Stream parsing crash: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseClips(jsonString: String): Result<Pair<List<ClipUiModel>, String?>> {
        return try {
            val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString)
            val rootObj = jsonElement.jsonObject

            val clipsArray = rootObj["clips"]?.jsonArray ?: rootObj["data"]?.jsonArray
            ?: return Result.failure(Exception("'clips' array not found in response"))

            val nextCursor = rootObj["next_cursor"]?.jsonPrimitive?.content
                ?: rootObj["cursor"]?.jsonPrimitive?.content
                ?: rootObj["pagination"]?.jsonObject?.get("next_cursor")?.jsonPrimitive?.content

            val uiModels = clipsArray.mapNotNull { element ->
                try {
                    val clipObj = element.jsonObject
                    val id = clipObj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val title = clipObj["title"]?.jsonPrimitive?.content ?: "Untitled"
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
                return Result.success(
                    Pair(
                        emptyList<ClipUiModel>(),
                        null
                    )
                )
            } else {
                return Result.success(
                    Pair(
                        uiModels,
                        if (nextCursor.isNullOrBlank()) null else nextCursor
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("OpenKick_API", "Clip parsing crash: ${e.message}", e)
            Result.failure(e)
        }
    }
}