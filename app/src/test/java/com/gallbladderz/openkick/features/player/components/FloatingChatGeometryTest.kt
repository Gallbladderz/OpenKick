/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingChatGeometryTest {

    @Test
    fun defaultGeometryIsTallVerticalSidebarDockedRight() {
        // Landscape phone, e.g. 800x360dp fullscreen: 30% wide, clamped
        // to the 260dp default minimum, 92% tall, docked right.
        val geometry = defaultFloatingChatGeometry(800f, 360f)
        assertEquals(260f, geometry.widthDp, 0.01f)
        assertEquals(360f * 0.92f, geometry.heightDp, 0.01f)
        assertEquals(800f - 260f - 8f, geometry.offsetXDp, 0.01f)
        assertEquals((360f - 360f * 0.92f) / 2f, geometry.offsetYDp, 0.01f)
    }

    @Test
    fun defaultGeometryNeverGoesBelowMinimumsOnSmallScreens() {
        val geometry = defaultFloatingChatGeometry(300f, 200f)
        assertEquals(260f, geometry.widthDp, 0.01f)
        assertEquals(184f, geometry.heightDp, 0.01f)
        assertEquals(300f - 260f - 8f, geometry.offsetXDp, 0.01f)
    }

    @Test
    fun clampOffsetKeepsPanelFullyOnScreen() {
        assertEquals(0f to 0f, clampFloatingChatOffset(-50f, -20f, 300f, 200f, 800f, 360f))
        assertEquals(500f to 160f, clampFloatingChatOffset(900f, 900f, 300f, 200f, 800f, 360f))
        assertEquals(100f to 50f, clampFloatingChatOffset(100f, 50f, 300f, 200f, 800f, 360f))
    }

    @Test
    fun clampOffsetHandlesPanelLargerThanScreen() {
        // Panel wider than the screen: only offset 0 keeps it as visible as possible.
        assertEquals(0f to 0f, clampFloatingChatOffset(100f, 100f, 900f, 400f, 800f, 360f))
    }

    @Test
    fun clampSizeEnforcesMinimumAndFiftyFivePercentMaximumWidth() {
        assertEquals(240f to 140f, clampFloatingChatSize(50f, 50f, 800f, 360f))
        // Width stops at 55% of the screen, height at the screen height.
        assertEquals(440f to 360f, clampFloatingChatSize(2000f, 2000f, 800f, 360f))
        assertEquals(400f to 250f, clampFloatingChatSize(400f, 250f, 800f, 360f))
    }

    @Test
    fun resizeAppliesDeltaAndClamps() {
        assertEquals(340f to 272f, resizeFloatingChat(300f, 200f, 40f, 72f, 800f, 360f))
        // Shrinking below the minimum stops at the new 240dp minimum.
        assertEquals(240f to 140f, resizeFloatingChat(220f, 160f, -100f, -100f, 800f, 360f))
        // Growing beyond the screen stops at 55% width / full height.
        assertEquals(440f to 360f, resizeFloatingChat(700f, 300f, 500f, 500f, 800f, 360f))
    }

    @Test
    fun sizeStepGrowsAndShrinksByFortyDp() {
        assertEquals(340f to 240f, stepFloatingChatSize(300f, 200f, growing = true, 800f, 360f))
        assertEquals(260f to 160f, stepFloatingChatSize(300f, 200f, growing = false, 800f, 360f))
        assertEquals(240f to 140f, stepFloatingChatSize(210f, 150f, growing = false, 800f, 360f))
    }

    @Test
    fun dragDeltasApplyToLatestGeometryInSequence() {
        // Regression test for the drag bug: a continuous drag is a series
        // of deltas, each applied to the geometry produced by the previous
        // one — NOT every delta applied to the geometry from drag start.
        // The old handler restarted on every geometry change, so only the
        // first tiny delta ever took effect and the panel barely moved.
        // Start mid-screen (not docked to an edge) so no intermediate
        // delta hits the screen clamp and hides a wrong fold: the net
        // delta is (-5, +10), so the expected offset is (295, 60).
        val start = FloatingChatGeometry(300f, 50f, 280f, 200f)
        val deltas = listOf(Offset(10f, 5f), Offset(-20f, 10f), Offset(5f, -5f))
        val result = applyDragDeltas(start, deltas, 800f, 360f)
        assertEquals(295f, result.offsetXDp, 0.01f)
        assertEquals(60f, result.offsetYDp, 0.01f)
        assertEquals(start.widthDp, result.widthDp, 0.01f)
        assertEquals(start.heightDp, result.heightDp, 0.01f)
        // Applying the same deltas one at a time gives the same result.
        var manual = start
        for (delta in deltas) manual = moveFloatingChat(manual, delta.x, delta.y, 800f, 360f)
        assertEquals(result, manual)
    }

    @Test
    fun dragDeltasAreClampedToTheScreen() {
        val start = FloatingChatGeometry(512f, 14.4f, 280f, 331.2f)
        val result = applyDragDeltas(start, listOf(Offset(1000f, 1000f)), 800f, 360f)
        assertEquals(520f, result.offsetXDp, 0.01f) // 800 - 280
        assertEquals(28.8f, result.offsetYDp, 0.01f) // 360 - 331.2
    }

    @Test
    fun snapToEdgeDocksLeftAndRight() {
        val screenW = 800f
        val screenH = 360f
        // Near the left edge: docks left, full-height vertical panel.
        val left = snapFloatingChatToEdge(
            FloatingChatGeometry(20f, 100f, 280f, 200f), screenW, screenH
        )
        assertEquals(8f, left.offsetXDp, 0.01f)
        assertEquals(screenH * 0.92f, left.heightDp, 0.01f)
        assertEquals((screenH - screenH * 0.92f) / 2f, left.offsetYDp, 0.01f)
        // Near the right edge: docks right.
        val right = snapFloatingChatToEdge(
            FloatingChatGeometry(500f, 100f, 280f, 200f), screenW, screenH
        )
        assertEquals(800f - 280f - 8f, right.offsetXDp, 0.01f)
        // Middle of the screen: no snap.
        val middle = snapFloatingChatToEdge(
            FloatingChatGeometry(200f, 100f, 280f, 200f), screenW, screenH
        )
        assertEquals(200f, middle.offsetXDp, 0.01f)
        assertEquals(200f, middle.heightDp, 0.01f)
    }

    @Test
    fun dockLeftAndRightProduceVerticalSidebars() {
        val start = FloatingChatGeometry(200f, 100f, 300f, 200f)
        val left = dockFloatingChat(start, dockLeft = true, 800f, 360f)
        assertEquals(8f, left.offsetXDp, 0.01f)
        assertEquals(360f * 0.92f, left.heightDp, 0.01f)
        val right = dockFloatingChat(start, dockLeft = false, 800f, 360f)
        assertEquals(800f - 300f - 8f, right.offsetXDp, 0.01f)
        assertEquals(360f * 0.92f, right.heightDp, 0.01f)
    }

    @Test
    fun leftEdgeResizeGrowsWhenDraggedLeftAndShrinksWhenDraggedRight() {
        val start = FloatingChatGeometry(512f, 14.4f, 280f, 331.2f)
        // Drag the left edge 40dp left: panel grows, left edge moves left.
        val grown = resizeFloatingChatFromLeftEdge(start, -40f, 800f, 360f)
        assertEquals(320f, grown.widthDp, 0.01f)
        assertEquals(472f, grown.offsetXDp, 0.01f)
        assertEquals(14.4f, grown.offsetYDp, 0.01f)
        // Drag the left edge 40dp right: panel shrinks, left edge moves right.
        val shrunk = resizeFloatingChatFromLeftEdge(start, 40f, 800f, 360f)
        assertEquals(240f, shrunk.widthDp, 0.01f)
        assertEquals(552f, shrunk.offsetXDp, 0.01f)
        // Shrinking past the minimum stops at 240dp and the offset only
        // moves by the actual width change.
        val clamped = resizeFloatingChatFromLeftEdge(start, 100f, 800f, 360f)
        assertEquals(240f, clamped.widthDp, 0.01f)
        assertEquals(552f, clamped.offsetXDp, 0.01f)
    }

    @Test
    fun rightEdgeResizeGrowsWhenDraggedRight() {
        val start = FloatingChatGeometry(8f, 14.4f, 280f, 331.2f)
        val grown = resizeFloatingChatFromRightEdge(start, 40f, 800f, 360f)
        assertEquals(320f, grown.widthDp, 0.01f)
        assertEquals(8f, grown.offsetXDp, 0.01f)
        val shrunk = resizeFloatingChatFromRightEdge(start, -100f, 800f, 360f)
        assertEquals(240f, shrunk.widthDp, 0.01f)
        assertEquals(8f, shrunk.offsetXDp, 0.01f)
    }
}
