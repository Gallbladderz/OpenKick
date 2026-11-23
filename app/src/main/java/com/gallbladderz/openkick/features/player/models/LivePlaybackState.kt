/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.models

internal data class LiveStreamArchive(val url: String, val startTimeMs: Long)

enum class LivePlaybackMode {
    LIVE,
    LIVE_WINDOW,
    ARCHIVE
}

data class LivePlaybackState(
    val mode: LivePlaybackMode = LivePlaybackMode.LIVE,
    val archiveStartTimeMs: Long? = null,
    val startedAtMs: Long? = null
)
