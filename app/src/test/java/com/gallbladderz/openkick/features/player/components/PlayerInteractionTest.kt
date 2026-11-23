/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerInteractionTest {
    @Test
    fun locatesEmotesInTheRawDraftAndInTheEditorLayout() {
        val source = "hi [emote:41:Wave] there [emote:42:Hey]"
        val emotes = draftEmotes(source)
        assertEquals(listOf(3, 11), emotes.map { it.displayOffset })
        assertEquals(listOf("[emote:41:Wave]", "[emote:42:Hey]"), emotes.map { source.substring(it.sourceRange) })
        assertEquals(listOf("41", "42"), emotes.map { it.id })
    }

    @Test
    fun preservesUnicodeEmojiAndSkipsIncompleteKickTokens() {
        val source = "😊[emote:41:Wave][emote:42:Hey][emote:43:unfinished"
        assertEquals(listOf(2, 3), draftEmotes(source).map { it.displayOffset })
    }

    @Test
    fun onlyCenterClaimsFullscreenGesturesInPortrait() {
        assertNull(playerGestureZone(0.2f, fullscreen = false))
        assertEquals(PlayerGestureZone.Fullscreen, playerGestureZone(0.5f, fullscreen = false))
        assertNull(playerGestureZone(0.8f, fullscreen = false))
        assertEquals(PlayerGestureZone.Brightness, playerGestureZone(0.2f, fullscreen = true))
        assertEquals(PlayerGestureZone.Volume, playerGestureZone(0.8f, fullscreen = true))
    }

    @Test
    fun capsLoudnessBoostAtTwiceTheSignalAmplitude() {
        assertEquals(0, volumeBoostMillibels(50f))
        assertEquals(352, volumeBoostMillibels(150f))
        assertEquals(602, volumeBoostMillibels(200f))
        assertEquals(602, volumeBoostMillibels(250f))
    }
}
