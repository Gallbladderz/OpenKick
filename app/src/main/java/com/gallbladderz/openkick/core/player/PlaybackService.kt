/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.core.player


import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import org.koin.android.ext.android.inject

@UnstableApi
class PlaybackService : MediaSessionService() {

    private val playerManager: PlayerManager by inject()
    private var mediaSession: MediaSession? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PIP_AUDIO_ONLY) {
            playerManager.setAudioOnly()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()

        playerManager.initializePlayer()
        val player = playerManager.player ?: return

        val customCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                super.onConnect(session, controller)
                val availableSessionCommands = androidx.media3.session.SessionCommands.Builder()
                    .build()

                val availablePlayerCommands = androidx.media3.common.Player.Commands.Builder()
                    .addAllCommands()
                    .remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .remove(Player.COMMAND_SEEK_TO_NEXT)
                    .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .remove(Player.COMMAND_SEEK_FORWARD)
                    .remove(Player.COMMAND_SEEK_BACK)
                    .build()

                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(availableSessionCommands)
                    .setAvailablePlayerCommands(availablePlayerCommands)
                    .build()
            }
        }

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(customCallback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        playerManager.release()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PIP_AUDIO_ONLY = "com.gallbladderz.openkick.action.PIP_AUDIO_ONLY"
    }
}
