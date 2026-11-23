/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.data.mapper

import com.gallbladderz.openkick.core.domain.model.ClipUiModel
import com.gallbladderz.openkick.core.domain.chat.ChatReplaySource
import com.gallbladderz.openkick.core.network.dto.home.ClipDto

fun ClipDto.toUiModel(): ClipUiModel {
    val minutes = duration / 60
    val seconds = duration % 60
    val durationStr = String.format("%02d:%02d", minutes, seconds)
    val streamer = channel?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.displayName?.takeIf { it.isNotBlank() }

    return ClipUiModel(
        id = id,
        title = title,
        thumbnailUrl = thumbnailUrl,
        videoUrl = clipUrl,
        views = views,
        durationFormatted = durationStr,
        streamerName = streamer ?: "Anonymous",
        streamerAvatarUrl = channel?.avatar.orEmpty()
            .ifBlank { user?.avatar.orEmpty() }.replace("\\/", "/"),
        streamerDisplayName = channel?.username?.takeIf { it.isNotBlank() }
            ?: streamer.orEmpty(),
        creatorName = creator?.username?.takeIf { it.isNotBlank() } ?: creator?.displayName,
        categoryName = category?.name,
        createdAt = createdAt,
        durationSec = duration.toLong().takeIf { it > 0 },
        chatReplay = ChatReplaySource.from(channelId ?: channel?.id, startedAt)
    )
}
