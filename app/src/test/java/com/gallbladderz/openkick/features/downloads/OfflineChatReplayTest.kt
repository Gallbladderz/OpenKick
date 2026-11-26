/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.downloads

import com.gallbladderz.openkick.core.domain.chat.ChatBadge
import com.gallbladderz.openkick.core.domain.chat.ChatBadgeKind
import com.gallbladderz.openkick.core.domain.chat.ChatEmoteParser
import com.gallbladderz.openkick.core.domain.chat.ChatMessage
import com.gallbladderz.openkick.core.domain.chat.ChatReplaySource
import com.gallbladderz.openkick.core.domain.chat.ReplyToInfo
import com.gallbladderz.openkick.core.network.chat.ChatMessageDataDto
import com.gallbladderz.openkick.core.network.chat.ChatReplayPageDto
import com.gallbladderz.openkick.core.network.chat.ChatSenderDto
import com.gallbladderz.openkick.features.player.ReplayChatMessage
import java.io.File
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineChatReplayTest {

    // (a) Offset / range mapping for a custom-range download.

    @Test
    fun rebasesAbsoluteOffsetsToDownloadStart() {
        assertEquals(65_000L, rebaseChatOffset(125_000L, 60_000L))
        assertEquals(0L, rebaseChatOffset(30_000L, 60_000L))
        assertEquals(0L, rebaseChatOffset(60_000L, 60_000L))
    }

    @Test
    fun resolvesRangeForCustomRangeDownload() {
        // User downloaded 01:00–02:00 of a VOD: chat offsets are absolute,
        // the range to fetch is the same window.
        assertEquals(60_000L to 120_000L, offlineChatRangeMs(60, 120, 218, null))
        // Entire VOD, no explicit range: start at 0, end at the full duration.
        assertEquals(0L to 218_000L, offlineChatRangeMs(null, null, 218, null))
        // Duration only known as a formatted string.
        assertEquals(0L to 218_000L, offlineChatRangeMs(null, null, null, "03:38"))
        assertEquals(0L to 3_723_000L, offlineChatRangeMs(null, null, null, "1:02:03"))
    }

    @Test
    fun returnsNullRangeWhenEndIsUnknown() {
        assertNull(offlineChatRangeMs(null, null, null, null))
        assertNull(offlineChatRangeMs(null, null, null, "not a time"))
        assertNull(offlineChatRangeMs(120, 60, null, null))
    }

    @Test
    fun downloadChatReplayRebasesOffsetsToRangeStart() = runBlocking {
        val startedAt = Instant.parse("2026-01-01T00:00:00Z")
        val source = ChatReplaySource(42L, startedAt)
        // One message 2s into each 5s page the API is asked for.
        val fetched = downloadChatReplay(source, 60_000L, 75_000L) { timestamp ->
            ChatReplayPageDto(
                messages = listOf(
                    ChatMessageDataDto(
                        id = "m-${timestamp.epochSecond}",
                        sender = ChatSenderDto(username = "viewer"),
                        content = "hello",
                        createdAt = timestamp.plusSeconds(2).toString()
                    )
                )
            )
        }
        assertEquals(listOf(2_000L, 7_000L, 12_000L), fetched.map { it.offsetMs })
        assertEquals(listOf("viewer", "viewer", "viewer"), fetched.map { it.message.sender })
    }

    // (b) chat.json serialization round-trip.

    @Test
    fun chatJsonRoundTripPreservesMessagesBadgesAndEmotes() = runBlocking {
        val messages = listOf(
            ReplayChatMessage(
                2_000L,
                ChatMessage(
                    id = "1", sender = "alice", avatarUrl = "https://example.com/a.png",
                    senderColor = "#FF0000", content = "[emote:123:Kappa] hello",
                    tokens = ChatEmoteParser.parseEmotes("[emote:123:Kappa] hello"),
                    badges = listOf(ChatBadge("Subscriber", ChatBadgeKind.CHANNEL, giftedSubscriptions = 3)),
                    replyTo = ReplyToInfo("0", "bob", "hi")
                )
            ),
            ReplayChatMessage(
                5_000L,
                ChatMessage(
                    id = "2", sender = "bob", senderColor = "",
                    content = "plain text",
                    tokens = ChatEmoteParser.parseEmotes("plain text")
                )
            )
        )
        val file = File.createTempFile("chat", ".json")
        try {
            assertTrue(saveOfflineChatReplay(file, messages))
            val loaded = loadOfflineChatReplay(file)
            assertEquals(messages.map { it.offsetMs }, loaded?.map { it.offsetMs })
            val first = loaded?.firstOrNull()?.message
            assertEquals("alice", first?.sender)
            assertEquals("#FF0000", first?.senderColor)
            assertEquals("https://example.com/a.png", first?.avatarUrl)
            assertEquals("[emote:123:Kappa] hello", first?.content)
            assertEquals(
                listOf(com.gallbladderz.openkick.core.domain.chat.ChatToken.Emote("123", "Kappa")),
                first?.tokens?.filterIsInstance<com.gallbladderz.openkick.core.domain.chat.ChatToken.Emote>()
            )
            assertEquals("Subscriber", first?.badges?.firstOrNull()?.name)
            assertEquals(ChatBadgeKind.CHANNEL, first?.badges?.firstOrNull()?.kind)
            assertEquals(3, first?.badges?.firstOrNull()?.giftedSubscriptions)
            assertEquals("bob", first?.replyTo?.senderName)
            assertEquals("plain text", loaded?.get(1)?.message?.content)
        } finally {
            file.delete()
        }
    }

    @Test
    fun loadReturnsNullForMissingOrCorruptChatFile() = runBlocking {
        assertNull(loadOfflineChatReplay(File("/tmp/openkick-does-not-exist-chat.json")))
        val corrupt = File.createTempFile("chat", ".json").apply { writeText("{ not json") }
        try {
            assertNull(loadOfflineChatReplay(corrupt))
        } finally {
            corrupt.delete()
        }
    }

    // (c) Offline filtering by playback position.

    @Test
    fun visibleMessagesAreOnlyThoseUpToPlaybackPosition() {
        val messages = listOf(0L, 5_000L, 10_000L, 20_000L).mapIndexed { index, offset ->
            ReplayChatMessage(
                offset,
                ChatMessage(
                    id = "$index", sender = "u$index", senderColor = "",
                    content = "m$index", tokens = emptyList()
                )
            )
        }
        assertEquals(listOf("0", "1", "2"), visibleOfflineChatMessages(messages, 10_000L).map { it.message.id })
        assertEquals(listOf("0"), visibleOfflineChatMessages(messages, 0L).map { it.message.id })
        assertEquals(
            listOf("0", "1", "2", "3"),
            visibleOfflineChatMessages(messages, 999_999L).map { it.message.id }
        )
    }

    @Test
    fun visibleMessagesAreCappedToMostRecentHundred() {
        val messages = (0 until 150).map { index ->
            ReplayChatMessage(
                index * 1_000L,
                ChatMessage(
                    id = "$index", sender = "u", senderColor = "",
                    content = "m$index", tokens = emptyList()
                )
            )
        }
        val visible = visibleOfflineChatMessages(messages, 1_000_000L)
        assertEquals(100, visible.size)
        assertEquals("50", visible.first().message.id)
        assertEquals("149", visible.last().message.id)
    }

    // (d) Chunked storage for long VODs.

    @Test
    fun chunkIndexAndFileNameForOffsets() {
        assertEquals(0, offlineChatChunkIndex(0L))
        assertEquals(0, offlineChatChunkIndex(3_599_999L))
        assertEquals(1, offlineChatChunkIndex(3_600_000L))
        assertEquals(5, offlineChatChunkIndex(5L * 3_600_000L))
        assertEquals(9, offlineChatChunkIndex(9L * 3_600_000L + 59L * 60_000L))
        assertEquals("chat-0000.json", offlineChatChunkFileName(0))
        assertEquals("chat-0009.json", offlineChatChunkFileName(9))
        assertEquals(5L * 3_600_000L, offlineChatChunkStartMs(5))
        assertEquals(setOf(0, 1), offlineChatChunksNeeded(0L))
        assertEquals(setOf(4, 5, 6), offlineChatChunksNeeded(5L * 3_600_000L))
        assertEquals(setOf(8, 9, 10), offlineChatChunksNeeded(9L * 3_600_000L + 59L * 60_000L))
    }

    @Test
    fun chunkedDownloadWritesOneFilePerHour() = runBlocking {
        val startedAt = Instant.parse("2026-01-01T00:00:00Z")
        val source = ChatReplaySource(42L, startedAt)
        val directory = java.nio.file.Files.createTempDirectory("openkick-chunks").toFile()
        try {
            // 1h 30s of chat: hour 0 (240 windows) + the first 2 windows of hour 1.
            val result = downloadChatReplayChunked(source, 0L, 3_630_000L, directory) { timestamp ->
                ChatReplayPageDto(
                    messages = listOf(
                        ChatMessageDataDto(
                            id = "m-${timestamp.epochSecond}",
                            sender = ChatSenderDto(username = "viewer"),
                            content = "hello",
                            createdAt = timestamp.plusSeconds(2).toString()
                        )
                    )
                )
            }
            assertEquals(listOf(0, 1), result.chunksWritten)
            assertEquals(726, result.totalMessages)
            assertNull(result.failedChunk)
            val chunk0 = loadOfflineChatReplay(offlineChatChunkFile(directory, 0))
            val chunk1 = loadOfflineChatReplay(offlineChatChunkFile(directory, 1))
            assertEquals(720, chunk0?.size)
            assertEquals(6, chunk1?.size)
            assertEquals(2_000L, chunk0?.firstOrNull()?.offsetMs)
            // Hour-1 messages are rebased to the download start, not the chunk start.
            assertEquals(3_602_000L, chunk1?.firstOrNull()?.offsetMs)
            assertEquals(3_627_000L, chunk1?.lastOrNull()?.offsetMs)
            assertEquals(listOf(0, 1), listOfflineChatChunkFiles(directory).map { offlineChatChunkIndexForFile(it) })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun chunkedDownloadRebasesCustomRangeToDownloadStart() = runBlocking {
        val startedAt = Instant.parse("2026-01-01T00:00:00Z")
        val source = ChatReplaySource(42L, startedAt)
        val directory = java.nio.file.Files.createTempDirectory("openkick-chunks-range").toFile()
        try {
            // Custom range 01:00:00–02:00:00 of a 10h VOD: one download-relative hour.
            val result = downloadChatReplayChunked(source, 3_600_000L, 7_200_000L, directory) { timestamp ->
                ChatReplayPageDto(
                    messages = listOf(
                        ChatMessageDataDto(
                            id = "m-${timestamp.epochSecond}",
                            sender = ChatSenderDto(username = "viewer"),
                            content = "hello",
                            createdAt = timestamp.plusSeconds(2).toString()
                        )
                    )
                )
            }
            assertEquals(listOf(0), result.chunksWritten)
            val chunk0 = loadOfflineChatReplay(offlineChatChunkFile(directory, 0))
            assertEquals(720, chunk0?.size)
            assertEquals(2_000L, chunk0?.firstOrNull()?.offsetMs)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun longRangeResolvesForTenHourVod() {
        assertEquals(0L to 36_000_000L, offlineChatRangeMs(null, null, 36_000, null))
        assertEquals(3_600_000L to 36_000_000L, offlineChatRangeMs(3_600, 36_000, null, null))
        assertEquals(0L to 36_000_000L, offlineChatRangeMs(null, null, null, "10:00:00"))
    }

    @Test
    fun loadsOnlyChunksAroundPlaybackPosition() = runBlocking {
        val directory = java.nio.file.Files.createTempDirectory("openkick-chunks-load").toFile()
        try {
            fun messageAt(offsetMs: Long, id: String) = ReplayChatMessage(
                offsetMs,
                ChatMessage(id = id, sender = "u", senderColor = "", content = id, tokens = emptyList())
            )
            assertTrue(saveOfflineChatReplay(offlineChatChunkFile(directory, 0), listOf(messageAt(1_000L, "h0"))))
            assertTrue(saveOfflineChatReplay(offlineChatChunkFile(directory, 5), listOf(messageAt(18_001_000L, "h5"))))
            assertTrue(
                saveOfflineChatReplay(
                    offlineChatChunkFile(directory, 9),
                    listOf(messageAt(35_940_000L, "h9"))
                )
            )
            val atStart = loadOfflineChatChunksForPosition(directory, 0L)
            assertEquals(setOf(0, 1), atStart.keys)
            assertEquals(listOf("h0"), atStart[0]?.map { it.message.id })
            val atFiveHours = loadOfflineChatChunksForPosition(directory, 5L * 3_600_000L)
            assertEquals(setOf(4, 5, 6), atFiveHours.keys)
            assertEquals(listOf("h5"), atFiveHours[5]?.map { it.message.id })
            assertNull(atFiveHours[0])
            val atNineFiftyNine = loadOfflineChatChunksForPosition(directory, 9L * 3_600_000L + 59L * 60_000L)
            assertEquals(setOf(8, 9, 10), atNineFiftyNine.keys)
            assertEquals(listOf("h9"), atNineFiftyNine[9]?.map { it.message.id })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun perChunkCapLimitsMessagesPerHour() = runBlocking {
        val startedAt = Instant.parse("2026-01-01T00:00:00Z")
        val source = ChatReplaySource(42L, startedAt)
        val fetched = downloadChatReplay(source, 0L, 15_000L, maxMessages = 5) { timestamp ->
            ChatReplayPageDto(
                messages = (0 until 25).map { index ->
                    ChatMessageDataDto(
                        id = "m-${timestamp.epochSecond}-$index",
                        sender = ChatSenderDto(username = "viewer"),
                        content = "hello $index",
                        createdAt = timestamp.plusSeconds(1).toString()
                    )
                }
            )
        }
        assertEquals(5, fetched.size)
    }

    @Test
    fun chunkedDownloadKeepsEarlierChunksWhenALaterChunkFails() = runBlocking {
        val startedAt = Instant.parse("2026-01-01T00:00:00Z")
        val source = ChatReplaySource(42L, startedAt)
        val directory = java.nio.file.Files.createTempDirectory("openkick-chunks-fail").toFile()
        try {
            val result = downloadChatReplayChunked(source, 0L, 7_200_000L, directory) { timestamp ->
                if (timestamp >= startedAt.plusMillis(3_600_000L)) {
                    throw java.io.IOException("rate limited")
                }
                ChatReplayPageDto(
                    messages = listOf(
                        ChatMessageDataDto(
                            id = "m-${timestamp.epochSecond}",
                            sender = ChatSenderDto(username = "viewer"),
                            content = "hello",
                            createdAt = timestamp.plusSeconds(2).toString()
                        )
                    )
                )
            }
            assertEquals(listOf(0), result.chunksWritten)
            assertEquals(720, result.totalMessages)
            assertEquals(1, result.failedChunk)
            assertTrue(offlineChatChunkFile(directory, 0).isFile)
            assertTrue(!offlineChatChunkFile(directory, 1).exists())
        } finally {
            directory.deleteRecursively()
        }
    }
}
