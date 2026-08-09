/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.models

fun ChannelLinkDto.toDomain(): ChannelLink {
    return ChannelLink(
        id = this.id,
        description = this.description,
        link = this.link,
        title = this.title,
        imageUrl = this.image?.url ?: ""
    )
}
