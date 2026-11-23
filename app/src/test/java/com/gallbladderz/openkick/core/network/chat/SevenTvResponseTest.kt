/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.network.chat

import com.gallbladderz.openkick.core.network.dto.player.ChannelStreamInfoResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SevenTvResponseTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun channelSetUsesAliasWebpDimensionsAndBothZeroWidthFlags() {
        val payload = """
            {"emote_set":{"emotes":[
                {"id":"A","name":"ChannelAlias","flags":1,"data":{
                    "name":"OriginalName","animated":true,"host":{"url":"//cdn.7tv.app/emote/A","files":[
                        {"name":"1x.webp","width":32,"height":16},
                        {"name":"2x.png","width":128,"height":64},
                        {"name":"2x.webp","width":64,"height":24}]}
                }},
                {"id":"B","name":"Overlay","data":{
                    "flags":256,"host":{"url":"https://cdn.7tv.app/emote/B/","files":[
                        {"name":"2x.png","width":64,"height":64},
                        {"name":"1x.webp","width":32,"height":32}]}
                }}
            ]}}
        """.trimIndent()
        val (first, second) = json.decodeFromString<SevenTvConnectionDto>(payload).emoteSet!!.toEmotes()

        assertEquals("ChannelAlias", first.name)
        assertEquals("https://cdn.7tv.app/emote/A/2x.webp", first.imageUrl)
        assertEquals(64, first.width)
        assertEquals(24, first.height)
        assertTrue(first.animated)
        assertTrue(first.isZeroWidth)
        assertEquals("https://cdn.7tv.app/emote/B/1x.webp", second.imageUrl)
        assertTrue(second.isZeroWidth)
        assertFalse(second.animated)
    }

    @Test
    fun globalSetFallsBackToPngThenFirstValidFileAndSkipsMissingImages() {
        val payload = """
            {"emotes":[
                {"id":"A","name":"Png","data":{"host":{"url":"//cdn.7tv.app/A","files":[
                    {"name":"1x.gif"},{"name":"2x.png"}]}}},
                {"id":"B","name":"Gif","data":{"host":{"url":"//cdn.7tv.app/B","files":[{"name":"1x.gif"}]}}},
                {"id":"C","name":"NoImage","data":{"host":{"url":"//cdn.7tv.app/C","files":[]}}}
            ]}
        """.trimIndent()
        val emotes = json.decodeFromString<SevenTvEmoteSetDto>(payload).toEmotes()

        assertEquals(listOf("Png", "Gif"), emotes.map { it.name })
        assertEquals(listOf("2x.png", "1x.gif"), emotes.map { it.imageUrl.substringAfterLast('/') })
        assertFalse(emotes.first().isZeroWidth)
    }

    @Test
    fun kickEndpointReadsOwnerIdAndNeverUsesChannelId() {
        assertEquals(22L, json.decodeFromString<ChannelStreamInfoResponse>(
            """{"id":11,"user_id":22,"user":{"id":33}}"""
        ).kickUserId)
        assertEquals(33L, json.decodeFromString<ChannelStreamInfoResponse>(
            """{"id":11,"user":{"id":33}}"""
        ).kickUserId)
        assertNull(json.decodeFromString<ChannelStreamInfoResponse>("""{"id":11}""").kickUserId)
    }
}
