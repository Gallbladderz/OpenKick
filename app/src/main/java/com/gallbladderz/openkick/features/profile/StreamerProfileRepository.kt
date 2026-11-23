/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.features.profile


import com.gallbladderz.openkick.core.data.mapper.toDomain
import com.gallbladderz.openkick.core.data.mapper.toUiModel
import com.gallbladderz.openkick.core.data.repository.ClipRepository
import com.gallbladderz.openkick.core.domain.DomainError
import com.gallbladderz.openkick.core.domain.chat.ChatReplaySource
import com.gallbladderz.openkick.core.domain.model.ChannelLink
import com.gallbladderz.openkick.core.domain.model.ClipUiModel
import com.gallbladderz.openkick.core.domain.toDomainError
import com.gallbladderz.openkick.core.network.api.KickApiService
import com.gallbladderz.openkick.core.network.dto.profile.ChannelV1Response
import com.gallbladderz.openkick.core.network.dto.profile.ChannelV1User
import com.gallbladderz.openkick.core.network.dto.profile.VideoItemDto
import com.gallbladderz.openkick.core.ui.utils.formatTime
import com.gallbladderz.openkick.core.ui.utils.formatVideoDuration
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

data class ProfileInfoUi(
    val channelId: Int,
    val slug: String,
    val username: String,
    val bio: String,
    val avatarUrl: String,
    val bannerUrl: String,
    val followers: Int,
    val isLive: Boolean,
    val isVerified: Boolean,
    val liveTitle: String? = null,
    val liveCategory: String? = null,
    val liveViewers: Int? = null,
    val lastBroadcastAt: Instant? = null,
    val socialLinks: List<ProfileSocialLink> = emptyList()
)

data class VideoUiModel(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val views: Int,
    val durationFormatted: String,
    val channelSlug: String,
    val channelUsername: String,
    val channelAvatarUrl: String = "",
    val categoryName: String? = null,
    val createdAt: String? = null,
    val chatReplay: ChatReplaySource? = null,
    val broadcastEndedAt: Instant? = null
)

