/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.downloads

import com.gallbladderz.openkick.core.data.local.DownloadEntity
import com.gallbladderz.openkick.core.domain.chat.ChatBadge
import com.gallbladderz.openkick.core.domain.chat.ChatBadgeKind
import com.gallbladderz.openkick.core.domain.chat.ChatEmoteParser
import com.gallbladderz.openkick.core.domain.chat.ChatMessage
import com.gallbladderz.openkick.core.domain.chat.ChatReplaySource
import com.gallbladderz.openkick.core.domain.chat.ReplyToInfo
import com.gallbladderz.openkick.core.network.chat.ChatReplayPageDto
import com.gallbladderz.openkick.features.player.CHAT_REPLAY_WINDOW_MS
import com.gallbladderz.openkick.features.player.ReplayChatMessage
import com.gallbladderz.openkick.features.player.fetchChatReplayWindow
import java.io.File
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Name of the chat file written next to the downloaded video, in the same directory. */
const val OFFLINE_CHAT_FILE_NAME = "chat.json"

/** One chat chunk covers one hour of the downloaded video. */
const val OFFLINE_CHAT_CHUNK_MS = 3_600_000L

/**
 * Maximum number of chat messages stored per one-hour chunk. A busy hour can
 * produce far more than this; the oldest messages (by offset) are kept first
 * and fetching stops once the cap is reached, so storage stays bounded
 * (~a few hundred KB per chunk). There is no longer a total cap across the
 * whole download — a 10h VOD can store up to 2,000 messages per hour.
 */
const val MAX_OFFLINE_CHAT_MESSAGES_PER_CHUNK = 2_000

/**
 * Maximum chat range fetched at download time (24h). Longer than any realistic
 * VOD and a guard against runaway API calls if the duration is ever unknown.
 * A full 10h chat download is still thousands of API calls (see
 * downloadChatReplayChunked) and may hit Kick rate limits — chunks already
 * written are kept, so a later re-download / retry only needs the missing ones.
 */
const val MAX_OFFLINE_CHAT_RANGE_MS = 24L * 60L * 60L * 1000L

private val offlineChatJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
data class StoredChatBadge(
    val name: String,
    val kind: String = "GLOBAL",
    val imageUrl: String? = null,
    val giftedSubscriptions: Int? = null
)

@Serializable
data class StoredReply(
    val messageId: String = "",
    val senderName: String = "",
    val content: String = ""
)

@Serializable
data class StoredChatMessage(
    /** Milliseconds from the start of the *downloaded* video (0 = download start). */
    val offsetMs: Long,
    val id: String,
    val sender: String,
    val avatarUrl: String = "",
    val senderColor: String = "",
    val content: String,
    val badges: List<StoredChatBadge> = emptyList(),
    val replyTo: StoredReply? = null
)

@Serializable
data class StoredChatReplay(
    val version: Int = 1,
    val messages: List<StoredChatMessage> = emptyList()
)

internal fun ReplayChatMessage.toStored(): StoredChatMessage = StoredChatMessage(
    offsetMs = offsetMs,
    id = message.id,
    sender = message.sender,
    avatarUrl = message.avatarUrl,
    senderColor = message.senderColor,
    content = message.content,
    badges = message.badges.map {
        StoredChatBadge(it.name, it.kind.name, it.imageUrl, it.giftedSubscriptions)
    },
    replyTo = message.replyTo?.let { StoredReply(it.messageId, it.senderName, it.content) }
)

internal fun StoredChatMessage.toReplay(): ReplayChatMessage = ReplayChatMessage(
    offsetMs = offsetMs,
    message = ChatMessage(
        id = id,
        sender = sender,
        avatarUrl = avatarUrl,
        senderColor = senderColor,
        content = content,
        // Tokens are derived from the content ([emote:id:name] markers), so they
        // are not stored; re-parsing on load keeps chat.json small.
        tokens = ChatEmoteParser.parseEmotes(content),
        badges = badges.map {
            ChatBadge(
                it.name,
                runCatching { ChatBadgeKind.valueOf(it.kind) }.getOrDefault(ChatBadgeKind.GLOBAL),
                it.imageUrl,
                it.giftedSubscriptions
            )
        },
        replyTo = replyTo?.let { ReplyToInfo(it.messageId, it.senderName, it.content) }
    )
)

/** Chat file inside a download directory, or next to a downloaded video file. */
fun offlineChatFile(directory: File): File = File(directory, OFFLINE_CHAT_FILE_NAME)

fun offlineChatFileForVideo(videoFile: File): File? =
    videoFile.parentFile?.let { offlineChatFile(it) }

/** Index of the one-hour chunk containing a download-relative [offsetMs]. */
fun offlineChatChunkIndex(offsetMs: Long): Int =
    (offsetMs.coerceAtLeast(0L) / OFFLINE_CHAT_CHUNK_MS).toInt()

/** File name of a chunk, e.g. `chat-0000.json` for the first hour, `chat-0009.json` for hour 9. */
fun offlineChatChunkFileName(chunkIndex: Int): String = "chat-%04d.json".format(chunkIndex)

