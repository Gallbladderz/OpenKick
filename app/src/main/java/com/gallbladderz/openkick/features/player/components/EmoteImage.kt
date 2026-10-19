/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import android.content.Context
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.gallbladderz.openkick.core.domain.chat.kickEmoteImageUrl


internal fun kickEmoteImageRequest(
    context: Context,
    emoteId: String,
    animated: Boolean
): ImageRequest = ImageRequest.Builder(context)
    .data(kickEmoteImageUrl(emoteId, animated))
    .memoryCacheKey("kick-emote:$emoteId:${if (animated) "animated" else "static"}")
    .apply {
        if (!animated) {
            decoderFactory(BitmapFactoryDecoder.Factory())
            allowHardware(false)
        }
    }
    .build()
