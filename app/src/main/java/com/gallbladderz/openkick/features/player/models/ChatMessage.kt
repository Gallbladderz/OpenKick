package com.gallbladderz.openkick.features.player.models

data class ChatMessage(
    val id: String,
    val sender: String,
    val senderColor: String,
    val content: String
)