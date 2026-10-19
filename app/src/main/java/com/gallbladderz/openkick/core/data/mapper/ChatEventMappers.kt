/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.core.data.mapper


import com.gallbladderz.openkick.core.domain.chat.ChatBadge
import com.gallbladderz.openkick.core.domain.chat.ChatBadgeKind
import com.gallbladderz.openkick.core.domain.chat.ChatEmoteParser
import com.gallbladderz.openkick.core.domain.chat.ChatMessage
import com.gallbladderz.openkick.core.domain.chat.ReplyToInfo
import com.gallbladderz.openkick.core.network.chat.ChatMessageDataDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

fun ChatMessageDataDto.toDomainModel(
    channelSubscriberBadges: Map<Int, String> = emptyMap()
): ChatMessage {
    val id = this.id ?: UUID.randomUUID().toString()
    val senderName = this.sender?.username ?: "Anonymous"
    val senderColor = this.sender?.identity?.let { identity ->

        identity.username_color?.takeIf { it.isNotBlank() } ?: identity.color
    } ?: ""
    val safeContent = this.content ?: ""
    val tokens = ChatEmoteParser.parseEmotes(safeContent)

    return ChatMessage(
        id = id,
        sender = senderName,
        avatarUrl = this.sender?.let { it.profilePic ?: it.profilePicture ?: it.avatarUrl }
            .orEmpty(),
        senderColor = senderColor,
        content = safeContent,
        tokens = tokens,
        badges = this.sender?.identity?.let { identity ->
            identity.badges.toBadges(channelSubscriberBadges) +
                    identity.badgesV2.toBadges(channelSubscriberBadges, includeMetadata = true)
        }.orEmpty(),
        replyTo = replyInfo()
    )
}

private fun JsonElement?.toBadges(
    channelSubscriberBadges: Map<Int, String>,
    includeMetadata: Boolean = false
): List<ChatBadge> = when (this) {
    is kotlinx.serialization.json.JsonArray -> mapNotNull { element ->
        val badge = element as? JsonObject ?: return@mapNotNull null
        val name = badge.string("text") ?: badge.string("name") ?: badge.string("type")
        ?: badge.string("badge")
        ?: return@mapNotNull null
        val scope = badge.string("scope") ?: badge.string("category") ?: badge.string("group")
        val type = (badge.string("type")
            ?: badge.string("name")?.takeIf { includeMetadata })?.lowercase().orEmpty()
        val isChannel = isChannelSubscriber(type, scope, name) ||
                badge.string("channel")?.equals("true", ignoreCase = true) == true
        val kind = if (isChannel) {
            ChatBadgeKind.CHANNEL
        } else {
            ChatBadgeKind.GLOBAL
        }
        val count = badge.int("count")
            ?: if (includeMetadata) badge.objectValue("metadata")?.int("level") else null
        val imageUrl = badge.imageUrl()
            ?: if (isChannel) count?.let(channelSubscriberBadges::get)
            else globalBadgeUrl(type, name, count)
        ChatBadge(name = name, kind = kind, imageUrl = imageUrl)
    }

    is JsonObject -> {
        val name = string("text") ?: string("name") ?: string("type") ?: string("badge")
        if (name == null) emptyList() else listOf(
            run {
                val type = (string("type")
                    ?: string("name")?.takeIf { includeMetadata })?.lowercase().orEmpty()
                val isChannel = isChannelSubscriber(type, string("scope"), name)
                ChatBadge(
                    name = name,
                    kind = if (isChannel) ChatBadgeKind.CHANNEL else ChatBadgeKind.GLOBAL,
                    imageUrl = imageUrl()
                        ?: if (isChannel) int("count")?.let(channelSubscriberBadges::get)
                        else globalBadgeUrl(
                            type,
                            name,
                            int("count")
                                ?: if (includeMetadata) objectValue("metadata")?.int("level") else null
                        )
                )
            }
        )
    }

    else -> emptyList()
}

private fun ChatMessageDataDto.replyInfo(): ReplyToInfo? {
    val metadataObject = metadata.asObject()
    val originalMessage = metadataObject?.get("original_message").asObject()
    val originalSender = metadataObject?.get("original_sender").asObject()
    val parent = replies_to.asObject()
    val parentSender = parent?.get("sender").asObject()

    val parentId = originalMessage.string("id") ?: parent.string("message_id") ?: ""
    val parentName = originalSender.string("username") ?: parentSender.string("username") ?: ""
    val parentContent = originalMessage.string("content") ?: parent.string("content") ?: ""
    if (parentName.isBlank() && parentContent.isBlank()) return null

    return ReplyToInfo(
        messageId = parentId,
        senderName = parentName,
        content = parentContent
    )
}

private fun JsonElement?.asObject(): JsonObject? = when (this) {
    is JsonObject -> this
    is JsonPrimitive -> runCatching { Json.parseToJsonElement(content) as? JsonObject }.getOrNull()
    else -> null
}

private fun JsonObject?.string(key: String): String? =
    (this?.get(key) as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() && it != "null" }

private fun JsonObject?.int(key: String): Int? =
    string(key)?.toIntOrNull()

private fun JsonObject?.objectValue(key: String): JsonObject? =
    this?.get(key) as? JsonObject

private fun JsonObject.imageUrl(): String? {
    val nested = (this["badge_image"] as? JsonObject)
    val badgeImage = (this["badge_image"] as? JsonPrimitive)?.content
    return string("image_url")
        ?: string("image")
        ?: string("url")
        ?: string("src")
        ?: badgeImage?.takeIf { it.isNotBlank() && it != "null" }
        ?: nested.string("src")
        ?: nested.string("url")
}

private fun isChannelSubscriber(type: String, scope: String?, name: String): Boolean =
    type.replace('-', '_') in setOf("subscriber", "sub", "channel_subscriber") ||
            (scope?.equals("channel", ignoreCase = true) == true &&
                    name.contains("subscriber", ignoreCase = true))

private fun globalBadgeUrl(type: String, name: String, count: Int?): String? {
    val slug = (type.ifBlank { name })
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
    val normalized = when (slug) {
        "mod" -> "moderator"
        else -> slug
    }
    if (normalized in setOf("level", "level_badge", "levelbadge")) {
        val level = count ?: Regex("level[^0-9]*(\\d+)").find(name.lowercase())
            ?.groupValues?.getOrNull(1)?.toIntOrNull()
        return if (level != null) {
            "https://chat.mobile.kick.com/badges/level_$level.png"
        } else {
            "https://chat.mobile.kick.com/badges/level.png"
        }
    }
    return normalized.takeIf { it.isNotBlank() && it !in setOf("subscriber", "sub") }
        ?.let { "https://chat.mobile.kick.com/badges/$it.png" }
}
