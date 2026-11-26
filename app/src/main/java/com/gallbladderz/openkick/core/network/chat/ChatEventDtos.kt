/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.network.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.ExperimentalSerializationApi

@Serializable
data class PusherEventDto(
    val event: String? = null,
    val data: JsonElement? = null
)

@Serializable
data class ChatMessageDataDto(
    val id: String? = null,
    val chatroom_id: Long? = null,
    val sender: ChatSenderDto? = null,
    val content: String? = null,
    val type: String? = null,
    val metadata: JsonElement? = null,
    val replies_to: JsonElement? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ChatHistoryResponseDto(val data: ChatHistoryDto)

@Serializable
data class ChatHistoryDto(
    @SerialName("pinned_message") val pinnedMessage: PinnedChatMessageDto? = null
)

@Serializable
data class ChatReplayResponseDto(val data: ChatReplayPageDto)

@Serializable
data class ChatReplayPageDto(
    val messages: List<ChatMessageDataDto> = emptyList(),
    val cursor: String? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PinnedChatMessageDto(
    val message: ChatMessageDataDto? = null,
    @SerialName("pinned_by") @JsonNames("pinnedBy") val pinnedBy: ChatSenderDto? = null,
    @SerialName("finish_at") val finishAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val duration: JsonPrimitive? = null
)

@Serializable
data class ChatSenderDto(
    val username: String? = null,
    @SerialName("profile_pic") val profilePic: String? = null,
    @SerialName("profile_picture") val profilePicture: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val identity: ChatIdentityDto? = null
)

@Serializable
data class ChatIdentityDto(
    val color: String? = null,
    val username_color: String? = null,
    val badges: JsonElement? = null,
    @SerialName("badges_v2") val badgesV2: JsonElement? = null
)

@Serializable
data class CentrifugoFrameDto(
    val id: Int? = null,
    val error: CentrifugoErrorDto? = null,
    val push: CentrifugoPushDto? = null,
    val event: String? = null,
    val data: JsonElement? = null,
    val channel: String? = null
) {

    val resolvedEvent: PusherEventDto?
        get() = push?.pub ?: push?.data
        ?: if (event != null || data != null) PusherEventDto(event, data) else null
}

@Serializable
data class CentrifugoPushDto(
    val channel: String? = null,

    val pub: PusherEventDto? = null,
    val data: PusherEventDto? = null
)

@Serializable
data class CentrifugoErrorDto(
    val code: Int? = null,
    val message: String? = null,
    val temporary: Boolean? = null
)

object CentrifugoErrors {
    const val INTERNAL_SERVER_ERROR = 100
    const val UNAUTHORIZED = 101
    const val UNKNOWN_CHANNEL = 102
    const val PERMISSION_DENIED = 103
    const val METHOD_NOT_FOUND = 104
    const val ALREADY_SUBSCRIBED = 105
    const val LIMIT_EXCEEDED = 106
    const val BAD_REQUEST = 107
    const val NOT_AVAILABLE = 108

    /**
     * Human-readable description of a Centrifugo error. Known codes get a
     * specific text, unknown codes fall back to the server message when it is
     * non-blank, otherwise to a generic message. (Restored: the v1.4 squash
     * dropped this helper while its tests remained; the same mapping also
     * exists as string resources in ChatRepository.describeCentrifugoError.)
     */
    fun describe(code: Int?, message: String?): String {
        when (code) {
            INTERNAL_SERVER_ERROR -> return "Ошибка сервера чата"
            UNAUTHORIZED -> return "Токен недействителен или истёк"
            UNKNOWN_CHANNEL -> return "Чат не найден"
            PERMISSION_DENIED -> return "Нет доступа к этому чату"
            METHOD_NOT_FOUND -> return "Этот метод чата не поддерживается"
            ALREADY_SUBSCRIBED -> return "Подписка уже существует"
            LIMIT_EXCEEDED -> return "Превышен лимит подключений к чату"
            BAD_REQUEST -> return "Неверный запрос к серверу чата"
            NOT_AVAILABLE -> return "Чат временно недоступен"
        }
        return message?.takeIf { it.isNotBlank() } ?: "Ошибка сервера чата"
    }

}
