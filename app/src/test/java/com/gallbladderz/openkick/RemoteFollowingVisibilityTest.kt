/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick

import com.gallbladderz.openkick.core.auth.AppMode
import com.gallbladderz.openkick.core.network.dto.following.RemoteFollowedChannel
import com.gallbladderz.openkick.features.following.visibleChannels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteFollowingVisibilityTest {
    private val previous = 3L to listOf(RemoteFollowedChannel("previous"))

    @Test fun hidesPreviousAccountAfterSignOutAndLogin() {
        assertEquals(previous.second, visibleChannels(AppMode.AUTHENTICATED, 3L, previous))
        assertTrue(visibleChannels(AppMode.AUTONOMOUS, 4L, previous).isEmpty())
        assertTrue(visibleChannels(AppMode.AUTHENTICATED, 5L, previous).isEmpty())
    }

    @Test fun hidesUnloadedSubscriptions() {
        assertTrue(visibleChannels(AppMode.AUTHENTICATED, 1L, null).isEmpty())
    }
}
