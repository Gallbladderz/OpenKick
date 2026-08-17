/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class PlayerExpandedState {
    HIDDEN, MINI, EXPANDED
}

@Stable
class GlobalPlayerController {
    var playerState by mutableStateOf(PlayerExpandedState.HIDDEN)
        private set

    var currentStreamerName by mutableStateOf<String?>(null)
        private set

    fun expandPlayer(streamerName: String) {
        currentStreamerName = streamerName
        playerState = PlayerExpandedState.EXPANDED
    }

    fun minimizePlayer() {
        if (playerState == PlayerExpandedState.EXPANDED) {
            playerState = PlayerExpandedState.MINI
        }
    }

    fun hidePlayer() {
        playerState = PlayerExpandedState.HIDDEN
        currentStreamerName = null
    }

    fun updatePlayerState(state: PlayerExpandedState) {
        playerState = state
    }
}

val LocalGlobalPlayerController = compositionLocalOf<GlobalPlayerController> {
    error("No GlobalPlayerController provided")
}
