package com.gallbladderz.openkick.features.profile

import com.gallbladderz.openkick.core.network.KickApiService
import com.gallbladderz.openkick.features.home.ClipUiModel
import com.gallbladderz.openkick.features.home.toUiModel
import com.gallbladderz.openkick.features.player.models.ChannelLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import com.gallbladderz.openkick.core.domain.DomainError
import java.io.IOException

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

class StreamerProfileRepository(private val apiService: KickApiService) {

    suspend fun fetchProfileInfo(slug: String): Result<ProfileInfoUi> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getChannelV1(slug)

            val channelId = response.id ?: return@withContext Result.failure(DomainError.ApiError("No channel ID"))
            val username = response.user?.username ?: slug
            val bio = response.user?.bio ?: ""
            val avatarUrl = response.user?.profilePic ?: ""
            val followers = response.followersCount


            val bannerUrl = when (val banner = response.bannerImage) {
                is JsonObject -> banner["url"]?.jsonPrimitive?.content ?: ""
                is JsonPrimitive -> banner.content
                else -> ""
            }

            Result.success(ProfileInfoUi(channelId, slug, username, bio, avatarUrl, bannerUrl, followers))
        } catch (e: Exception) {
            Result.failure(if (e is IOException) DomainError.NetworkError() else DomainError.UnknownError(e.message ?: "Unknown error"))
        }
    }

    suspend fun fetchChannelLinks(streamerName: String): Result<List<ChannelLink>> = withContext(Dispatchers.IO) {
        try {

            val dtos = apiService.getChannelLinks(streamerName)
            val links = dtos.map { dto ->
                ChannelLink(
                    id = dto.id,
                    description = dto.description,
                    link = dto.link,
                    title = dto.title,
                    imageUrl = dto.image?.url ?: ""
                )
            }
            Result.success(links)
        } catch (e: Exception) {
            Result.failure(if (e is IOException) DomainError.NetworkError() else DomainError.UnknownError(e.message ?: "Unknown error"))
        }
    }

    suspend fun fetchVideos(channelId: Int): Result<List<VideoUiModel>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getChannelVideos(channelId)
            val videos = extractVideoItems(response)
            val uiModels = videos.mapNotNull { it.toVideoUiModel() }
            Result.success(uiModels)
        } catch (e: Exception) {
            Result.failure(if (e is IOException) DomainError.NetworkError() else DomainError.UnknownError(e.message ?: "Unknown error"))
        }
    }

    suspend fun fetchClips(slug: String): Result<List<ClipUiModel>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getChannelClips(slug)

            val uiModels = response.actualClips.map { it.toUiModel() }
            Result.success(uiModels)
        } catch (e: Exception) {
            Result.failure(if (e is IOException) DomainError.NetworkError() else DomainError.UnknownError(e.message ?: "Unknown error"))
        }
    }
}

private fun extractVideoItems(element: kotlinx.serialization.json.JsonElement): List<JsonObject> {
    return when (element) {
        is kotlinx.serialization.json.JsonArray -> element.mapNotNull { it as? JsonObject }
        is JsonObject -> {
            val directArrays = listOf("data", "videos", "items", "results")
                .mapNotNull { key -> element[key] as? kotlinx.serialization.json.JsonArray }

            if (directArrays.isNotEmpty()) {
                directArrays.first().mapNotNull { it as? JsonObject }
            } else {
                val nested = listOf("data", "body", "result")
                    .mapNotNull { key -> element[key] as? JsonObject }
                    .firstNotNullOfOrNull { nestedObject ->
                        listOf("videos", "items", "results")
                            .mapNotNull { key -> nestedObject[key] as? kotlinx.serialization.json.JsonArray }
                            .firstOrNull()
                    }

                nested?.mapNotNull { it as? JsonObject } ?: emptyList()
            }
        }
        else -> emptyList()
    }
}

private fun JsonObject.toVideoUiModel(): VideoUiModel? {
    if (boolean("is_live") == true) return null

    val nestedVideo = obj("video")
    val id = nestedVideo?.string("uuid")
        ?: string("uuid")
        ?: string("id")
        ?: numberString("id")
        ?: return null

    val title = string("session_title")
        ?: string("title")
        ?: nestedVideo?.string("session_title")
        ?: nestedVideo?.string("title")
        ?: "Untitled"

    val source = string("source")
        ?: string("playback_url")
        ?: string("video_url")
        ?: nestedVideo?.string("source")
        ?: nestedVideo?.string("playback_url")
        ?: nestedVideo?.string("video_url")
        ?: ""

    val thumbnailObject = obj("thumbnail") ?: nestedVideo?.obj("thumbnail")
    val thumbnailUrl = thumbnailObject?.string("src")
        ?: thumbnailObject?.string("url")
        ?: string("thumbnail_url")
        ?: nestedVideo?.string("thumbnail_url")
        ?: ""

    val durationValue = long("duration") ?: nestedVideo?.long("duration") ?: 0L

    return VideoUiModel(
        id = id,
        title = title,
        thumbnailUrl = thumbnailUrl.replace("\\/", "/"),
        videoUrl = source.replace("\\/", "/"),
        views = int("views") ?: int("view_count") ?: nestedVideo?.int("views") ?: 0,
        durationFormatted = formatVideoDuration(durationValue)
    )
}

private fun formatVideoDuration(rawDuration: Long): String {
    val totalSeconds = if (rawDuration > 86_400) rawDuration / 1000 else rawDuration
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.string(key: String): String? =
    this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

private fun JsonObject.numberString(key: String): String? =
    this[key]?.jsonPrimitive?.let { primitive ->
        primitive.longOrNull?.toString() ?: primitive.intOrNull?.toString()
    }

private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull

private fun JsonObject.long(key: String): Long? = this[key]?.jsonPrimitive?.longOrNull

private fun JsonObject.boolean(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull
