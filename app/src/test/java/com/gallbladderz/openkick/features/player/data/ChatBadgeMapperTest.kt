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
}
