package com.gallbladderz.openkick.features.player

import com.gallbladderz.openkick.core.network.KickApiService
import com.gallbladderz.openkick.features.player.models.ChannelLink
import com.gallbladderz.openkick.features.player.models.StreamInfo
import com.gallbladderz.openkick.core.domain.DomainError
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
            val finalUrl = response.playback_url ?: response.livestream?.playback_url

            val chatroomId = (response.chatroom?.id ?: response.chatroom_id)?.toString()
            var avatar = response.user?.profile_pic ?: ""
            avatar = avatar.replace("\\/", "/")

            val viewers = response.livestream?.viewer_count ?: 0
            val title = response.livestream?.session_title ?: "Stream"

            if (!finalUrl.isNullOrEmpty()) {
                emit(
                    Result.success(
                        StreamInfo(
                            playbackUrl = finalUrl,
                            avatarUrl = avatar,
                            viewers = viewers,
                            title = title,
                            chatroomId = chatroomId
                        )
                    )
                )
            } else {
                emit(Result.failure(DomainError.OfflineError()))
            }
        } catch (e: Exception) {
            emit(Result.failure(DomainError.NetworkError("API processing error: ${e.message}")))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun fetchChannelLinks(streamerName: String): Result<List<ChannelLink>> = withContext(Dispatchers.IO) {
        try {
            val dtos = apiService.getChannelLinks(streamerName)
            val links = dtos.map { dto ->
                ChannelLink(dto.id, dto.description, dto.link, dto.title, dto.image?.url ?: "")
            }
            Result.success(links)
        } catch (e: Exception) {
            Result.failure(DomainError.ApiError("Kick hid the info: ${e.message}"))
        }
    }
}