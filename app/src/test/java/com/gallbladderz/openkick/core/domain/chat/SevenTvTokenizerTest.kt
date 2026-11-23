/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.domain.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class SevenTvTokenizerTest {
    @Test
    fun mixesKickAndSevenTvAndPrefersTheChannelAlias() {
        val global = emote("global", "Wave")
        val channel = emote("channel", "Wave")
        val other = emote("wide", "Wide")
        val content = "Hi [emote:42:Wave] Wave @Wave https://example.test/Wave wave Wave! Wide"
        val result = tokenizeChatDisplay(
            ChatEmoteParser.parseEmotes(content),
            sevenTvEmoteIndex(listOf(channel), listOf(global, other))
        )

        assertEquals(listOf(
            ChatDisplayToken.Text("Hi "),
            ChatDisplayToken.Emote(ChatDisplayEmote("42", "Wave")),
            ChatDisplayToken.Text(" "),
            ChatDisplayToken.Emote(channel.toDisplayEmote()),
            ChatDisplayToken.Text(" @Wave https://example.test/Wave wave Wave! "),
            ChatDisplayToken.Emote(other.toDisplayEmote())
        ), result)
    }

    @Test
    fun stacksOverlayChainsOnKickAndSevenTvAndRendersAnOrphanNormally() {
        val heart = emote("heart", "Heart", zeroWidth = true)
        val hat = emote("hat", "Hat", zeroWidth = true)
        val base = emote("base", "Base")
        val result = tokenizeChatDisplay(
            ChatEmoteParser.parseEmotes("Heart said [emote:7:Kick] Hat Heart Base Hat\nHeart"),
            sevenTvEmoteIndex(listOf(base, heart, hat), emptyList())
        )

        assertEquals(listOf(
            ChatDisplayToken.Emote(heart.toDisplayEmote()),
            ChatDisplayToken.Text(" said "),
            ChatDisplayToken.Emote(ChatDisplayEmote("7", "Kick"), listOf(hat.toDisplayEmote(), heart.toDisplayEmote())),
            ChatDisplayToken.Text(" "),
            ChatDisplayToken.Emote(base.toDisplayEmote(), listOf(hat.toDisplayEmote())),
            ChatDisplayToken.Text("\n"),
            ChatDisplayToken.Emote(heart.toDisplayEmote())
        ), result)
    }

    @Test
    fun preservesSevenTvNamesWhenTheDictionaryIsEmpty() {
        val result = tokenizeChatDisplay(ChatEmoteParser.parseEmotes("Wave [emote:42:Wave]"), emptyMap())

        assertEquals(listOf(
            ChatDisplayToken.Text("Wave "),
            ChatDisplayToken.Emote(ChatDisplayEmote("42", "Wave"))
        ), result)
    }

    private fun emote(id: String, name: String, zeroWidth: Boolean = false) =
        SevenTvEmote(id, name, "https://cdn.7tv.app/$id/2x.webp", 64, 32, false, zeroWidth)
}
