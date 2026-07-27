package com.gallbladderz.openkick.features.player

import com.gallbladderz.openkick.core.domain.DomainError
import com.gallbladderz.openkick.core.domain.toDomainError
import com.gallbladderz.openkick.core.network.KickApiService
import com.gallbladderz.openkick.features.player.models.ChannelLink
import com.gallbladderz.openkick.features.player.models.ChannelLinkDto
import com.gallbladderz.openkick.features.player.models.ChannelStreamInfoResponse
import com.gallbladderz.openkick.features.player.models.StreamInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class PlayerRepository(
    private val apiService: KickApiService
) {
    fun fetchStreamInfo(streamerName: String): Flow<Result<StreamInfo>> = flow {
        try {
            val response = apiService.getChannelStreamInfo(streamerName)
            val streamInfo = response.toDomain()
            if (streamInfo != null) {
                emit(Result.success(streamInfo))
            } else {
                emit(Result.failure(DomainError.OfflineError()))
            }
        } catch (e: Exception) {
            emit(Result.failure(e.toDomainError()))
        }
    }.flowOn(Dispatchers.IO)

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
}

fun ChannelStreamInfoResponse.toDomain(): StreamInfo? {
    val finalUrl = this.playback_url ?: this.livestream?.playback_url
    if (finalUrl.isNullOrEmpty()) return null

    val chatroomId = (this.chatroom?.id ?: this.chatroom_id)?.toString()
    val avatar = this.user?.profile_pic?.replace("\\/", "/") ?: ""
    val viewers = this.livestream?.viewer_count ?: 0
    val title = this.livestream?.session_title ?: "Stream"
    val categoryName = this.livestream?.category?.name
    val categorySlug = this.livestream?.category?.slug

    return StreamInfo(
        playbackUrl = finalUrl,
        avatarUrl = avatar,
        viewers = viewers,
        title = title,
        chatroomId = chatroomId,
        categoryName = categoryName,
        categorySlug = categorySlug
    )
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