fun offlineChatChunkFile(directory: File, chunkIndex: Int): File =
    File(directory, offlineChatChunkFileName(chunkIndex))

private val chunkFileRegex = Regex("""chat-(\d{4})\.json""")

/** Chunk index encoded in a chunk file name, or null for any other file. */
fun offlineChatChunkIndexForFile(file: File): Int? =
    chunkFileRegex.matchEntire(file.name)?.groupValues?.getOrNull(1)?.toIntOrNull()

/** All chunk files in a download directory, sorted by chunk index. */
fun listOfflineChatChunkFiles(directory: File): List<File> =
    directory.listFiles().orEmpty()
        .filter { offlineChatChunkIndexForFile(it) != null }
        .sortedBy { offlineChatChunkIndexForFile(it) }

/** Start (inclusive) of a chunk, in download-relative milliseconds. */
fun offlineChatChunkStartMs(chunkIndex: Int): Long = chunkIndex.toLong() * OFFLINE_CHAT_CHUNK_MS

/**
 * Chunk indices that should be in memory for playback at [positionMs]:
 * the current hour plus its neighbours, so a seek across an hour boundary
 * and messages just before/after the position are already loaded.
 */
fun offlineChatChunksNeeded(positionMs: Long): Set<Int> {
    val current = offlineChatChunkIndex(positionMs)
    return setOf(current - 1, current, current + 1).filter { it >= 0 }.toSet()
}

/** Chat source persisted on a download, if it was recorded at download time. */
fun DownloadEntity.chatReplaySource(): ChatReplaySource? =
    ChatReplaySource.from(channelId, chatStartedAt)

/** Convert an absolute offset (from the broadcast start) to a download-relative offset. */
fun rebaseChatOffset(absoluteOffsetMs: Long, startMs: Long): Long =
    (absoluteOffsetMs - startMs).coerceAtLeast(0)

/**
 * Resolve the chat range to fetch for a download, in milliseconds from the
 * broadcast start. Returns null when the end of the download is unknown
 * (no explicit range, no duration and no parseable duration string) — in that
 * case the caller should skip the chat download rather than fetch unboundedly.
 */
fun offlineChatRangeMs(
    startSec: Long?,
    endSec: Long?,
    durationSec: Long?,
    durationFormatted: String? = null
): Pair<Long, Long>? {
    val startMs = (startSec ?: 0L).coerceAtLeast(0L) * 1000L
    val endSecResolved = endSec ?: durationSec ?: parseDurationFormattedSeconds(durationFormatted)
        ?: return null
    val endMs = endSecResolved.coerceAtLeast(0L) * 1000L
    if (endMs <= startMs) return null
    return startMs to endMs
}

/** Parse a "MM:SS" / "H:MM:SS" duration string (as produced by formatTime) to seconds. */
fun parseDurationFormattedSeconds(value: String?): Long? {
    val parts = value?.trim()?.split(":")?.takeIf { it.size == 2 || it.size == 3 } ?: return null
    val numbers = parts.map { it.toLongOrNull() ?: return null }
    if (numbers.any { it < 0 }) return null
    if (numbers.size == 3 && (numbers[1] >= 60 || numbers[2] >= 60)) return null
    if (numbers.size == 2 && numbers[1] >= 60) return null
    return if (numbers.size == 3) numbers[0] * 3600 + numbers[1] * 60 + numbers[2]
    else numbers[0] * 60 + numbers[1]
}

/**
 * Fetch the full chat replay for [startMs, endMs) (absolute offsets from the
 * broadcast start), in 15s windows via [fetchChatReplayWindow], and return the
 * messages rebased to download-relative offsets (0 = download start),
 * de-duplicated by id, sorted by offset and capped at [maxMessages].
 * Callers must catch exceptions: a chat failure must never fail the video download.
 */
internal suspend fun downloadChatReplay(
    source: ChatReplaySource,
    startMs: Long,
    endMs: Long,
    maxMessages: Int = MAX_OFFLINE_CHAT_MESSAGES_PER_CHUNK,
    fetchPage: suspend (Instant) -> ChatReplayPageDto
): List<ReplayChatMessage> {
    if (endMs <= startMs) return emptyList()
    val cappedEnd = minOf(endMs, startMs + MAX_OFFLINE_CHAT_RANGE_MS)
    val collected = LinkedHashMap<String, ReplayChatMessage>()
    var windowStart = startMs
    while (windowStart < cappedEnd && collected.size < maxMessages) {
        val windowEnd = minOf(windowStart + CHAT_REPLAY_WINDOW_MS, cappedEnd)
        val window = fetchChatReplayWindow(source, windowStart, windowEnd, fetchPage)
        for (entry in window) {
            if (entry.message.id !in collected) {
                collected[entry.message.id] =
                    entry.copy(offsetMs = rebaseChatOffset(entry.offsetMs, startMs))
            }
            if (collected.size >= maxMessages) break
        }
        windowStart = windowEnd
    }
    return collected.values.sortedBy { it.offsetMs }
}

