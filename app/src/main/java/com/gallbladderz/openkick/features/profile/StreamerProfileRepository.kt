package com.gallbladderz.openkick.features.profile

import com.gallbladderz.openkick.core.network.KickApiService
import com.gallbladderz.openkick.features.home.ClipUiModel
import com.gallbladderz.openkick.features.home.toUiModel
import com.gallbladderz.openkick.features.player.models.ChannelLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import com.gallbladderz.openkick.core.domain.DomainError
import com.gallbladderz.openkick.core.domain.toDomainError
import com.gallbladderz.openkick.features.player.models.ChannelLinkDto

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
            val profileInfo = response.toDomain(slug)
            if (profileInfo != null) {
                Result.success(profileInfo)
            } else {
                Result.failure(DomainError.ApiError("No channel ID"))
            }
        } catch (e: Exception) {
            Result.failure(e.toDomainError())
        }
    }

    suspend fun fetchChannelLinks(streamerName: String): Result<List<ChannelLink>> = withContext(Dispatchers.IO) {
        try {
            val dtos = apiService.getChannelLinks(streamerName)
            val links = dtos.map { it.toDomain() }
            Result.success(links)
        } catch (e: Exception) {
            Result.failure(e.toDomainError())
        }
    }

    suspend fun fetchVideos(slug: String): Result<List<VideoUiModel>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getChannelVideos(slug)
            val videos = response.mapNotNull { it.toDomain() }
            Result.success(videos)
        } catch (e: Exception) {
            android.util.Log.e("KICK_VODS", "АПИШКА ОПЯТЬ ЧУДИТ В fetchVideos:", e)
            Result.failure(e.toDomainError())
        }
    }

    suspend fun fetchClips(slug: String): Result<List<ClipUiModel>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getChannelClips(slug)
            val clips = response.actualClips.map { it.toUiModel() }
            Result.success(clips)
        } catch (e: Exception) {
            Result.failure(e.toDomainError())
        }
    }

    suspend fun fetchVideoPlaybackUrl(videoId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getVideoPlayback(videoId)
            val jsonObject = response as? JsonObject
                ?: return@withContext Result.failure(DomainError.ApiError("Empty response"))

            val playbackUrl = jsonObject["source"]?.jsonPrimitive?.contentOrNull
                ?: jsonObject["playback_url"]?.jsonPrimitive?.contentOrNull
                ?: jsonObject["video_url"]?.jsonPrimitive?.contentOrNull
                ?: jsonObject["url"]?.jsonPrimitive?.contentOrNull
                ?: ""

            if (playbackUrl.isBlank()) {
                Result.failure(DomainError.ApiError("No playback URL in response"))
            } else {
                Result.success(playbackUrl.replace("\\/", "/"))
            }
        } catch (e: Exception) {
            Result.failure(e.toDomainError())
        }
    }
}

fun ChannelV1Response.toDomain(slugFallback: String): ProfileInfoUi? {
    val channelId = this.id ?: return null
    val username = this.user?.username ?: slugFallback
    val bio = this.user?.bio ?: ""
    val avatarUrl = this.user?.profilePic ?: ""
    val followers = this.followersCount

    val bannerUrl = when (val banner = this.bannerImage) {
        is JsonObject -> banner["url"]?.jsonPrimitive?.content ?: ""
        is JsonPrimitive -> banner.content
        else -> ""
    }

    return ProfileInfoUi(channelId, slugFallback, username, bio, avatarUrl, bannerUrl, followers)
}

fun ChannelLinkDto.toDomain(): ChannelLink {
    return ChannelLink(
        id = this.id,
        description = this.description,
        link = this.link,
        title = this.title,
        imageUrl = this.image?.url ?: ""
    )
}

fun VideoItemDto.toDomain(): VideoUiModel? {
    if (this.is_live) return null

    val videoId = this.video?.uuid ?: this.id?.toString() ?: return null
    val videoTitle = this.sessionTitle ?: this.title ?: "Без названия"
    val thumbnailUrl = this.thumbnail?.src ?: this.thumbnail?.srcSet?.substringBefore(" ") ?: ""

    return VideoUiModel(
        id = videoId,
        title = videoTitle,
        thumbnailUrl = thumbnailUrl.replace("\\/", "/"),
        videoUrl = "",
        views = this.viewer_count,
        durationFormatted = formatVideoDuration(this.duration)
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