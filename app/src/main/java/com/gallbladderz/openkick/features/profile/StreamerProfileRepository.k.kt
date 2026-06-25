package com.gallbladderz.openkick.features.profile

import android.util.Log
import com.gallbladderz.openkick.core.network.KickApiConstants
import com.gallbladderz.openkick.features.home.ClipUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request

data class ProfileInfoUi(
    val channelId: Int,
    val slug: String,
    val username: String,
    val bio: String,
    val avatarUrl: String,
    val bannerUrl: String,
    val followers: Int
)

data class VideoUiModel(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val views: Int,
    val durationFormatted: String
)

class StreamerProfileRepository(private val client: OkHttpClient) {

    suspend fun fetchProfileInfo(slug: String): Result<ProfileInfoUi> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${KickApiConstants.KICK_API_BASE_URL}/channels/$slug")
            .build()
        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Error: ${response.code}"))

            val root = Json { ignoreUnknownKeys = true }.parseToJsonElement(body).jsonObject
            val channelId = root["id"]?.jsonPrimitive?.intOrNull ?: return@withContext Result.failure(Exception("No channel ID"))
            val userObj = root["user"]?.jsonObject

            val username = userObj?.get("username")?.jsonPrimitive?.content ?: slug
            val bio = userObj?.get("bio")?.jsonPrimitive?.content ?: ""
            val avatarUrl = userObj?.get("profile_pic")?.jsonPrimitive?.content ?: ""
            var bannerUrl = root["banner_image"]?.jsonObject?.get("url")?.jsonPrimitive?.content ?: ""
            if (bannerUrl.isBlank()) bannerUrl = root["banner_image"]?.jsonPrimitive?.content ?: ""
            val followers = root["followersCount"]?.jsonPrimitive?.intOrNull ?: 0

            Result.success(ProfileInfoUi(channelId, slug, username, bio, avatarUrl, bannerUrl, followers))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchVideos(channelId: Int): Result<List<VideoUiModel>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${KickApiConstants.KICK_MOBILE_API_BASE_URL}/channels/$channelId/videos")
            .build()
        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.success(emptyList())


            val rootArray = Json { ignoreUnknownKeys = true }.parseToJsonElement(body).jsonArray

            val videos = rootArray.mapNotNull { element ->
                try {
                    val obj = element.jsonObject


                    val isLive = obj["is_live"]?.jsonPrimitive?.booleanOrNull ?: false
                    if (isLive) return@mapNotNull null

                    val id = obj["video"]?.jsonObject?.get("uuid")?.jsonPrimitive?.content
                        ?: obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null

                    val title = obj["session_title"]?.jsonPrimitive?.content ?: "Untitled"
                    val views = obj["views"]?.jsonPrimitive?.intOrNull ?: 0
                    val videoUrl = obj["source"]?.jsonPrimitive?.content ?: ""

                    val thumbObj = obj["thumbnail"]?.jsonObject
                    val thumbUrl = thumbObj?.get("src")?.jsonPrimitive?.content ?: ""


                    val durationMs = obj["duration"]?.jsonPrimitive?.longOrNull ?: 0L
                    val totalSeconds = durationMs / 1000
                    val hours = totalSeconds / 3600
                    val minutes = (totalSeconds % 3600) / 60
                    val seconds = totalSeconds % 60

                    val durationStr = if (hours > 0) {
                        String.format("%d:%02d:%02d", hours, minutes, seconds)
                    } else {
                        String.format("%02d:%02d", minutes, seconds)
                    }

                    VideoUiModel(id, title, thumbUrl, videoUrl, views, durationStr)
                } catch (e: Exception) { null }
            }
            Result.success(videos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchClips(slug: String): Result<List<ClipUiModel>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${KickApiConstants.KICK_API_V2_BASE_URL}/channels/$slug/clips")
            .build()
        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.success(emptyList())

            val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(body)
            val clipsArray = findFirstJsonArray(jsonElement) ?: return@withContext Result.success(emptyList())

            val clips = clipsArray.mapNotNull { element ->
                try {
                    val obj = element.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val title = obj["title"]?.jsonPrimitive?.content ?: "Untitled"
                    val views = obj["views"]?.jsonPrimitive?.intOrNull ?: 0
                    val duration = obj["duration"]?.jsonPrimitive?.intOrNull ?: 0
                    val clipUrl = obj["clip_url"]?.jsonPrimitive?.content ?: ""
                    val thumbUrl = obj["thumbnail_url"]?.jsonPrimitive?.content ?: ""

                    val minutes = duration / 60
                    val seconds = duration % 60
                    val durationStr = String.format("%02d:%02d", minutes, seconds)

                    ClipUiModel(id, title, thumbUrl, clipUrl, views, durationStr)
                } catch (e: Exception) { null }
            }
            Result.success(clips)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findFirstJsonArray(element: JsonElement): JsonArray? {
        if (element is JsonArray) return element
        if (element is JsonObject) {
            for ((_, value) in element) {
                if (value is JsonArray) return value
                val found = findFirstJsonArray(value)
                if (found != null) return found
            }
        }
        return null
    }
}