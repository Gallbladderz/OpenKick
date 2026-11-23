/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player

import com.gallbladderz.openkick.core.domain.chat.ChatModeType
import com.gallbladderz.openkick.core.network.dto.player.ChannelStreamInfoResponse
import com.gallbladderz.openkick.core.network.dto.player.ChatroomDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowersOnlyChatTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun readsFollowersOnlyFromKickChatroom() {
        val mode = chatMode("""{"followers_mode":{"enabled":true,"min_duration":0}}""")
        assertTrue(mode.followersOnly)
    }

    @Test
    fun keepsSlowModeWhenChatAlsoRequiresFollowing() {
        val mode = chatMode("""{"followers_mode":true,"slow_mode":{"enabled":true,"message_interval":15}}""")
        assertTrue(mode.followersOnly)
        assertEquals(ChatModeType.SLOW, mode.type)
        assertEquals(15, mode.slowModeSeconds)
    }

    @Test
    fun distinguishesDisabledFollowersModeFromPaidSubscribersMode() {
        val mode = chatMode("""{"followers_mode":{"enabled":false},"subscribers_mode":{"enabled":true}}""")
        assertFalse(mode.followersOnly)
        assertEquals(ChatModeType.SUBSCRIBERS, mode.type)
    }

    private fun chatMode(payload: String) = requireNotNull(
        ChannelStreamInfoResponse(playback_url = "https://example.com/live.m3u8")
            .toDomain(json.decodeFromString<ChatroomDto>(payload))
    ).chatMode
}
