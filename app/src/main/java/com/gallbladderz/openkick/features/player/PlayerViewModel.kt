package com.gallbladderz.openkick.features.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.core.datastore.SettingsRepository
import com.gallbladderz.openkick.core.network.KickApiConstants
import com.gallbladderz.openkick.features.player.models.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class PlayerViewModel(
    private val repository: PlayerRepository,
    private val okHttpClient: OkHttpClient,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    private val _isFollowed = MutableStateFlow(false)
    val isFollowed = _isFollowed.asStateFlow()

    private var webSocket: WebSocket? = null

    fun loadStreamInfo(streamerName: String) {
        viewModelScope.launch {
            settingsRepository.followedChannelsFlow.collect { follows ->
                _isFollowed.value = follows.contains(streamerName)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.fetchStreamInfo(streamerName).collect { result ->
                result.onSuccess { responseBody ->
                    parseJson(responseBody)
                }.onFailure { exception ->
                    _uiState.value = PlayerUiState.Error("Ошибка сети: ${exception.message}")
                }
            }
        }
    }

    fun toggleFollow(streamerName: String) {
        viewModelScope.launch {
            settingsRepository.toggleFollow(streamerName)
        }
    }

    private fun parseJson(jsonString: String) {
        try {
            val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString)

            if (jsonElement is JsonObject) {
                val livestreamObj = jsonElement["livestream"]?.jsonObject
                val userObj = jsonElement["user"]?.jsonObject

                val url = jsonElement["playback_url"]?.jsonPrimitive?.content
                    ?: livestreamObj?.get("playback_url")?.jsonPrimitive?.content

                val chatroomId = jsonElement["chatroom"]?.jsonObject?.get("id")?.jsonPrimitive?.content
                    ?: jsonElement["chatroom_id"]?.jsonPrimitive?.content

                var avatar = userObj?.get("profile_pic")?.jsonPrimitive?.content ?: ""
                avatar = avatar.replace("\\/", "/")

                val viewers = livestreamObj?.get("viewer_count")?.jsonPrimitive?.intOrNull ?: 0
                val title = livestreamObj?.get("session_title")?.jsonPrimitive?.content ?: "Трансляция"

                if (!url.isNullOrEmpty()) {
                    _uiState.value = PlayerUiState.Playing(url, avatar, viewers, title)

                    if (chatroomId != null) {
                        connectToChat(chatroomId)
                    }
                } else {
                    _uiState.value = PlayerUiState.Error("Стример сейчас оффлайн")
                }
            } else {
                _uiState.value = PlayerUiState.Error("Пришел не JSON объект")
            }
        } catch (e: Exception) {
            _uiState.value = PlayerUiState.Error("Ошибка обработки ответа API")
        }
    }

    private fun connectToChat(chatroomId: String) {
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

                        val sender = dataJson["sender"]?.jsonObject?.get("username")?.jsonPrimitive?.content ?: "Аноним"
                        val content = dataJson["content"]?.jsonPrimitive?.content ?: ""

                        val newMessage = ChatMessage(sender, content)
                        _chatMessages.value = (listOf(newMessage) + _chatMessages.value).take(100)
                    }
                } catch (e: Exception) {
                }
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        webSocket?.cancel()
    }
}