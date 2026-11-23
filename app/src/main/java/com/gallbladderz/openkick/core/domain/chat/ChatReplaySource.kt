/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.domain.chat

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class ChatReplaySource(val channelId: Long, val startedAt: Instant) {
    companion object {
        fun from(channelId: Long?, startedAt: String?): ChatReplaySource? {
            if (channelId == null || channelId <= 0) return null
            return parseChatTimestamp(startedAt)?.let { ChatReplaySource(channelId, it) }
        }
    }
}

internal fun parseChatTimestamp(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value) }.getOrElse {
        runCatching { LocalDateTime.parse(value.replace(' ', 'T')).toInstant(ZoneOffset.UTC) }.getOrNull()
    }
}
