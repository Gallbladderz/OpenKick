/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.profile

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun broadcastEndTime(
    startedAt: String?,
    createdAt: String?,
    endedAt: String?,
    durationMs: Long
): Instant? {
    val start = parseTimestamp(startedAt) ?: parseTimestamp(createdAt)
    val end = parseTimestamp(endedAt)
    if (end != null && (start == null || end >= start)) return end
    return start?.plusMillis(durationMs.coerceAtLeast(0))
}

private fun parseTimestamp(value: String?): Instant? {
    val iso = value?.trim()?.replace(' ', 'T') ?: return null
    return runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrElse {
        runCatching { LocalDateTime.parse(iso).toInstant(ZoneOffset.UTC) }.getOrNull()
    }
}

internal sealed interface LastBroadcastTime {
    data object JustNow : LastBroadcastTime
    data class Minutes(val amount: Int) : LastBroadcastTime
    data class Hours(val amount: Int) : LastBroadcastTime
    data class Days(val amount: Int) : LastBroadcastTime
    data class Date(val date: LocalDate, val includeYear: Boolean) : LastBroadcastTime {
        fun format(pattern: String, locale: Locale): String =
            date.format(DateTimeFormatter.ofPattern(pattern, locale))
    }
}

internal fun lastBroadcastTime(endedAt: Instant?, now: Instant, zone: ZoneId): LastBroadcastTime? {
    if (endedAt == null || endedAt > now) return null
    val elapsed = Duration.between(endedAt, now)
    return when {
        elapsed.toMinutes() == 0L -> LastBroadcastTime.JustNow
        elapsed.toHours() == 0L -> LastBroadcastTime.Minutes(elapsed.toMinutes().toInt())
        elapsed.toDays() == 0L -> LastBroadcastTime.Hours(elapsed.toHours().toInt())
        elapsed.toDays() < 7 -> LastBroadcastTime.Days(elapsed.toDays().toInt())
        else -> {
            val date = endedAt.atZone(zone).toLocalDate()
            LastBroadcastTime.Date(date, date.year != now.atZone(zone).year)
        }
    }
}
