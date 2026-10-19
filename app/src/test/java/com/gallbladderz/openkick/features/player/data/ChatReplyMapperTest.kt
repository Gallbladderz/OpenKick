/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.data

import com.gallbladderz.openkick.core.network.chat.ChatMessageDataDto
import com.gallbladderz.openkick.core.data.mapper.toDomainModel
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatReplyMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun mapsRealtimeReplyMetadata() {
        val event = json.decodeFromString<ChatMessageDataDto>(
            """{"id":"reply-1","sender":{"username":"viewer"},"content":"hello","metadata":{"original_message":{"id":"parent-1","content":"How are you?"},"original_sender":{"username":"me"}}}"""
        )

        val reply = event.toDomainModel().replyTo
        assertEquals("parent-1", reply?.messageId)
        assertEquals("me", reply?.senderName)
        assertEquals("How are you?", reply?.content)
    }

    @Test
    fun mapsDocumentedRepliesToShape() {
        val event = json.decodeFromString<ChatMessageDataDto>(
            """{"id":"reply-2","content":"hello","replies_to":{"message_id":"parent-2","content":"Original text","sender":{"username":"streamer"}}}"""
        )

        val reply = event.toDomainModel().replyTo
        assertEquals("parent-2", reply?.messageId)
        assertEquals("streamer", reply?.senderName)
        assertEquals("Original text", reply?.content)
    }

    @Test
    fun ordinaryMessageHasNoReply() {
        val event = json.decodeFromString<ChatMessageDataDto>(
            """{"id":"message-1","content":"hello"}"""
        )

        assertNull(event.toDomainModel().replyTo)
    }

    @Test
    fun mapsOfficialNameColorAndBadgeScopes() {
        val event = json.decodeFromString<ChatMessageDataDto>(
            """{"sender":{"username":"viewer","identity":{"username_color":"#12AB34","badges":[{"type":"verified","text":"Verified"},{"type":"subscriber","text":"Sub"}]}},"content":"hello"}"""
        )

        val message = event.toDomainModel()
        assertEquals("#12AB34", message.senderColor)
        assertEquals(2, message.badges.size)
        assertEquals("GLOBAL", message.badges[0].kind.name)
        assertEquals("CHANNEL", message.badges[1].kind.name)
    }
}
