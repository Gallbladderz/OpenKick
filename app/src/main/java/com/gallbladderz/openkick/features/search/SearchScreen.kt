package com.gallbladderz.openkick.features.search

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

class SearchJsBridge(private val onJsonReceived: (String) -> Unit) {
    @android.webkit.JavascriptInterface
    fun sendDataToAndroid(json: String) {
        onJsonReceived(json)
    }
}

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = koinViewModel(),
    onChannelClick: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            viewModel.clearResults()
            return@LaunchedEffect
        }
        viewModel.setLoading()
        delay(500)
        webViewRef?.evaluateJavascript("if(window.doSearch) window.doSearch('$query');", null)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Найти стримера...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when (val uiState = state) {
                is SearchUiState.Idle -> {
                    Text(
                        "Введите никнейм для поиска",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is SearchUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is SearchUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.channels, key = { it.username }) { channel ->
                            SearchChannelCard(channel = channel, onClick = { onChannelClick(channel.username) })
                        }
                    }
                }
                is SearchUiState.Error -> {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            SearchBypassWebView(
                onWebViewCreated = { webViewRef = it },
                onBypassSuccess = { json -> viewModel.processJson(json) }
            )
        }
    }
}

@Composable
fun SearchChannelCard(channel: SearchUiModel, onClick: () -> Unit) {
    val context = LocalContext.current

    val fallbackAvatar = "https://ui-avatars.com/api/?name=${channel.username}&background=random&color=fff&size=256"
    val finalImageUrl = if (channel.profilePic.isBlank()) fallbackAvatar else channel.profilePic

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(finalImageUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .addHeader("Referer", "https://kick.com/")
                .crossfade(true)
                .build(),
            contentDescription = "Аватар",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            onState = { state ->
                if (state is coil.compose.AsyncImagePainter.State.Error) {
                    Log.e("OpenKick_SearchImage", "Coil обосрался на аве ${channel.username}. Урл: $finalImageUrl", state.result.throwable)
                }
            }
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = channel.username,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        if (channel.isLive) {
            Box(
                modifier = Modifier
                    .background(Color.Red, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "LIVE",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SearchBypassWebView(
    onWebViewCreated: (WebView) -> Unit,
    onBypassSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val currentOnBypassSuccess by rememberUpdatedState(onBypassSuccess)

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = settings.userAgentString.replace("; wv", "")

            addJavascriptInterface(SearchJsBridge { json ->
                post { currentOnBypassSuccess(json) }
            }, "AndroidBridge")

            webViewClient = object : WebViewClient() {
                @SuppressLint("WebViewClientOnReceivedSslError")
                override fun onReceivedSslError(view: WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                    handler?.proceed()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    val jsScript = """
                        (function() {
                            if (window.searchInjected) return;
                            window.searchInjected = true;

                            window.doSearch = function(query) {
                                if (!query) {
                                    window.AndroidBridge.sendDataToAndroid('{"empty":true}');
                                    return;
                                }
                                fetch('https://kick.com/api/search?searched_word=' + encodeURIComponent(query), {
                                    headers: { 'Accept': 'application/json' }
                                })
                                .then(async response => {
                                    const text = await response.text();
                                    if (response.ok) {
                                        window.AndroidBridge.sendDataToAndroid(text);
                                    } else {
                                        window.AndroidBridge.sendDataToAndroid("JS_ERROR: " + response.status);
                                    }
                                })
                                .catch(err => {
                                    window.AndroidBridge.sendDataToAndroid("JS_ERROR: fetch failed");
                                });
                            };
                        })();
                    """.trimIndent()

                    view?.evaluateJavascript(jsScript, null)
                }
            }
            loadDataWithBaseURL("https://kick.com", "<html><body></body></html>", "text/html", "UTF-8", null)
        }
    }

    LaunchedEffect(webView) {
        onWebViewCreated(webView)
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(factory = { webView }, modifier = Modifier.size(1.dp))
}