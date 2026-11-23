/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.core.domain.model

import com.gallbladderz.openkick.core.domain.chat.ChatReplaySource

data class ClipUiModel(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val views: Int,
    val durationFormatted: String,
    val streamerName: String,
    val streamerAvatarUrl: String,
    val streamerDisplayName: String = streamerName,
    val creatorName: String? = null,
    val categoryName: String? = null,
    val createdAt: String? = null,
    val durationSec: Long? = null,
    val chatReplay: ChatReplaySource? = null
)
