/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.domain.chat


data class ChatEmote(
    val id: String,
    val name: String,
    val imageUrl: String = ""
)

data class ChatEmoteSet(
    val name: String,
    val emotes: List<ChatEmote>
)

fun ChatEmote.imageUrl(animated: Boolean): String {
    val base = "https://files.kick.com/emotes/$id/fullsize"
    return if (animated) base else "$base?format=png&width=96"
}

fun kickEmoteImageUrl(id: String, animated: Boolean): String =
    ChatEmote(id = id, name = "").imageUrl(animated)
