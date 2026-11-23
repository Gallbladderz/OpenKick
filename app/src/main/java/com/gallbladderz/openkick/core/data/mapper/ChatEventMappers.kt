/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.core.data.mapper


import com.gallbladderz.openkick.core.domain.chat.ChatBadge
import com.gallbladderz.openkick.core.domain.chat.ChatBadgeKind
import com.gallbladderz.openkick.core.domain.chat.ChatEmoteParser
import com.gallbladderz.openkick.core.domain.chat.ChatMessage
import com.gallbladderz.openkick.core.domain.chat.PinnedChatMessage
import com.gallbladderz.openkick.core.domain.chat.ReplyToInfo
import com.gallbladderz.openkick.core.network.chat.ChatIdentityDto
import com.gallbladderz.openkick.core.network.chat.PinnedChatMessageDto
import com.gallbladderz.openkick.core.network.chat.ChatMessageDataDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.OffsetDateTime
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
        badges = sender?.identity.toBadges(channelSubscriberBadges),
        replyTo = replyInfo()
    )
}

internal fun PinnedChatMessageDto.toDomainModel(
    channelSubscriberBadges: Map<Int, String>,
    receivedAtMillis: Long = System.currentTimeMillis()
): PinnedChatMessage? {
    val message = message?.takeIf { !it.id.isNullOrBlank() && it.content != null } ?: return null
    val expiresAtMillis = finishAt.toEpochMillis()
        ?: duration?.longOrNull?.takeIf { it in 1..Int.MAX_VALUE.toLong() }?.let { minutes ->
            (createdAt.toEpochMillis() ?: receivedAtMillis) + minutes * 60_000L
        }
    return PinnedChatMessage(
        message = message.toDomainModel(channelSubscriberBadges),
        pinnedBy = pinnedBy?.username?.takeIf { it.isNotBlank() },
        pinnerBadges = pinnedBy?.identity.toBadges(channelSubscriberBadges, selectedOnly = true),
        expiresAtMillis = expiresAtMillis
    )
}

private fun String?.toEpochMillis(): Long? = this?.let {
    runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
}

private fun ChatIdentityDto?.toBadges(
    channelSubscriberBadges: Map<Int, String>,
    selectedOnly: Boolean = false
): List<ChatBadge> = this?.let { identity ->
    (identity.badges.toBadges(channelSubscriberBadges, selectedOnly = selectedOnly) +
        identity.badgesV2.toBadges(channelSubscriberBadges, includeMetadata = true, selectedOnly = selectedOnly))
        .sortedBy { it.kind == ChatBadgeKind.CHANNEL }
}.orEmpty()

private fun JsonElement?.toBadges(
    channelSubscriberBadges: Map<Int, String>,
    includeMetadata: Boolean = false,
    selectedOnly: Boolean = false
): List<ChatBadge> {
    val badges = when (this) {
        is kotlinx.serialization.json.JsonArray -> filterIsInstance<JsonObject>()
        is JsonObject -> listOf(this)
        else -> emptyList()
    }
    return badges.mapNotNull { badge ->
        if (selectedOnly && !badge.isSelected()) return@mapNotNull null
        val name = badge.string("text") ?: badge.string("name") ?: badge.string("type")
            ?: badge.string("badge") ?: return@mapNotNull null
        val scope = badge.string("badge_type") ?: badge.string("scope")
            ?: badge.string("category") ?: badge.string("group")
        val type = (badge.string("type") ?: badge.string("name")?.takeIf { includeMetadata })
            ?.lowercase().orEmpty()
        val subscriber = isChannelSubscriber(type, scope, name)
        val metadata = badge.objectValue("metadata")
        val count = badge.int("count") ?: if (includeMetadata) metadata?.int("level") else null
        val giftedSubscriptions = if (type.ifBlank { name.lowercase() }.replace('-', '_').replace(' ', '_') == "sub_gifter") {
            (badge.int("count") ?: metadata?.int("gifted_subs") ?: metadata?.int("count") ?: count)
                ?.coerceAtLeast(1) ?: 1
        } else null
        if (giftedSubscriptions != null) {
            return@mapNotNull ChatBadge(name, ChatBadgeKind.CHANNEL, giftedSubscriptions = giftedSubscriptions)
        }
        val channel = subscriber || scope.equals("channel", ignoreCase = true) ||
            badge.string("channel")?.equals("true", ignoreCase = true) == true
        val image = badge.imageUrl() ?: when {
            subscriber -> {
                val months = badge.int("count") ?: badge.int("months") ?: metadata?.int("months") ?: count
                channelSubscriberBadges.subscriberImage(months)
            }
            channel -> count?.let(channelSubscriberBadges::get)
            else -> globalBadgeUrl(type, name, count)
        }
        ChatBadge(name, if (channel) ChatBadgeKind.CHANNEL else ChatBadgeKind.GLOBAL, image)
    }
}

private fun Map<Int, String>.subscriberImage(months: Int?): String? =
    if (months == null || months <= 0) {
        minByOrNull { it.key }?.value
    } else {
        get(months) ?: entries.asSequence().filter { it.key <= months }.maxByOrNull { it.key }?.value
    }

private fun JsonObject.isSelected(): Boolean =
    string("selected") != "false" && string("active") != "false"

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
