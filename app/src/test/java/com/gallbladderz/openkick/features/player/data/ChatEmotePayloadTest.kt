/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.data

import com.gallbladderz.openkick.core.network.chat.ChatEmotePayload

import com.gallbladderz.openkick.core.domain.chat.ChatEmoteSet
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatEmotePayloadTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parsesNamedSetsAndKeepsEmoteMetadata() {
        val payload = json.parseToJsonElement(
            """{"data":[{"name":"Global","emotes":[{"id":1,"name":"KEKW"}]},{"name":"My subs","emotes":[{"id":"2","code":"Pog"}]}]}"""
        )

        val sets = ChatEmotePayload.parse(payload)

        assertEquals(listOf("Global", "My subs"), sets.map(ChatEmoteSet::name))
        assertEquals("[emote:1:KEKW]", "[emote:${sets[0].emotes[0].id}:${sets[0].emotes[0].name}]")
        assertTrue(sets[1].emotes[0].imageUrl.contains("/emotes/2/"))
    }

    @Test
    fun supportsFlatGlobalAndChannelArrays() {
        val payload = json.parseToJsonElement(
            """{"global_emotes":[{"id":3,"name":"LUL"}],"channel_emotes":[{"id":4,"name":"Hello"}]}"""
        )

        val sets = ChatEmotePayload.parse(payload)

        assertEquals(listOf("Global Emotes", "Channel Emotes"), sets.map(ChatEmoteSet::name))
        assertEquals(2, sets.sumOf { it.emotes.size })
    }

    @Test
    fun parsesKickChannelEnvelopeWithoutTreatingChannelAsAnEmote() {
        val payload = json.parseToJsonElement(
            """[{"id":668,"name":"xqc","emotes":[{"id":1,"channel_id":668,"name":"xqcLK","subscribers_only":false}]},{"name":"Global","id":"Global","emotes":[{"id":2,"channel_id":null,"name":"KEKW","subscribers_only":false}]}]"""
        )

        val sets = ChatEmotePayload.parse(payload)

        assertEquals(listOf("xqc", "Global"), sets.map(ChatEmoteSet::name))
        assertEquals(listOf("xqcLK"), sets[0].emotes.map { it.name })
    }
}