data class ChunkedChatDownloadResult(
    /** Chunk indices successfully written to disk, in order. */
    val chunksWritten: List<Int> = emptyList(),
    val totalMessages: Int = 0,
    /** First chunk that failed (fetch or write), or null when all succeeded. */
    val failedChunk: Int? = null
)

/**
 * Fetch the chat replay for [startMs, endMs) chunk-by-chunk (one hour per
 * chunk) and write each chunk to `chat-XXXX.json` in [directory] as soon as
 * it completes, instead of holding the whole chat in memory.
 *
 * A chunk is skipped (no file) when it has no messages. On the first failed
 * chunk — network error, rate limit, or write failure — fetching stops and
 * the chunks already written are kept, so the video still has partial chat
 * and a later re-download can pick up where it left off. Never throws except
 * for [kotlinx.coroutines.CancellationException].
 */
internal suspend fun downloadChatReplayChunked(
    source: ChatReplaySource,
    startMs: Long,
    endMs: Long,
    directory: File,
    fetchPage: suspend (Instant) -> ChatReplayPageDto
): ChunkedChatDownloadResult {
    if (endMs <= startMs) return ChunkedChatDownloadResult()
    val cappedEnd = minOf(endMs, startMs + MAX_OFFLINE_CHAT_RANGE_MS)
    // Chunk boundaries are download-relative: chunk 0 always starts at the
    // download start, even for a custom-range download.
    val firstChunk = 0
    val lastChunk = offlineChatChunkIndex(cappedEnd - startMs - 1)
    val written = mutableListOf<Int>()
    var total = 0
    for (chunk in firstChunk..lastChunk) {
        val chunkStart = startMs + offlineChatChunkStartMs(chunk)
        val chunkEnd = minOf(chunkStart + OFFLINE_CHAT_CHUNK_MS, cappedEnd)
        if (chunkEnd <= chunkStart) break
        val messages = try {
            downloadChatReplay(source, chunkStart, chunkEnd, fetchPage = fetchPage)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            return ChunkedChatDownloadResult(written, total, failedChunk = chunk)
        }
        if (messages.isNotEmpty()) {
            // downloadChatReplay rebases to its own start (the chunk start);
            // rebase again to the download start so chunk files share one
            // offset space and the player can filter by playback position.
            val rebased = messages.map {
                it.copy(offsetMs = it.offsetMs + offlineChatChunkStartMs(chunk))
            }
            val saved = saveOfflineChatReplay(offlineChatChunkFile(directory, chunk), rebased)
            if (!saved) return ChunkedChatDownloadResult(written, total, failedChunk = chunk)
            written += chunk
            total += rebased.size
        }
    }
    return ChunkedChatDownloadResult(written, total)
}

/** Load the chunk files overlapping [positionMs] (current hour ± 1), keyed by chunk index. */
internal suspend fun loadOfflineChatChunksForPosition(
    directory: File,
    positionMs: Long
): Map<Int, List<ReplayChatMessage>> {
    val needed = offlineChatChunksNeeded(positionMs)
    val result = LinkedHashMap<Int, List<ReplayChatMessage>>()
    for (index in needed) {
        val file = offlineChatChunkFile(directory, index)
        // A missing chunk file means that hour had no saved messages (or the
        // chunk failed); record an empty list so the caller does not retry
        // the disk read on every playback tick.
        result[index] = loadOfflineChatReplay(file).orEmpty()
    }
    return result
}

/** Write downloaded chat next to the video. Returns false (never throws) on I/O failure. */
internal suspend fun saveOfflineChatReplay(file: File, messages: List<ReplayChatMessage>): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            file.writeText(
                offlineChatJson.encodeToString(
                    StoredChatReplay.serializer(),
                    // Already capped per chunk by the caller; no total cap here.
                    StoredChatReplay(messages = messages.map { it.toStored() })
                )
            )
            true
        }.getOrDefault(false)
    }

/** Load a previously saved chat.json, or null when it is missing or unreadable. */
internal suspend fun loadOfflineChatReplay(file: File): List<ReplayChatMessage>? =
    withContext(Dispatchers.IO) {
        runCatching {
            if (!file.isFile) return@runCatching null
            offlineChatJson.decodeFromString(StoredChatReplay.serializer(), file.readText())
                .messages.map { it.toReplay() }.sortedBy { it.offsetMs }
        }.getOrNull()
    }

/**
 * Messages visible at [positionMs] during playback, in chronological order,
 * limited to the most recent [limit] (the panel reverses them for display,
 * like the online ChatReplayPanel).
 */
internal fun visibleOfflineChatMessages(
    messages: List<ReplayChatMessage>,
    positionMs: Long,
    limit: Int = 100
): List<ReplayChatMessage> =
    messages.filter { it.offsetMs <= positionMs.coerceAtLeast(0) }
        .sortedBy { it.offsetMs }
        .takeLast(limit.coerceAtLeast(0))
