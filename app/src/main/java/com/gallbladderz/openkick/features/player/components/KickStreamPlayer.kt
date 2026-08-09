/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import android.view.LayoutInflater
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.gallbladderz.openkick.R

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun KickStreamPlayer(player: Player, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            val view = LayoutInflater.from(context)
                .inflate(R.layout.view_kick_player, null, false) as PlayerView
            view.apply {
                keepScreenOn = true
                useController = false
            }
        },
        update = { view ->
            view.player = player
        },
        onRelease = { view ->
            view.player = null
        },
        modifier = modifier
    )
}

