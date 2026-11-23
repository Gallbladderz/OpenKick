/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.data

import com.gallbladderz.openkick.core.data.mapper.toDomainModel
import com.gallbladderz.openkick.core.domain.chat.ChatBadgeKind
import com.gallbladderz.openkick.core.network.chat.ChatIdentityDto
import com.gallbladderz.openkick.core.network.chat.ChatMessageDataDto
import com.gallbladderz.openkick.core.network.chat.ChatSenderDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatBadgeMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun resolvesGlobalAndChannelBadgeImages() {
        val dto = ChatMessageDataDto(
            sender = ChatSenderDto(
                username = "viewer",
                identity = ChatIdentityDto(
                    badges = json.parseToJsonElement(
                        """[
                            {"type":"moderator","text":"Moderator"},
                            {"type":"og","text":"OG"},
                            {"type":"subscriber","text":"Subscriber","count":3}
                        ]"""
                    ),
                    badgesV2 = json.parseToJsonElement(
                        """[{"name":"level","metadata":{"level":12}}]"""
                    )
                )
            ),
            content = "hello"
        )

        val message = dto.toDomainModel(
            channelSubscriberBadges = mapOf(
                3 to "https://files.cdn.kick.com/channel_subscriber_badges/913947/conversion/original-fullsize.png?width=96"
            )
        )

        assertEquals(4, message.badges.size)
        assertEquals(ChatBadgeKind.GLOBAL, message.badges[0].kind)
        assertEquals("https://chat.mobile.kick.com/badges/moderator.png", message.badges[0].imageUrl)
        assertEquals("https://chat.mobile.kick.com/badges/og.png", message.badges[1].imageUrl)
        assertEquals("https://chat.mobile.kick.com/badges/level_12.png", message.badges[2].imageUrl)
        assertEquals(ChatBadgeKind.CHANNEL, message.badges[3].kind)
        assertTrue(message.badges[3].imageUrl!!.contains("channel_subscriber_badges/913947"))
    }

    @Test
    fun selectsHighestUnlockedChannelBadgeWhenMonthDoesNotMatchATier() {
        val tiers = mapOf(
            1 to "https://files.kick.com/channel_subscriber_badges/398434/conversion/original-fullsize.png",
            2 to "https://files.kick.com/channel_subscriber_badges/398435/conversion/original-fullsize.png"
        )
        val dto = json.decodeFromString<ChatMessageDataDto>(
            """{"sender":{"identity":{"badges":[
                {"type":"subscriber","text":"Subscriber","count":21}
            ]}}}"""
        )

        val badge = dto.toDomainModel(tiers).badges.single()
        assertEquals(ChatBadgeKind.CHANNEL, badge.kind)
        assertEquals(tiers[2], badge.imageUrl)
    }

    @Test
    fun usesMonthMetadataAndKeepsExplicitBadgeArtwork() {
        val tiers = mapOf(1 to "first", 3 to "third", 12 to "twelfth")
        val dto = json.decodeFromString<ChatMessageDataDto>(
            """{"sender":{"identity":{"badges_v2":{
                "name":"subscriber","metadata":{"months":7}
            }}}}"""
        )
        val withImage = json.decodeFromString<ChatMessageDataDto>(
            """{"sender":{"identity":{"badges":[
                {"type":"subscriber","count":7,"image_url":"https://files.kick.com/custom.png"}
            ]}}}"""
        )

        assertEquals(tiers[3], dto.toDomainModel(tiers).badges.single().imageUrl)
        assertEquals("https://files.kick.com/custom.png", withImage.toDomainModel(tiers).badges.single().imageUrl)
    }

    @Test
    fun mapsGiftedSubscriptionsToLocalChannelBadgeWithCount() {
        val dto = json.decodeFromString<ChatMessageDataDto>(
            """{"sender":{"identity":{"badges":[
                {"type":"sub_gifter","text":"Sub Gifter","count":125,"sort_order":8}
            ]}}}"""
        )
        val badge = dto.toDomainModel(mapOf(1 to "subscriber-art")).badges.single()
        assertEquals(ChatBadgeKind.CHANNEL, badge.kind)
        assertEquals(125, badge.giftedSubscriptions)
        assertNull(badge.imageUrl)
    }

    @Test
    fun recognizesGiftCountInModernBadgeMetadata() {
        val dto = json.decodeFromString<ChatMessageDataDto>(
            """{"sender":{"identity":{"badges_v2":[
                {"name":"sub_gifter","metadata":{"gifted_subs":12}}
            ]}}}"""
        )
        assertEquals(12, dto.toDomainModel().badges.single().giftedSubscriptions)
    }
}
