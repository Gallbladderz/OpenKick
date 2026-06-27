package com.gallbladderz.openkick.features.profile

import com.gallbladderz.openkick.core.network.KickApiService
import com.gallbladderz.openkick.features.home.ClipUiModel
import com.gallbladderz.openkick.features.home.toUiModel
import com.gallbladderz.openkick.features.player.models.ChannelLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

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

            val channelId = response.id ?: return@withContext Result.failure(Exception("No channel ID"))
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
            Result.failure(e)
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
            Result.failure(e)
        }
    }

    suspend fun fetchVideos(channelId: Int): Result<List<VideoUiModel>> = withContext(Dispatchers.IO) {
        try {
            val videos = apiService.getChannelVideos(channelId)
            val uiModels = videos.filter { !it.isLive }.mapNotNull { dto ->
                val id = dto.actualId
                if (id.isEmpty()) return@mapNotNull null

                val totalSeconds = dto.duration / 1000
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60

                val durationStr = if (hours > 0) {
                    String.format("%d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format("%02d:%02d", minutes, seconds)
                }

                VideoUiModel(
                    id = id,
                    title = dto.sessionTitle ?: "Untitled",
                    thumbnailUrl = dto.thumbnail?.src ?: "",
                    videoUrl = dto.source ?: "",
                    views = dto.views,
                    durationFormatted = durationStr
                )
            }
            Result.success(uiModels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchClips(slug: String): Result<List<ClipUiModel>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getChannelClips(slug)
            
            val uiModels = response.actualClips.map { it.toUiModel() }
            Result.success(uiModels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}