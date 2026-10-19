/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.features.player


import com.gallbladderz.openkick.core.auth.SessionManager
import com.gallbladderz.openkick.core.data.mapper.toDomain
import com.gallbladderz.openkick.core.domain.DomainError
import com.gallbladderz.openkick.core.domain.model.ChannelLink
import com.gallbladderz.openkick.core.domain.toDomainError
import com.gallbladderz.openkick.core.network.api.KickApiService
import com.gallbladderz.openkick.core.network.dto.player.ChannelStreamInfoResponse
import com.gallbladderz.openkick.features.player.models.StreamInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

class PlayerRepository(
    private val apiService: KickApiService,
    private val authenticatedApiService: KickApiService? = null,
    private val sessionManager: SessionManager? = null
) {
    fun fetchStreamInfo(streamerName: String): Flow<Result<StreamInfo>> = flow {
        try {
            val response = apiService.getChannelStreamInfo(streamerName)
            val chatroom = runCatching {
                apiService.getChannelChatroom(streamerName)
            }.getOrNull()
            val channelMe = if (sessionManager?.tokens() != null) {
                runCatching {
                    authenticatedApiService?.getChannelMe(streamerName)
                }.getOrNull()
            } else {
                null
            }
            val streamInfo = response.toDomain(chatroom ?: response.chatroom, channelMe)
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

fun ChannelStreamInfoResponse.toDomain(
    chatroom: com.gallbladderz.openkick.core.network.dto.player.ChatroomDto? = this.chatroom,
    channelMe: kotlinx.serialization.json.JsonElement? = null
): StreamInfo? {
    val finalUrl = this.playback_url ?: this.livestream?.playback_url
    if (finalUrl.isNullOrEmpty()) return null

    val chatroomId = (chatroom?.id ?: this.chatroom?.id ?: this.chatroom_id)?.toString()
    val avatar = this.user?.profile_pic?.replace("\\/", "/") ?: ""
    val viewers = this.livestream?.viewer_count ?: 0
    val title = this.livestream?.session_title ?: "Stream"
    val categoryName = this.livestream?.category?.name ?: this.recent_categories.firstOrNull()?.name
    val categorySlug = this.livestream?.category?.slug ?: this.recent_categories.firstOrNull()?.slug
    val subscriberBadges = subscriber_badges.mapNotNull { badge ->
        val months = badge.months ?: return@mapNotNull null
        val image = badge.badgeImage?.src
            ?.takeIf { it.isNotBlank() }
            ?: badge.id?.let { "https://files.cdn.kick.com/channel_subscriber_badges/$it/conversion/original-fullsize.png" }
            ?: return@mapNotNull null
        months to if ('?' in image) image else "$image?width=96"
    }.toMap()

    return StreamInfo(
        playbackUrl = finalUrl,
        avatarUrl = avatar,
        viewers = viewers,
        title = title,
        chatroomId = chatroomId,
        categoryName = categoryName,
        categorySlug = categorySlug,
        isVerified = this.verified,
        subscriberBadges = subscriberBadges,
        chatMode = chatroom.toChatMode(channelMe)
    )
}

private fun com.gallbladderz.openkick.core.network.dto.player.ChatroomDto?.toChatMode(
    channelMe: kotlinx.serialization.json.JsonElement?
): com.gallbladderz.openkick.core.domain.chat.ChatMode {
    if (this == null) return com.gallbladderz.openkick.core.domain.chat.ChatMode()

    val mode = chat_mode.orEmpty().lowercase()
    val disabled = enabled.asBoolean() == false || is_enabled.asBoolean() == false || mode in setOf(
        "disabled",
        "off",
        "closed"
    )
    val subscribersOnly = subscribers_mode.asBoolean() == true || mode.contains("subscriber")
    val slow = slow_mode.asBoolean() == true || mode.contains("slow")
    val type = when {
        disabled -> com.gallbladderz.openkick.core.domain.chat.ChatModeType.DISABLED
        subscribersOnly -> com.gallbladderz.openkick.core.domain.chat.ChatModeType.SUBSCRIBERS
        slow -> com.gallbladderz.openkick.core.domain.chat.ChatModeType.SLOW
        else -> com.gallbladderz.openkick.core.domain.chat.ChatModeType.OPEN
    }

    return com.gallbladderz.openkick.core.domain.chat.ChatMode(
        type = type,
        slowModeSeconds = message_interval.asInt() ?: slow_mode.objectInt("message_interval") ?: 0,
        isSubscriber = is_subscriber.asBoolean()
            ?: user_is_subscriber.asBoolean()
            ?: subscribed.asBoolean()
            ?: channelMe.findBoolean("is_subscriber", "is_subscribed", "subscribed")
    )
}

private fun kotlinx.serialization.json.JsonElement?.asBoolean(): Boolean? = when (this) {
    is kotlinx.serialization.json.JsonPrimitive -> this.booleanOrNull
    is kotlinx.serialization.json.JsonObject -> this["enabled"].asBoolean()
    else -> null
}

private fun kotlinx.serialization.json.JsonElement?.asInt(): Int? = when (this) {
    is kotlinx.serialization.json.JsonPrimitive -> this.intOrNull
    else -> null
}

private fun kotlinx.serialization.json.JsonElement?.objectInt(key: String): Int? =
    (this as? kotlinx.serialization.json.JsonObject)?.get(key).asInt()

private fun kotlinx.serialization.json.JsonElement?.findBoolean(vararg keys: String): Boolean? =
    when (this) {
        is kotlinx.serialization.json.JsonObject -> {
            keys.firstNotNullOfOrNull { key -> this[key].asBoolean() }
                ?: this.values.firstNotNullOfOrNull { it.findBoolean(*keys) }
        }

        is kotlinx.serialization.json.JsonArray -> firstNotNullOfOrNull { it.findBoolean(*keys) }
        else -> null
    }
