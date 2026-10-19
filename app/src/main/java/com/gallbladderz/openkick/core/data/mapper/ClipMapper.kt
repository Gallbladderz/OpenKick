/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.data.mapper

import com.gallbladderz.openkick.core.domain.model.ClipUiModel
import com.gallbladderz.openkick.core.network.dto.home.ClipDto

fun ClipDto.toUiModel(): ClipUiModel {
    val minutes = duration / 60
    val seconds = duration % 60
    val durationStr = String.format("%02d:%02d", minutes, seconds)

    return ClipUiModel(
        id = id,
        title = title,
        thumbnailUrl = thumbnailUrl,
        videoUrl = clipUrl,
        views = views,
        durationFormatted = durationStr,
        streamerName = channel?.displayName
            ?.ifBlank { creator?.displayName ?: "" }
            ?.ifBlank { user?.displayName ?: "" }
            ?.ifBlank { "Anonymous" } ?: "Anonymous",
        streamerAvatarUrl = channel?.avatar
            ?.ifBlank { creator?.avatar ?: "" }
            ?.ifBlank { user?.avatar ?: "" }
            ?.replace("\\/", "/")
            ?: ""
    )
}
