package com.gallbladderz.openkick.features.player

import com.gallbladderz.openkick.core.network.KickApiConstants
import com.gallbladderz.openkick.features.player.models.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class ChatRepository(private val okHttpClient: OkHttpClient) {
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    private var webSocket: WebSocket? = null

    fun connectToChat(chatroomId: String) {
        webSocket?.cancel()

        val request = Request.Builder()
            .url(KickApiConstants.PUSHER_WS_URL)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val subscribeMsg = """{"event":"pusher:subscribe","data":{"auth":"","channel":"chatrooms.$chatroomId.v2"}}"""
                webSocket.send(subscribeMsg)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(text).jsonObject
                    val event = json["event"]?.jsonPrimitive?.content

                    if (event == "App\\Events\\ChatMessageEvent") {
                        val dataString = json["data"]?.jsonPrimitive?.content ?: return
                        val dataJson = Json { ignoreUnknownKeys = true }.parseToJsonElement(dataString).jsonObject

                        
                        val id = dataJson["id"]?.jsonPrimitive?.content ?: java.util.UUID.randomUUID().toString()

                        val senderObj = dataJson["sender"]?.jsonObject
                        val sender = senderObj?.get("username")?.jsonPrimitive?.content ?: "Аноним"

                        
                        val senderColor = senderObj?.get("identity")?.jsonObject?.get("color")?.jsonPrimitive?.content ?: ""

                        val content = dataJson["content"]?.jsonPrimitive?.content ?: ""

                        val newMessage = ChatMessage(id, sender, senderColor, content)
                        _chatMessages.value = (listOf(newMessage) + _chatMessages.value).take(100)
                    }
                } catch (e: Exception) {
                }
            }
        })
    }

    fun disconnect() {
        webSocket?.cancel()
        webSocket = null
        _chatMessages.value = emptyList()
    }
}
