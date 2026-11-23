/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player

import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import com.gallbladderz.openkick.core.player.PlayerManager
import com.gallbladderz.openkick.features.player.models.LivePlaybackMode
import com.gallbladderz.openkick.features.player.models.LivePlaybackState
import com.gallbladderz.openkick.features.player.models.LiveStreamArchive
import com.gallbladderz.openkick.features.player.models.StreamInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@UnstableApi
internal class LiveStreamPlayback(
    private val manager: PlayerManager,
    private val repository: PlayerRepository,
    private val scope: CoroutineScope
) {
    private var session: LiveSession? = null
    private val _state = MutableStateFlow(LivePlaybackState())
    val state = _state.asStateFlow()

    fun open(streamerName: String, stream: StreamInfo) {
        close()
        val player = manager.player ?: return
        _state.value = LivePlaybackState(startedAtMs = stream.startedAtMs)
        session = LiveSession(player, streamerName, stream).also { it.start() }
    }

    fun close() {
        session?.close()
        session = null
        _state.value = LivePlaybackState()
    }

    fun seekTo(positionMs: Long) {
        session?.seekTo(positionMs)
    }

    fun goLive() {
        session?.goLive()
    }

    fun resume() {
        if (_state.value.mode == LivePlaybackMode.LIVE && manager.player?.playWhenReady == false) {
            manager.seekToLiveEdge()
        }
        manager.resume()
    }

    private inner class LiveSession(
        private val player: Player,
        private val streamerName: String,
        private val stream: StreamInfo
    ) : Player.Listener {
        private var archive: LiveStreamArchive? = null
        private var lookup: Job? = null
        private var continuation: Job? = null
        private var liveUrl = stream.playbackUrl
        private var lastArchiveEndMs: Long? = null
        private var openingArchiveAtMs: Long? = null
        private val window = Timeline.Window()

        private var mode: LivePlaybackMode
            get() = _state.value.mode
            set(value) = _state.update { it.copy(mode = value) }

        fun start() {
            player.addListener(this)
            lookup = scope.launch {
                while (true) {
                    val available = repository.fetchLiveArchive(streamerName, stream.streamId)
                    if (available != null) {
                        archive = available
                        _state.update { it.copy(archiveStartTimeMs = available.startTimeMs) }
                        return@launch
                    }
                    delay(30_000)
                }
            }
        }

        fun close() {
            player.removeListener(this)
            lookup?.cancel()
            continuation?.cancel()
        }

        fun seekTo(positionMs: Long) {
            val currentArchive = archive
            val duration = currentArchive?.elapsedMs() ?: player.duration
            if (duration <= 0) return

            val position = positionMs.coerceIn(0, duration)
            if (duration - position <= LIVE_EDGE_TOLERANCE_MS) {
                goLive()
                return
            }

            if (currentArchive == null) {
                if (!player.isCurrentMediaItemSeekable) return
                mode = LivePlaybackMode.LIVE_WINDOW
                player.seekTo(position)
                return
            }

            if (mode != LivePlaybackMode.ARCHIVE && seekInLiveWindow(currentArchive, position)) return

            continuation?.cancel()
            lastArchiveEndMs = null
            if (mode == LivePlaybackMode.ARCHIVE && player.duration > position) {
                player.seekTo(position)
            } else {
                openArchive(currentArchive, position)
            }
        }

        fun goLive() {
            switchToLive(resume = true)
        }

        private fun switchToLive(resume: Boolean = false) {
            continuation?.cancel()
            openingArchiveAtMs = null
            if (mode == LivePlaybackMode.ARCHIVE) {
                continuation = scope.launch {
                    liveUrl = repository.refreshLivePlaybackUrl(streamerName, stream.streamId) ?: liveUrl
                    mode = LivePlaybackMode.LIVE
                    manager.replaceLivePlaybackSource(liveUrl)
                    if (resume) manager.resume()
                }
            } else {
                mode = LivePlaybackMode.LIVE
                manager.seekToLiveEdge()
                if (resume) manager.resume()
            }
        }

        private fun seekInLiveWindow(archive: LiveStreamArchive, positionMs: Long): Boolean {
            val timeline = player.currentTimeline
            val windowIndex = player.currentMediaItemIndex
            if (!player.isCurrentMediaItemSeekable || windowIndex !in 0 until timeline.windowCount) {
                return false
            }
            timeline.getWindow(windowIndex, window)
            if (window.windowStartTimeMs == C.TIME_UNSET || window.durationMs <= 0) return false

            val positionInWindow = archive.startTimeMs + positionMs - window.windowStartTimeMs
            if (positionInWindow !in 0 until window.durationMs) return false
            mode = LivePlaybackMode.LIVE_WINDOW
            player.seekTo(positionInWindow)
            return true
        }

        private fun openArchive(archive: LiveStreamArchive, positionMs: Long) {
            mode = LivePlaybackMode.ARCHIVE
            openingArchiveAtMs = positionMs
            manager.replaceLivePlaybackSource(archive.url, positionMs)
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            val requestedPosition = openingArchiveAtMs ?: return
            val windowIndex = player.currentMediaItemIndex
            if (windowIndex !in 0 until timeline.windowCount) return
            val archiveWindow = timeline.getWindow(windowIndex, window)
            if (archiveWindow.isPlaceholder || archiveWindow.durationMs <= 0) return

            openingArchiveAtMs = null
            if (requestedPosition >= archiveWindow.durationMs) {
                player.seekTo((archiveWindow.durationMs - 2_000L).coerceAtLeast(0))
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (mode == LivePlaybackMode.ARCHIVE) {
                switchToLive()
            } else if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                mode = LivePlaybackMode.LIVE
                manager.seekToLiveEdge()
                player.prepare()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && mode == LivePlaybackMode.ARCHIVE) {
                continueArchive()
            }
        }

        private fun continueArchive() {
            val endPosition = player.currentPosition
            continuation?.cancel()
            continuation = scope.launch {
                val latest = repository.fetchLiveArchive(streamerName, stream.streamId)
                    ?: return@launch
                archive = latest
                val didNotAdvance = lastArchiveEndMs?.let { endPosition - it <= 1_000L } == true
                if (latest.elapsedMs() - endPosition <= ARCHIVE_TAIL_TOLERANCE_MS ||
                    didNotAdvance
                ) {
                    switchToLive()
                } else {
                    lastArchiveEndMs = endPosition
                    openArchive(latest, endPosition)
                }
            }
        }
    }
}

private fun LiveStreamArchive.elapsedMs() = System.currentTimeMillis() - startTimeMs

private const val LIVE_EDGE_TOLERANCE_MS = 2_000L
private const val ARCHIVE_TAIL_TOLERANCE_MS = 20_000L
