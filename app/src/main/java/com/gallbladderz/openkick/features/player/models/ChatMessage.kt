package com.gallbladderz.openkick.features.player.models

sealed interface ChatToken {
    data class Text(val text: String) : ChatToken
    data class Emote(val emoteId: String, val emoteName: String) : ChatToken
}

data class ChatMessage(
    val id: String,
    val sender: String,
    val senderColor: String,
    val content: String,
    val tokens: List<ChatToken>
)
