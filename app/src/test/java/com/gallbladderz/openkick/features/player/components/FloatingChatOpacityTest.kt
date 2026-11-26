/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingChatOpacityTest {

    @Test
    fun opacityCyclesTransparentSemiSolidAndBack() {
        val start = FloatingChatOpacity.TRANSPARENT
        assertEquals(FloatingChatOpacity.SEMI, start.next())
        assertEquals(FloatingChatOpacity.SOLID, start.next().next())
        assertEquals(FloatingChatOpacity.TRANSPARENT, start.next().next().next())
    }

    @Test
    fun transparentModeHasNoPanelFillOrBorder() {
        assertEquals(0f, floatingChatPanelBackgroundAlpha(FloatingChatOpacity.TRANSPARENT), 0.001f)
        assertEquals(0f, floatingChatPanelBorderAlpha(FloatingChatOpacity.TRANSPARENT), 0.001f)
    }

    @Test
    fun semiAndSolidModesDarkenProgressively() {
        assertEquals(0.35f, floatingChatPanelBackgroundAlpha(FloatingChatOpacity.SEMI), 0.001f)
        assertEquals(0.15f, floatingChatPanelBorderAlpha(FloatingChatOpacity.SEMI), 0.001f)
        assertEquals(0.75f, floatingChatPanelBackgroundAlpha(FloatingChatOpacity.SOLID), 0.001f)
        assertEquals(0.25f, floatingChatPanelBorderAlpha(FloatingChatOpacity.SOLID), 0.001f)
    }

    @Test
    fun scrimFadesFromClearAtTopToDarkAtBottomInTransparentMode() {
        // TikTok-style: the video stays fully visible at the top of the
        // panel; only the bottom (newest messages) gets a dark scrim.
        assertEquals(0f, floatingChatScrimTopAlpha(FloatingChatOpacity.TRANSPARENT), 0.001f)
        assertEquals(0.45f, floatingChatScrimBottomAlpha(FloatingChatOpacity.TRANSPARENT), 0.001f)
        assertTrue(
            floatingChatScrimBottomAlpha(FloatingChatOpacity.TRANSPARENT) >
                floatingChatScrimTopAlpha(FloatingChatOpacity.TRANSPARENT)
        )
    }

    @Test
    fun semiModeKeepsAScrimAndSolidModeNeedsNone() {
        assertEquals(0.10f, floatingChatScrimTopAlpha(FloatingChatOpacity.SEMI), 0.001f)
        assertEquals(0.50f, floatingChatScrimBottomAlpha(FloatingChatOpacity.SEMI), 0.001f)
        assertEquals(0f, floatingChatScrimTopAlpha(FloatingChatOpacity.SOLID), 0.001f)
        assertEquals(0f, floatingChatScrimBottomAlpha(FloatingChatOpacity.SOLID), 0.001f)
    }

    @Test
    fun headerScrimKeepsIconsLegibleWithoutASolidBar() {
        assertEquals(0.30f, floatingChatHeaderScrimTopAlpha(FloatingChatOpacity.TRANSPARENT), 0.001f)
        assertEquals(0.25f, floatingChatHeaderScrimTopAlpha(FloatingChatOpacity.SEMI), 0.001f)
        assertEquals(0f, floatingChatHeaderScrimTopAlpha(FloatingChatOpacity.SOLID), 0.001f)
    }

    @Test
    fun textNeedsShadowWheneverVideoShowsThrough() {
        assertTrue(floatingChatReadableOverVideo(FloatingChatOpacity.TRANSPARENT))
        assertTrue(floatingChatReadableOverVideo(FloatingChatOpacity.SEMI))
        assertFalse(floatingChatReadableOverVideo(FloatingChatOpacity.SOLID))
    }
}
