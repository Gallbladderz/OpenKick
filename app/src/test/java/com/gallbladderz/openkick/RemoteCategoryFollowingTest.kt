/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick

import com.gallbladderz.openkick.core.auth.AppMode
import com.gallbladderz.openkick.core.network.dto.following.RemoteFollowedCategory
import com.gallbladderz.openkick.features.following.visibleCategories
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteCategoryFollowingTest {
    @Test fun readsFollowedCategoriesAsAnArray() {
        val json = Json { ignoreUnknownKeys = true }
        val categories = json.decodeFromString<List<RemoteFollowedCategory>>(
            """[{"slug":"minecraft","name":"Minecraft","viewers":42,"banner":{"srcset":"image"}}]"""
        )

        assertEquals(listOf(RemoteFollowedCategory("minecraft", "Minecraft")), categories)
    }

    @Test fun hidesCategoriesFromPreviousSession() {
        val previous = 3L to listOf(RemoteFollowedCategory("minecraft"))

        assertEquals(previous.second, visibleCategories(AppMode.AUTHENTICATED, 3L, previous))
        assertTrue(visibleCategories(AppMode.AUTONOMOUS, 4L, previous).isEmpty())
        assertTrue(visibleCategories(AppMode.AUTHENTICATED, 5L, previous).isEmpty())
    }
}
