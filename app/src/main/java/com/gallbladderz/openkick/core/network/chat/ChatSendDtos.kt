/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.core.network.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendChatMessageRequest(
    @SerialName("content") val content: String,
    @SerialName("type") val type: String,
    @SerialName("message_ref") val messageRef: String,
    @SerialName("metadata") val metadata: SendChatMetadata? = null
)

@Serializable
data class SendChatMetadata(
    @SerialName("original_sender") val originalSender: SendChatOriginalSender? = null,
    @SerialName("original_message") val originalMessage: SendChatOriginalMessage? = null
)

@Serializable
data class SendChatOriginalSender(
    @SerialName("id") val id: Int? = null,
    @SerialName("username") val username: String? = null
)

@Serializable
data class SendChatOriginalMessage(
    @SerialName("id") val id: String? = null,
    @SerialName("content") val content: String? = null
)

@Serializable
data class SendChatMessageResponse(
    @SerialName("data") val data: SendChatMessageData? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("status") val status: SendChatStatus? = null
)

@Serializable
data class SendChatMessageData(
    @SerialName("id") val id: String? = null,
    @SerialName("chatroom_id") val chatroomId: Long? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SendChatStatus(
    @SerialName("error") val error: Boolean? = null,
    @SerialName("code") val code: Int? = null,
    @SerialName("message") val message: String? = null
)
