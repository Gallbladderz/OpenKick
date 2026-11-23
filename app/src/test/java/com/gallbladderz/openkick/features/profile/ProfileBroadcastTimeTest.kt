/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.profile

import com.gallbladderz.openkick.core.network.dto.profile.ChannelV1Livestream
import com.gallbladderz.openkick.core.network.dto.profile.ChannelV1Response
import com.gallbladderz.openkick.core.network.dto.profile.PreviousLivestreamDto
import com.gallbladderz.openkick.core.network.dto.profile.VideoItemDto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileBroadcastTimeTest {
    private val now = Instant.parse("2026-11-23T12:00:00Z")
    private val zone = ZoneOffset.UTC

    @Test
    fun calculatesEndFromExplicitEndOrStartAndMilliseconds() {
        assertEquals(
            Instant.parse("2026-11-22T06:08:27Z"),
            broadcastEndTime("2026-11-21 15:54:57", null, null, 51_210_000)
        )
        assertEquals(
            Instant.parse("2026-11-22T13:00:00Z"),
            broadcastEndTime(null, "2026-11-22 12:00:00", "2026-11-22T16:00:00+03:00", 10)
        )
        assertEquals(
            Instant.parse("2026-11-22T12:00:10Z"),
            broadcastEndTime("invalid", "2026-11-22 12:00:00", null, 10_000)
        )
        assertNull(broadcastEndTime(null, null, null, 1000))
    }

    @Test
    fun mapsLatestFinishedStreamAndMakesVodEndAvailableAsFallback() {
        val older = PreviousLivestreamDto(startedAt = "2026-11-21 10:00:00")
        val newest = PreviousLivestreamDto(startedAt = "2026-11-22 10:00:00", duration = 30_000)
        val live = PreviousLivestreamDto(startedAt = "2026-11-23 10:00:00", isLive = true)
        val response = ChannelV1Response(id = 1, previousLivestreams = listOf(older, newest, live))
        val expected = Instant.parse("2026-11-22T10:00:30Z")
        assertEquals(expected, response.toDomain("channel", false)?.lastBroadcastAt)
        assertNull(response.copy(livestream = ChannelV1Livestream()).toDomain("channel", false)?.lastBroadcastAt)
        assertEquals(
            expected,
            VideoItemDto(id = 1, start_time = newest.startedAt, duration = 30_000).toDomain()?.broadcastEndedAt
        )
    }

    @Test
    fun decodesChannelVerificationObjectAndSocialHandles() {
        val channel = Json.decodeFromString<ChannelV1Response>(
            """{"id":16957782,"verified":{"id":7852},"user":{"youtube":"madgostream"}}"""
        ).toDomain("maddyson", false)
        assertEquals(true, channel?.isVerified)
        assertEquals(
            listOf(ProfileSocialLink(SocialPlatform.YouTube, "https://youtube.com/@madgostream")),
            channel?.socialLinks
        )
    }

    @Test
    fun selectsRelativeUnitsAndFormatsOlderDates() {
        assertEquals(LastBroadcastTime.JustNow, lastBroadcastTime(now.minusSeconds(20), now, zone))
        assertEquals(LastBroadcastTime.Minutes(3), lastBroadcastTime(now.minusSeconds(220), now, zone))
        assertEquals(LastBroadcastTime.Hours(2), lastBroadcastTime(now.minusSeconds(7200), now, zone))
        assertEquals(LastBroadcastTime.Days(3), lastBroadcastTime(now.minusSeconds(259_200), now, zone))
        val time = lastBroadcastTime(Instant.parse("2026-11-14T12:30:00Z"), now, zone)
        assertEquals(LastBroadcastTime.Date(LocalDate.of(2026, 11, 14), false), time)
        assertEquals("Nov 14", (time as LastBroadcastTime.Date).format("MMM d", Locale.US))
        assertEquals("11月14日", time.format("M月d日", Locale.SIMPLIFIED_CHINESE))
        assertEquals(
            LastBroadcastTime.Date(LocalDate.of(2025, 11, 14), true),
            lastBroadcastTime(Instant.parse("2025-11-14T12:30:00Z"), now, zone)
        )
        assertNull(lastBroadcastTime(now.plusSeconds(1), now, zone))
        assertNull(lastBroadcastTime(null, now, zone))
    }
}
