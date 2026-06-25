package com.gallbladderz.openkick.features.player

import com.gallbladderz.openkick.core.network.KickApiConstants
import com.gallbladderz.openkick.features.player.models.ChannelLink
import com.gallbladderz.openkick.features.player.models.StreamInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class PlayerRepository(
    private val client: OkHttpClient
) {

    fun fetchStreamInfo(streamerName: String): Flow<Result<StreamInfo>> = flow {
        val request = Request.Builder()
            .url("${KickApiConstants.KICK_API_V2_BASE_URL}/channels/$streamerName")
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                emit(Result.failure(Exception("Kick rejected request: code ${response.code}")))
                return@flow
            }

            emit(parseJson(responseBody))
        } catch (e: IOException) {
            emit(Result.failure(e))
        }
    }

    suspend fun fetchChannelLinks(
        streamerName: String
    ): Result<List<ChannelLink>> = withContext(Dispatchers.IO) {

        val request = Request.Builder()
            .url("${KickApiConstants.KICK_API_BASE_URL}/channels/$streamerName/links")
            .build()

        try {
            val response = client.newCall(request).execute()

            val body = response.body?.string()
                ?: return@withContext Result.failure(
                    Exception("Kick hid the info")
                )

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Error code: ${response.code}")
                )
            }

            val jsonArray = Json {
                ignoreUnknownKeys = true
            }.parseToJsonElement(body).jsonArray

            val links = jsonArray.mapNotNull { element ->
                try {
                    val obj = element.jsonObject

                    ChannelLink(
                        id = obj["id"]?.jsonPrimitive?.intOrNull ?: 0,
                        description = obj["description"]?.jsonPrimitive?.content ?: "",
                        link = obj["link"]?.jsonPrimitive?.content ?: "",
                        title = obj["title"]?.jsonPrimitive?.content ?: "",
                        imageUrl = obj["image"]
                            ?.jsonObject
                            ?.get("url")
                            ?.jsonPrimitive
                            ?.content
                            ?: ""
                    )
                } catch (_: Exception) {
                    null
                }
            }

            Result.success(links)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseJson(jsonString: String): Result<StreamInfo> {
        return try {
            val jsonElement = Json {
                ignoreUnknownKeys = true
            }.parseToJsonElement(jsonString)

            if (jsonElement is JsonObject) {
                val livestreamObj = jsonElement["livestream"]?.jsonObject
                val userObj = jsonElement["user"]?.jsonObject

                val url = jsonElement["playback_url"]?.jsonPrimitive?.content
                    ?: livestreamObj?.get("playback_url")?.jsonPrimitive?.content

                val chatroomId = jsonElement["chatroom"]?.jsonObject
                    ?.get("id")
                    ?.jsonPrimitive
                    ?.content
                    ?: jsonElement["chatroom_id"]?.jsonPrimitive?.content

                var avatar = userObj?.get("profile_pic")?.jsonPrimitive?.content ?: ""
                avatar = avatar.replace("\\/", "/")

                val viewers =
                    livestreamObj?.get("viewer_count")?.jsonPrimitive?.intOrNull ?: 0

                val title =
                    livestreamObj?.get("session_title")?.jsonPrimitive?.content
                        ?: "Stream"

                if (!url.isNullOrEmpty()) {
                    Result.success(
                        StreamInfo(
                            playbackUrl = url,
                            avatarUrl = avatar,
                            viewers = viewers,
                            title = title,
                            chatroomId = chatroomId
                        )
                    )
                } else {
                    Result.failure(Exception("Streamer is currently offline"))
                }
            } else {
                Result.failure(Exception("Received non-JSON object"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("API response processing error"))
        }
    }
}