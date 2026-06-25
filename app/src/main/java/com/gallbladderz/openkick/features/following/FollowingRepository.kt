package com.gallbladderz.openkick.features.following

import android.util.Log
import com.gallbladderz.openkick.core.network.KickApiConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

class FollowingRepository(private val okHttpClient: OkHttpClient) {

    suspend fun fetchChannelDetails(slug: String): FollowedStreamerUi? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${KickApiConstants.KICK_API_BASE_URL}/channels/$slug")
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            if (!response.isSuccessful) return@withContext null

            val root = Json { ignoreUnknownKeys = true }.parseToJsonElement(body).jsonObject
            val userObj = root["user"]?.jsonObject

            val username = userObj?.get("username")?.jsonPrimitive?.content ?: slug
            val avatarUrl = userObj?.get("profile_pic")?.jsonPrimitive?.content ?: ""

            val livestreamObj = root["livestream"]?.jsonObject
            if (livestreamObj != null) {
                val title = livestreamObj["session_title"]?.jsonPrimitive?.content ?: "Untitled"
                val viewers = livestreamObj["viewer_count"]?.jsonPrimitive?.intOrNull ?: 0
                val categoryName = livestreamObj["category"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: ""

                val thumbObj = livestreamObj["thumbnail"]?.jsonObject
                val streamThumbnailUrl = thumbObj?.get("url")?.jsonPrimitive?.content
                    ?: thumbObj?.get("src")?.jsonPrimitive?.content ?: ""

                FollowedStreamerUi(slug, username, avatarUrl, true, title, viewers, categoryName, streamThumbnailUrl)
            } else {
                FollowedStreamerUi(slug, username, avatarUrl, false)
            }
        } catch (e: Exception) {
            Log.e("FollowingRepo", "Error loading channel $slug: ${e.message}")
            null
        }
    }

    suspend fun fetchCategoryDetails(slug: String): FollowedCategoryUi? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${KickApiConstants.KICK_API_BASE_URL}/subcategories/$slug")
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            if (!response.isSuccessful) return@withContext null

            val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(body).jsonObject
            val name = jsonElement["name"]?.jsonPrimitive?.content ?: slug
            val viewers = jsonElement["viewers"]?.jsonPrimitive?.intOrNull ?: 0

            var bannerUrl = jsonElement["banner"]?.jsonObject?.get("srcset")?.jsonPrimitive?.content ?: ""
            bannerUrl = bannerUrl.replace("\\/", "/")
            if (bannerUrl.contains(" ")) {
                bannerUrl = bannerUrl.split(",").firstOrNull()?.trim()?.substringBefore(" ") ?: bannerUrl
            }

            FollowedCategoryUi(slug, name, bannerUrl, viewers)
        } catch (e: Exception) {
            Log.e("FollowingRepo", "Error loading category $slug: ${e.message}")
            null
        }
    }
}