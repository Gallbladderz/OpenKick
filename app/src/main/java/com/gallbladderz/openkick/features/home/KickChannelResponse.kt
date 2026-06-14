package com.gallbladderz.openkick.features.home

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.InternalSerializationApi

@OptIn(InternalSerializationApi::class)
@Serializable
data class KickChannelResponse(
    @SerialName("playback_url")
    val playbackUrl: String? = null,
    @SerialName("is_banned")
    val isBanned: Boolean = false,
    val user: KickUser? = null
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class KickUser(
    val username: String
)