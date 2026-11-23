/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.following

import com.gallbladderz.openkick.core.network.dto.following.RemoteFollowedChannel
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertSame
import org.junit.Test

class FollowMigrationTest {
    @Test
    fun confirmedWriteDoesNotDependOnReloadingSubscriptions() = runBlocking {
        confirmChannelFollow("channel", follow = {}, reload = { error("Unexpected reload") })
    }

    @Test
    fun interruptedResponseIsSuccessWhenRemoteListContainsChannel() = runBlocking {
        confirmChannelFollow(
            "channel",
            follow = { throw IOException("Connection closed after write") },
            reload = { listOf(RemoteFollowedChannel("CHANNEL")) }
        )
    }

    @Test
    fun preservesErrorIfChannelStillHasNotBeenFollowed() = runBlocking {
        val expected = IOException("Write failed")
        val error = runCatching {
            confirmChannelFollow("channel", follow = { throw expected }, reload = { emptyList() })
        }.exceptionOrNull()
        assertSame(expected, error)
    }

    @Test
    fun cancellationDoesNotStartAnotherRequest() = runBlocking {
        val expected = CancellationException("Account changed")
        val error = runCatching {
            confirmChannelFollow("channel", follow = { throw expected }, reload = { error("Unexpected reload") })
        }.exceptionOrNull()
        assertSame(expected, error)
    }
}
