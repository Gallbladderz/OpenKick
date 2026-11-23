/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player

import com.gallbladderz.openkick.core.domain.chat.ChatReplaySource
import com.gallbladderz.openkick.core.network.chat.ChatMessageDataDto
import com.gallbladderz.openkick.core.network.chat.ChatReplayPageDto
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatReplayTest {
    private val start = Instant.parse("2026-11-06T17:52:03Z")
    private val source = ChatReplaySource(16_957_782, start)

    @Test
    fun usesSourceTimeAndBoundsMessagesAfterSeeking() = runBlocking {
        val requested = mutableListOf<Instant>()
        val messages = fetchChatReplayWindow(source, 15_000, 20_000) { timestamp ->
            requested.add(timestamp)
            ChatReplayPageDto(
                messages = listOf(
                    message("earlier", 14),
                    message("end", 20),
                    message("last", 19),
                    message("first", 15),
                    message("last", 19)
                )
            )
        }
        assertEquals(listOf(start.plusSeconds(15)), requested)
        assertEquals(listOf("first", "last"), messages.map { it.message.id })
        assertEquals(listOf(15_000L, 19_000L), messages.map { it.offsetMs })
    }

    @Test
    fun advancesFromMicrosecondCursorOnFullPagesAndKeepsGoingPastSilence() = runBlocking {
        val cursorTime = start.plusSeconds(2).plusNanos(345_678_000)
        val cursorUs = cursorTime.epochSecond * 1_000_000 + cursorTime.nano / 1_000
        val requested = mutableListOf<Instant>()
        val messages = fetchChatReplayWindow(source, 0, 10_000) {
            requested.add(it)
            when (requested.size) {
                1 -> ChatReplayPageDto(List(25) { index -> message("id-$index", 2) }, cursorUs.toString())
                2 -> ChatReplayPageDto(cursor = start.plusSeconds(100).epochSecond.toString())
                else -> ChatReplayPageDto(listOf(message("after-silence", 9)))
            }
        }
        assertEquals(
            listOf(start, cursorTime.plusNanos(1_000), cursorTime.plusNanos(1_000).plusSeconds(5)),
            requested
        )
        assertEquals(26, messages.size)
        assertEquals("after-silence", messages.last().message.id)
    }

    @Test
    fun parsesPlaybackTimestampsFromBothApiFormats() {
        assertEquals(source, ChatReplaySource.from(source.channelId, "2026-11-06 17:52:03"))
        assertEquals(source, ChatReplaySource.from(source.channelId, "2026-11-06T20:52:03+03:00"))
        assertNull(ChatReplaySource.from(source.channelId, null))
    }

    private fun message(id: String, seconds: Long) =
        ChatMessageDataDto(id = id, content = id, createdAt = start.plusSeconds(seconds).toString())
}
