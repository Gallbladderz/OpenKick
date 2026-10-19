/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.data

import com.gallbladderz.openkick.core.network.chat.SendChatMessageRequest
import com.gallbladderz.openkick.core.network.chat.SendChatMetadata
import com.gallbladderz.openkick.core.network.chat.SendChatOriginalMessage
import com.gallbladderz.openkick.core.network.chat.SendChatOriginalSender
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChatSendRequestTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun ordinaryMessageIncludesAllRequiredFields() {
        val body = json.parseToJsonElement(json.encodeToString(
            SendChatMessageRequest(content = "hello", type = "message", messageRef = "123")
        )).jsonObject

        assertEquals(JsonPrimitive("hello"), body["content"])
        assertEquals(JsonPrimitive("message"), body["type"])
        assertEquals(JsonPrimitive("123"), body["message_ref"])
        assertFalse(body.containsKey("metadata"))
    }

    @Test
    fun replyUsesReplyTypeAndMetadata() {
        val body = json.parseToJsonElement(json.encodeToString(
            SendChatMessageRequest(
                content = "hello",
                type = "reply",
                messageRef = "124",
                metadata = SendChatMetadata(
                    originalSender = SendChatOriginalSender(username = "streamer"),
                    originalMessage = SendChatOriginalMessage(id = "message-id", content = "previous")
                )
            )
        )).jsonObject

        assertEquals(JsonPrimitive("reply"), body["type"])
        assertEquals(JsonPrimitive("124"), body["message_ref"])
        assertEquals(JsonPrimitive("message-id"), body["metadata"]?.jsonObject?.get("original_message")?.jsonObject?.get("id"))
    }
}