class StreamerProfileRepository(
    private val apiService: KickApiService,
    private val clipRepository: ClipRepository
) {

    companion object {
        private val vodCache = java.util.concurrent.ConcurrentHashMap<String, VideoUiModel>()
    }

    suspend fun fetchProfileInfo(slug: String): Result<ProfileInfoUi> =
        try {
            withContext(Dispatchers.IO) {
                val v1Deferred = async { apiService.getChannelV1(slug) }
                val v2Deferred = async {
                    try {
                        apiService.getChannelInfoV2(slug)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        null
                    }
                }

                val response = v1Deferred.await()
                val v2Response = v2Deferred.await()

                val isVerified = v2Response?.verified ?: false

                val profileInfo = response.toDomain(slug, isVerified)
                if (profileInfo != null) {
                    Result.success(profileInfo)
                } else {
                    Result.failure(DomainError.ApiError("No channel ID"))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.toDomainError())
        }

    suspend fun fetchChannelLinks(streamerName: String): Result<List<ChannelLink>> =
        withContext(Dispatchers.IO) {
            try {
                val dtos = apiService.getChannelLinks(streamerName)
                val links = dtos.map { it.toDomain() }
                Result.success(links)
            } catch (e: Exception) {
                Result.failure(e.toDomainError())
            }
        }

    suspend fun fetchVideos(slug: String): Result<List<VideoUiModel>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getChannelVideos(slug)
                val videos = response.mapNotNull { it.toDomain() }
                videos.forEach { vodCache[it.id] = it }
                Result.success(videos)
            } catch (e: Exception) {
                android.util.Log.e("KICK_VODS", "АПИШКА ОПЯТЬ ЧУДИТ В fetchVideos:", e)
                Result.failure(e.toDomainError())
            }
        }

    suspend fun fetchClips(
        slug: String,
        cursor: String? = null
    ): Result<Pair<List<ClipUiModel>, String?>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getChannelClips(slug, cursor)
            val clips = response.actualClips.map { it.toUiModel() }
            val nextCursor = response.actualCursor
            Result.success(Pair(clips, if (nextCursor.isNullOrBlank()) null else nextCursor))
        } catch (e: Exception) {
            Result.failure(e.toDomainError())
        }
    }

    suspend fun fetchVideoPlayback(
        videoId: String,
        defaultTitle: String,
        channelSlugHint: String? = null
    ): Result<VideoUiModel> =
        withContext(Dispatchers.IO) {
            try {
                val cached = vodCache[videoId]
                val response = try {
                    apiService.getVideoPlayback(videoId)
                } catch (e: HttpException) {
                    val slug = channelSlugHint ?: cached?.channelSlug?.takeIf { it.isNotBlank() }
                    if (e.code() != 404 || slug == null) throw e
                    return@withContext Result.success(fetchWebVideo(videoId, slug, defaultTitle))
                }
                val playbackUrl = sequenceOf(response.source, response.playbackUrl, response.videoUrl, response.url)
                    .filterNotNull().firstOrNull { it.isNotBlank() }
                    ?: return@withContext Result.failure(DomainError.ApiError("No playback URL in response"))
                val stream = response.livestream
                val channelSlug = cached?.channelSlug?.takeIf { it.isNotBlank() }
                    ?: stream?.channel?.slug.orEmpty()
                Result.success(
                    VideoUiModel(
                        id = videoId,
                        title = stream?.sessionTitle?.takeIf { it.isNotBlank() }
                            ?: stream?.title?.takeIf { it.isNotBlank() }
                            ?: cached?.title ?: defaultTitle,
                        thumbnailUrl = stream?.thumbnail.videoThumbnailUrl()
                            .ifBlank { cached?.thumbnailUrl.orEmpty() },
                        videoUrl = playbackUrl.replace("\\/", "/"),
                        views = response.views ?: cached?.views ?: stream?.viewer_count ?: 0,
                        durationFormatted = stream?.duration?.takeIf { it > 0 }?.let(::formatTime)
                            ?: cached?.durationFormatted ?: formatTime(0),
                        channelSlug = channelSlug,
                        channelUsername = cached?.channelUsername?.takeIf { it.isNotBlank() }
                            ?: stream?.channel?.user?.username ?: stream?.channel?.username ?: channelSlug,
                        channelAvatarUrl = stream?.channel?.avatar.orEmpty().replace("\\/", "/")
                            .ifBlank { cached?.channelAvatarUrl.orEmpty() },
                        categoryName = stream?.categoryName() ?: cached?.categoryName,
                        createdAt = stream?.start_time ?: cached?.createdAt,
                        chatReplay = ChatReplaySource.from(
                            stream?.channelId ?: stream?.channel?.id?.toLong(),
                            stream?.start_time
                        ) ?: cached?.chatReplay
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e.toDomainError())
            }
        }

    private suspend fun fetchWebVideo(videoId: String, slug: String, defaultTitle: String): VideoUiModel {
        val profile = apiService.getChannelV1(slug)
        val channelId = profile.id ?: throw DomainError.ApiError("No channel ID")
        val video = apiService.getWebVideoPlayback(channelId, videoId).data
        val recordingUrl = video.recordingUrl?.takeIf { it.isNotBlank() }
            ?: throw DomainError.ApiError("No playback URL in response")
        return VideoUiModel(
            id = videoId,
            title = video.title?.takeIf { it.isNotBlank() } ?: defaultTitle,
            thumbnailUrl = video.thumbnail.videoThumbnailUrl(),
            videoUrl = recordingUrl,
            views = video.viewerCount,
            durationFormatted = formatTime(video.duration * 1_000),
            channelSlug = video.channel?.slug?.ifBlank { slug } ?: slug,
            channelUsername = video.channel?.username ?: profile.user?.username ?: slug,
            channelAvatarUrl = video.channel?.avatar.orEmpty().ifBlank { profile.user?.profilePic.orEmpty() },
            categoryName = video.category?.name,
            createdAt = video.startTime,
            chatReplay = ChatReplaySource.from(channelId.toLong(), video.startTime)
        )
    }
}

