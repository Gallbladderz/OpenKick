/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.network.dto.profile

import com.gallbladderz.openkick.core.auth.AccountIdentity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val username: String = "",
    val profilepic: String? = null,
    @SerialName("profile_pic") val profilePic: String? = null,
    @SerialName("profile_picture") val profilePicture: String? = null,
    @SerialName("profile_thumb") val profileThumb: String? = null,
    @SerialName("streamer_channel") val streamerChannel: AccountChannelDto? = null
) {
    fun toIdentity(): AccountIdentity? {
        val name = username.takeIf { it.isNotBlank() } ?: return null
        return AccountIdentity(
            username = name,
            avatarUrl = profilepic?.takeIf { it.isNotBlank() }
                ?: profilePic?.takeIf { it.isNotBlank() }
                ?: profilePicture?.takeIf { it.isNotBlank() }
                ?: profileThumb?.takeIf { it.isNotBlank() }.orEmpty(),
            profileSlug = streamerChannel?.slug?.takeIf { it.isNotBlank() } ?: name
        )
    }
}

@Serializable
data class AccountChannelDto(val slug: String? = null)

