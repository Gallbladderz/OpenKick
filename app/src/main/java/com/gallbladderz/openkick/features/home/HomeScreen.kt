package com.gallbladderz.openkick.features.home

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel

class KickJsBridge(private val onJsonReceived: (String) -> Unit) {
    @android.webkit.JavascriptInterface
    fun sendDataToAndroid(json: String) {
        onJsonReceived(json)
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onStreamClick: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val uiState = state) {
                is HomeUiState.NeedsCloudflareBypass, is HomeUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Подключение к Kick...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (uiState is HomeUiState.NeedsCloudflareBypass) {
                        CloudflareBypassWebView(
                            onBypassSuccess = { jsonString ->
                                viewModel.processJson(jsonString)
                            }
                        )
                    }
                }

                is HomeUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            uiState.streams,
                            key = { it.id }
                        ) { stream ->
                            StreamCard(
                                stream = stream,
                                onClick = {
                                    onStreamClick(stream.streamerName)
                                }
                            )
                        }
                    }
                }

                is HomeUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Ошибка: ${uiState.message}",
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.triggerBypassAgain() }
                        ) {
                            Text("Пройти Cloudflare заново")
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CloudflareBypassWebView(onBypassSuccess: (json: String) -> Unit) {
    val context = LocalContext.current
    val currentOnBypassSuccess by rememberUpdatedState(onBypassSuccess)

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true
            val cleanUserAgent = settings.userAgentString.replace("; wv", "")
            settings.userAgentString = cleanUserAgent

            addJavascriptInterface(KickJsBridge { json ->
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
                            if (window.kickBypassInjected) return;
                            window.kickBypassInjected = true;

                            function tryFetch() {
                                if (document.title.includes("Just a moment") || document.title.includes("Cloudflare")) {
                                    setTimeout(tryFetch, 2000);
                                    return;
                                }

                                fetch('https://web.kick.com/api/v1/livestreams/featured?language=ru', {
                                    headers: {
                                        'Accept': 'application/json',
                                        'X-Requested-With': 'XMLHttpRequest'
                                    }
                                })
                                .then(async response => {
                                    const text = await response.text();
                                    const trimmed = text.trim();
                                    
                                    if (response.ok && (trimmed.startsWith("[") || trimmed.startsWith("{"))) {
                                        window.AndroidBridge.sendDataToAndroid(trimmed);
                                    } 
                                    else if (response.status === 403 || trimmed.startsWith("<") || trimmed === "") {
                                        setTimeout(tryFetch, 2000);
                                    } 
                                    else {
                                        window.AndroidBridge.sendDataToAndroid("JS_ERROR: " + response.status + " " + trimmed.substring(0, 50));
                                    }
                                })
                                .catch(err => {
                                    setTimeout(tryFetch, 2000); 
                                });
                            }
                            setTimeout(tryFetch, 1000);
                        })();
                    """.trimIndent()

                    view?.evaluateJavascript(jsScript, null)
                }
            }
            loadUrl("https://kick.com")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = Modifier.size(1.dp)
    )
}

@Composable
fun StreamCard(
    stream: StreamUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            AsyncImage(
                model = stream.thumbnailUrl,
                contentDescription = "Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stream.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stream.streamerName,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "👁 ${stream.viewers}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stream.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}