fun ChannelV1Response.toDomain(slugFallback: String, isVerified: Boolean): ProfileInfoUi? {
    val channelId = this.id ?: return null
    val username = user?.username?.takeIf { it.isNotBlank() } ?: slugFallback
    val bio = user?.bio?.trim().orEmpty()
    val avatarUrl = this.user?.profilePic ?: ""
    val followers = this.followersCount

    val bannerUrl = when (val banner = this.bannerImage) {
        is JsonObject -> banner["url"]?.jsonPrimitive?.content ?: ""
        is JsonPrimitive -> banner.content
        else -> ""
    }
    val live = livestream
    val lastBroadcastAt = if (live == null) previousLivestreams.orEmpty()
        .asSequence()
        .filterNot { it.isLive }
        .mapNotNull { broadcastEndTime(it.startedAt, it.createdAt, it.endedAt, it.duration ?: 0) }
        .maxOrNull()
    else null

    return ProfileInfoUi(
        channelId = channelId,
        slug = slugFallback,
        username = username,
        bio = bio,
        avatarUrl = avatarUrl,
        bannerUrl = bannerUrl,
        followers = followers,
        isLive = live != null,
        isVerified = isVerified || this.isVerified,
        liveTitle = live?.sessionTitle?.takeIf { it.isNotBlank() },
        liveCategory = live?.categories.orEmpty().firstNotNullOfOrNull { it.name?.takeIf(String::isNotBlank) }
            ?: live?.category?.name?.takeIf(String::isNotBlank),
        liveViewers = live?.viewerCount?.takeIf { it >= 0 },
        lastBroadcastAt = lastBroadcastAt,
        socialLinks = user?.socialLinks().orEmpty()
    )
}

private fun ChannelV1User.socialLinks(): List<ProfileSocialLink> = listOf(
    SocialPlatform.YouTube to youtube,
    SocialPlatform.Instagram to instagram,
    SocialPlatform.X to twitter,
    SocialPlatform.TikTok to tiktok,
    SocialPlatform.Discord to discord,
    SocialPlatform.Facebook to facebook,
    SocialPlatform.Website to website
).mapNotNull { (platform, value) ->
    normalizeSocialUrl(platform, value)?.let { ProfileSocialLink(platform, it) }
}

fun VideoItemDto.toDomain(): VideoUiModel? {
    if (this.is_live) return null

    val videoId = this.video?.uuid ?: this.id?.toString() ?: return null
    val videoTitle = this.sessionTitle ?: this.title ?: "Без названия"
    val channelSlug = this.channel?.slug ?: ""
    val channelUsername = this.channel?.username ?: this.channel?.user?.username ?: channelSlug

    return VideoUiModel(
        id = videoId,
        title = videoTitle,
        thumbnailUrl = thumbnail.videoThumbnailUrl(),
        videoUrl = "",
        views = this.viewer_count,
        durationFormatted = formatVideoDuration(this.duration),
        channelSlug = channelSlug,
        channelUsername = channelUsername,
        channelAvatarUrl = channel?.avatar.orEmpty().replace("\\/", "/"),
        categoryName = categoryName(),
        createdAt = start_time,
        chatReplay = ChatReplaySource.from(channelId ?: channel?.id?.toLong(), start_time),
        broadcastEndedAt = broadcastEndTime(start_time, createdAt, end_time, duration)
    )
}

private fun VideoItemDto.categoryName(): String? =
    categories.orEmpty().firstNotNullOfOrNull { it.name?.takeIf(String::isNotBlank) }
        ?: category?.name?.takeIf(String::isNotBlank)

private fun JsonElement?.videoThumbnailUrl(): String {
    val url = when (this) {
        is JsonPrimitive -> thumbnailString()
        is JsonObject -> this["src"].thumbnailString()
            ?: this["url"].thumbnailString()
            ?: this["srcSet"].thumbnailString()?.substringBefore(' ')
        else -> null
    }
    return url.orEmpty().replace("\\/", "/")
}

private fun JsonElement?.thumbnailString(): String? =
    (this as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }
