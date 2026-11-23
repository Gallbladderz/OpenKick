/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick

import com.gallbladderz.openkick.core.network.dto.profile.VideoPlaybackResponse
import com.gallbladderz.openkick.features.profile.toDomain
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoPlaybackResponseTest {
    @Test
    fun acceptsThumbnailUrlString() {
        val response = responseWithThumbnail("\"https:\\/\\/example.com\\/thumbnail.jpg\"")

        assertEquals("https://example.com/video.m3u8", response.source)
        assertEquals("https://example.com/thumbnail.jpg", response.livestream?.toDomain()?.thumbnailUrl)
    }

    @Test
    fun acceptsThumbnailObjectAndKeepsItsFallbacks() {
        val response = responseWithThumbnail(
            """{"src":"","url":"https://example.com/thumbnail.jpg"}"""
        )
        val responsive = responseWithThumbnail(
            """{"srcSet":"https://example.com/small.jpg 120w, https://example.com/large.jpg 360w"}"""
        )

        assertEquals("https://example.com/thumbnail.jpg", response.livestream?.toDomain()?.thumbnailUrl)
        assertEquals("https://example.com/small.jpg", responsive.livestream?.toDomain()?.thumbnailUrl)
    }

    @Test
    fun ignoresUnrecognizedThumbnailWithoutBreakingPlaybackResponse() {
        val response = responseWithThumbnail("""{"url":[]}""")

        assertEquals("https://example.com/video.m3u8", response.source)
        assertEquals("", response.livestream?.toDomain()?.thumbnailUrl)
    }

    private fun responseWithThumbnail(thumbnail: String): VideoPlaybackResponse =
        Json.decodeFromString(
            """
            {
                "source": "https://example.com/video.m3u8",
                "livestream": {"id": 123, "thumbnail": $thumbnail}
            }
            """.trimIndent()
        )
}
