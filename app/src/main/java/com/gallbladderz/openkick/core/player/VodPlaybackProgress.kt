/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.player

import android.content.Context
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun vodProgressKey(videoId: String, startSec: Long? = null, endSec: Long? = null): String =
    if (startSec != null && endSec != null) "${videoId.lowercase()}:$startSec-$endSec"
    else videoId.lowercase()

class VodPlaybackProgress(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("vod_playback_progress", Context.MODE_PRIVATE)

    suspend fun position(key: String): Long = withContext(Dispatchers.IO) {
        preferences.getLong(key, 0L).coerceAtLeast(0L)
    }

    fun track(player: Player, key: String, scope: CoroutineScope): AutoCloseable {
        var lastSavedPosition = player.currentPosition

        fun save() {
            val duration = player.duration
            if (duration <= 0L || player.playerError != null) return

            val position = player.currentPosition
            val bookmark = if (player.playbackState == Player.STATE_ENDED || position >= duration) {
                0L
            } else {
                position.coerceAtLeast(0L)
            }
            if (bookmark == lastSavedPosition) return
            lastSavedPosition = bookmark
            val editor = preferences.edit()
            if (bookmark == 0L) editor.remove(key) else editor.putLong(key, bookmark)
            editor.apply()
        }

        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_POSITION_DISCONTINUITY) ||
                    events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) ||
                    player.playbackState == Player.STATE_ENDED
                ) {
                    save()
                }
            }
        }
        player.addListener(listener)
        val checkpoint = scope.launch {
            while (true) {
                delay(5_000)
                if (player.isPlaying) save()
            }
        }
        return AutoCloseable {
            checkpoint.cancel()
            player.removeListener(listener)
            save()
        }
    }
}
