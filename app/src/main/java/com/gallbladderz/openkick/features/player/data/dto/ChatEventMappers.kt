/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.data.dto

import com.gallbladderz.openkick.features.player.models.ChatMessage
import com.gallbladderz.openkick.features.player.utils.ChatEmoteParser
import java.util.UUID

fun ChatMessageDataDto.toDomainModel(): ChatMessage {
    val id = this.id ?: UUID.randomUUID().toString()
    val senderName = this.sender?.username ?: "Anonymous"
    val senderColor = this.sender?.identity?.color ?: ""
    val safeContent = this.content ?: ""
    val tokens = ChatEmoteParser.parseEmotes(safeContent)

    return ChatMessage(
        id = id,
        sender = senderName,
        senderColor = senderColor,
        content = safeContent,
        tokens = tokens
    )
}
