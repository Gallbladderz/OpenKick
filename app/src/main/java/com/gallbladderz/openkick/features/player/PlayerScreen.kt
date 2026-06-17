package com.gallbladderz.openkick.features.player

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.webview.ChannelBypassWebView
import com.gallbladderz.openkick.features.home.KickStreamPlayer
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.koin.androidx.compose.koinViewModel


sealed interface PlayerUiState {
    data object Bypassing : PlayerUiState
    data class Playing(val url: String) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

data class ChatMessage(val sender: String, val content: String)

class PlayerViewModel(private val client: HttpClient) : ViewModel() {
    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Bypassing)
    val uiState = _uiState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    private var webSocket: WebSocket? = null
    private val okHttpClient = OkHttpClient()

    fun processJson(jsonString: String) {
        try {
            if (jsonString.startsWith("JS_ERROR")) {
                _uiState.value = PlayerUiState.Error("Ошибка API: $jsonString")
                return
            }

            val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString)
            if (jsonElement is JsonObject) {
                val url = jsonElement["playback_url"]?.jsonPrimitive?.content
                    ?: jsonElement["livestream"]?.jsonObject?.get("playback_url")?.jsonPrimitive?.content

                val chatroomId = jsonElement["chatroom"]?.jsonObject?.get("id")?.jsonPrimitive?.content

                if (!url.isNullOrEmpty()) {
                    _uiState.value = PlayerUiState.Playing(url)
                    if (chatroomId != null) {
                        Log.d("OpenKick_Chat", "Нашли ID комнаты: $chatroomId. Запускаем коннект")
                        connectToChat(chatroomId)
                    } else {
                        Log.e("OpenKick_Chat", "ID комнаты не найден в JSON. Ключи: ${jsonElement.keys}")
                    }
                } else {
                    _uiState.value = PlayerUiState.Error("Стрим оффлайн или ссылка не найдена")
                }
            } else {
                _uiState.value = PlayerUiState.Error("Пришел не JSON")
            }
        } catch (e: Exception) {
            _uiState.value = PlayerUiState.Error("Краш парсинга: ${e.message}")
        }
    }

    private fun connectToChat(chatroomId: String) {
        val request = Request.Builder()
            .url("wss://ws-us2.pusher.com/app/32cbd69e4b950bf97679?protocol=7&client=js&version=7.6.0&flash=false")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("OpenKick_Chat", "Успешно подключились к Pusher. Шлем запрос на подписку")
                val subscribeMsg = """{"event":"pusher:subscribe","data":{"auth":"","channel":"chatrooms.$chatroomId.v2"}}"""
                webSocket.send(subscribeMsg)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("OpenKick_Chat", "RAW MESSAGE: $text")

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
                    Log.e("OpenKick_Chat", "JSON: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("OpenKick_Chat", "Pusher отвалился: ${t.message}")
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        webSocket?.cancel()
    }
}

@Composable
fun PlayerScreen(
    streamerName: String,
    viewModel: PlayerViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White
                )
            }
            Text(
                text = streamerName,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            when (val currentState = state) {
                is PlayerUiState.Playing -> {
                    KickStreamPlayer(videoUrl = currentState.url, modifier = Modifier.fillMaxSize())
                }
                is PlayerUiState.Bypassing -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    ChannelBypassWebView(streamerName = streamerName) { json ->
                        viewModel.processJson(json)
                    }
                }
                is PlayerUiState.Error -> {
                    Text(
                        text = "Ошибка: ${currentState.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.stream_chat),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(chatMessages) { message ->
                    Text(
                        text = "",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(text = "${message.sender}: ${message.content}", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

