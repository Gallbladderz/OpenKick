package com.gallbladderz.openkick.features.player.models

data class StreamInfo(
    val playbackUrl: String,
    val avatarUrl: String,
    val viewers: Int,
    val title: String,
    val chatroomId: String?
)
