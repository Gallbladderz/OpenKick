/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player

import com.gallbladderz.openkick.core.data.mapper.toDomainModel
import com.gallbladderz.openkick.core.domain.chat.ChatMessage
import com.gallbladderz.openkick.core.domain.chat.ChatReplaySource
import com.gallbladderz.openkick.core.domain.chat.parseChatTimestamp
import com.gallbladderz.openkick.core.network.chat.ChatReplayPageDto
import java.time.Instant
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal const val CHAT_REPLAY_WINDOW_MS = 15_000L
private const val PAGE_SECONDS = 5L
private const val PAGE_MESSAGE_LIMIT = 25

internal data class ReplayChatMessage(val offsetMs: Long, val message: ChatMessage)

internal suspend fun fetchChatReplayWindow(
    source: ChatReplaySource,
    startMs: Long,
    endMs: Long,
    fetchPage: suspend (Instant) -> ChatReplayPageDto
): List<ReplayChatMessage> {
    val start = source.startedAt.plusMillis(startMs)
    val end = source.startedAt.plusMillis(endMs)
    val messages = mutableMapOf<String, ReplayChatMessage>()
    var next = start
    while (next < end) {
        currentCoroutineContext().ensureActive()
        val page = fetchPage(next)
        for (dto in page.messages) {
            val id = dto.id?.takeIf { it.isNotBlank() } ?: continue
            val timestamp = parseChatTimestamp(dto.createdAt) ?: continue
            if (timestamp >= start && timestamp < end && id !in messages) {
                messages[id] = ReplayChatMessage(
                    timestamp.toEpochMilli() - source.startedAt.toEpochMilli(),
                    dto.toDomainModel()
                )
            }
        }

        val windowEnd = next.plusSeconds(PAGE_SECONDS)
        val cursor = page.cursor?.toLongOrNull()?.takeIf { it > 0 }?.let {
            Instant.ofEpochSecond(it / 1_000_000, it % 1_000_000 * 1_000).plusNanos(1_000)
        }
        next = if (page.messages.size >= PAGE_MESSAGE_LIMIT && cursor != null && cursor > next) {
            minOf(cursor, windowEnd)
        } else {
            windowEnd
        }
    }
    return messages.values.sortedBy { it.offsetMs }
}
