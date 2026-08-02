package com.gallbladderz.openkick.features.player

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
                    .add(Player.COMMAND_PLAY_PAUSE)
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
        super.onDestroy()
    }
}
