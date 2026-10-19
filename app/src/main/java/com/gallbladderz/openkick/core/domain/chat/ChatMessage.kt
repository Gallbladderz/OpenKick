/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.core.domain.chat


sealed interface ChatToken {
    data class Text(val text: String) : ChatToken
    data class Emote(val emoteId: String, val emoteName: String) : ChatToken
}

enum class ChatBadgeKind {
    GLOBAL,
    CHANNEL
}

data class ChatBadge(
    val name: String,
    val kind: ChatBadgeKind,
    val imageUrl: String? = null
)

data class ReplyToInfo(
    val messageId: String,
    val senderName: String,
    val content: String
)

data class ChatMessage(
    val id: String,
    val sender: String,
    val avatarUrl: String = "",
    val senderColor: String,
    val content: String,
    val tokens: List<ChatToken>,
    val badges: List<ChatBadge> = emptyList(),
    val replyTo: ReplyToInfo? = null,
    val isPending: Boolean = false
)

enum class ChatModeType {
    OPEN,
    SLOW,
    SUBSCRIBERS,
    DISABLED
}

data class ChatMode(
    val type: ChatModeType = ChatModeType.OPEN,
    val slowModeSeconds: Int = 0,
    val isSubscriber: Boolean? = null
)
