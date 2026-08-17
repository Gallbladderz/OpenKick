/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class PusherEventDto(
    val event: String? = null,
    val data: String? = null
)

@Serializable
data class ChatMessageDataDto(
    val id: String? = null,
    val sender: ChatSenderDto? = null,
    val content: String? = null
)

@Serializable
data class ChatSenderDto(
    val username: String? = null,
    val identity: ChatIdentityDto? = null
)

@Serializable
data class ChatIdentityDto(
    val color: String? = null
)
