package com.gallbladderz.openkick.features.player

import com.gallbladderz.openkick.core.network.KickApiService
import com.gallbladderz.openkick.features.player.models.ChannelLink
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

            val url = response.playback_url ?: response.livestream?.playback_url
            val chatroomId = (response.chatroom?.id ?: response.chatroom_id)?.toString()
            var avatar = response.user?.profile_pic ?: ""
            avatar = avatar.replace("\\/", "/")

            val viewers = response.livestream?.viewer_count ?: 0
            val title = response.livestream?.session_title ?: "Stream"

            if (!url.isNullOrEmpty()) {
                emit(
                    Result.success(
                        StreamInfo(
                            playbackUrl = url,
                            avatarUrl = avatar,
                            viewers = viewers,
                            title = title,
                            chatroomId = chatroomId
                        )
                    )
                )
            } else {
                emit(Result.failure(Exception("Streamer is currently offline")))
            }
        } catch (e: Exception) {
            emit(Result.failure(Exception("API response processing error: ${e.message}")))
        }
    }.flowOn(Dispatchers.IO)

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
            Result.failure(Exception("Kick hid the info: ${e.message}"))
        }
    }
}