/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.data

import com.gallbladderz.openkick.core.data.mapper.toDomainModel
import com.gallbladderz.openkick.core.domain.chat.ChatBadgeKind
import com.gallbladderz.openkick.core.network.chat.CentrifugoFrameParser
import com.gallbladderz.openkick.core.network.chat.ChatHistoryResponseDto
import com.gallbladderz.openkick.core.network.chat.PinnedChatMessageDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class PinnedChatMessageTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `loads the pinning user's selected badges and expiry from chat history`() {
        val payload = """
            {"data": {"pinned_message": {
                "message": {"id": "pin", "content": "Welcome", "sender": {"username": "Author"}},
                "finish_at": "2026-11-21T08:46:33.842684Z",
                "pinned_by": {
                    "username": "Moderator",
                    "identity": {
                        "badges": [{"type": "moderator", "active": true}],
                        "badges_v2": [
                            {"name": "level", "badge_type": "global", "selected": false},
                            {"name": "founder", "badge_type": "channel", "selected": true}
                        ]
                    }
                },
                "duration": 756
            }}}
        """.trimIndent()

        val pin = requireNotNull(json.decodeFromString<ChatHistoryResponseDto>(payload)
            .data.pinnedMessage?.toDomainModel(emptyMap()))

        assertEquals("Author", pin.message.sender)
        assertEquals("Moderator", pin.pinnedBy)
        assertEquals(listOf("moderator", "founder"), pin.pinnerBadges.map { it.name })
        assertEquals(listOf(ChatBadgeKind.GLOBAL, ChatBadgeKind.CHANNEL), pin.pinnerBadges.map { it.kind })
        assertEquals(Instant.parse("2026-11-21T08:46:33.842684Z").toEpochMilli(), pin.expiresAtMillis)
        assertNull(json.decodeFromString<ChatHistoryResponseDto>("""{"data":{"pinned_message":null}}""")
            .data.pinnedMessage)
    }

    @Test
    fun `decodes a realtime pin with pinnedBy and string duration`() {
        val payload = """
            {"event":"App\\Events\\PinnedMessageCreatedEvent",
             "data":"{\"message\":{\"id\":\"pin\",\"content\":\"Welcome\"},\"pinnedBy\":{\"username\":\"Moderator\"},\"duration\":\"3\"}"}
        """.trimIndent()
        val event = requireNotNull(CentrifugoFrameParser.parse(payload)?.resolvedEvent)
        val pin = requireNotNull(json.decodeFromString<PinnedChatMessageDto>((event.data as JsonPrimitive).content)
            .toDomainModel(emptyMap(), receivedAtMillis = 1_000L))

        assertEquals("App\\Events\\PinnedMessageCreatedEvent", event.event)
        assertEquals("pin", pin.message.id)
        assertEquals("Moderator", pin.pinnedBy)
        assertEquals(181_000L, pin.expiresAtMillis)
    }
}
