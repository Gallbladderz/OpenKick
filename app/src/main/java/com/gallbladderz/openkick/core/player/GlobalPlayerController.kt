/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.core.player


import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class PlayerExpandedState {
    HIDDEN, MINI, EXPANDED
}

sealed interface PlayerMedia {
    data class Live(val streamerName: String) : PlayerMedia

    sealed interface Recording : PlayerMedia {
        data class Clip(val id: String) : Recording
        data class Vod(val id: String, val channelSlug: String?) : Recording
        data class Download(val id: String) : Recording
    }
}

@Stable
class GlobalPlayerController {
    var playerState by mutableStateOf(PlayerExpandedState.HIDDEN)
        private set

    var currentMedia by mutableStateOf<PlayerMedia?>(null)
        private set

    fun expandPlayer(streamerName: String) = openPlayer(PlayerMedia.Live(streamerName))

    fun openRecording(recording: PlayerMedia.Recording) = openPlayer(recording)

    private fun openPlayer(media: PlayerMedia) {
        currentMedia = media
        playerState = PlayerExpandedState.EXPANDED
    }

    fun minimizePlayer() {
        if (playerState == PlayerExpandedState.EXPANDED) {
            playerState = PlayerExpandedState.MINI
        }
    }

    fun hidePlayer() {
        playerState = PlayerExpandedState.HIDDEN
        currentMedia = null
    }

    fun updatePlayerState(state: PlayerExpandedState) {
        if (state == PlayerExpandedState.HIDDEN) hidePlayer() else playerState = state
    }
}

val LocalGlobalPlayerController = compositionLocalOf<GlobalPlayerController> {
    error("No GlobalPlayerController provided")
}
