/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.downloads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadRangesTest {
    @Test
    fun parsesTimecodesWithAndWithoutHours() {
        assertEquals(0L, parseDownloadTime("00:00"))
        assertEquals(65L, parseDownloadTime("01:05"))
        assertEquals(4_325L, parseDownloadTime("01:12:05"))
        assertEquals(7_205L, parseDownloadTime("120:05"))
    }

    @Test
    fun rejectsMalformedOutOfRangeAndOverflowingFields() {
        listOf("", "90", "1:4", "00:60", "1:60:00", "-1:00", "9999999999999999:00:00").forEach {
            assertNull(it, parseDownloadTime(it))
        }
    }

    @Test
    fun selectsOnlySegmentsIntersectingTheRequestedRange() {
        val durations = listOf(4_000_000L, 6_000_000L, 2_500_000L, 4_500_000L)
        assertEquals(listOf(1), downloadSegmentIndices(durations, 4, 10))
        assertEquals(listOf(0, 1, 2), downloadSegmentIndices(durations, 3, 11))
        assertEquals(listOf(2, 3), downloadSegmentIndices(durations, 12, 16))
        assertEquals(listOf(0, 1, 2, 3), downloadSegmentIndices(durations, null, null))
        assertEquals(emptyList<Int>(), downloadSegmentIndices(durations, 17, 20))
    }
}
