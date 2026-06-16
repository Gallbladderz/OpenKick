package com.gallbladderz.openkick.features.categories

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import coil.request.ImageRequest
import org.koin.androidx.compose.koinViewModel

class CategoriesJsBridge(private val onJsonReceived: (String) -> Unit) {
    @android.webkit.JavascriptInterface
    fun sendDataToAndroid(json: String) {
        onJsonReceived(json)
    }
}

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel = koinViewModel(),
    onCategoryClick: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val uiState = state) {
            is CategoriesUiState.Loading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Загрузка категорий...")
                }

                CategoriesBypassWebView { json ->
                    viewModel.processJson(json)
                }
            }

            is CategoriesUiState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.categories, key = { it.id }) { category ->
                        CategoryCard(category = category, onClick = { onCategoryClick(category.name) })
                    }
                }
            }

            is CategoriesUiState.Error -> {
                Text(
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun CategoryCard(category: CategoryUiModel, onClick: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(category.bannerUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .addHeader("Referer", "https://kick.com/")
                    .crossfade(true)
                    .build(),
                contentDescription = category.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onState = { state ->
                    // Если картинка не грузится, мы точно увидим почему в логах
                    if (state is coil.compose.AsyncImagePainter.State.Error) {
                        Log.e("OpenKick_Image", "Coil обосрался на ${category.name}. Урл: ${category.bannerUrl}", state.result.throwable)
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = "👁 ${category.viewers}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CategoriesBypassWebView(onBypassSuccess: (String) -> Unit) {
    val context = LocalContext.current
    val currentOnBypassSuccess by rememberUpdatedState(onBypassSuccess)

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = settings.userAgentString.replace("; wv", "")

            addJavascriptInterface(CategoriesJsBridge { json ->
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
                            if (window.categoriesBypassInjected) return;
                            window.categoriesBypassInjected = true;

                            function tryFetch() {
                                if (document.title.includes("Just a moment") || document.title.includes("Cloudflare")) {
                                    setTimeout(tryFetch, 1000);
                                    return;
                                }

                                fetch('https://kick.com/api/v1/subcategories?limit=100', {
                                    headers: { 'Accept': 'application/json' }
                                })
                                .then(async response => {
                                    const text = await response.text();
                                    const trimmed = text.trim();
                                    if (response.ok) {
                                        window.AndroidBridge.sendDataToAndroid(trimmed);
                                    } else {
                                        setTimeout(tryFetch, 1000); 
                                    }
                                })
                                .catch(err => {
                                    setTimeout(tryFetch, 1000); 
                                });
                            }
                            setTimeout(tryFetch, 500);
                        })();
                    """.trimIndent()

                    view?.evaluateJavascript(jsScript, null)
                }
            }
            loadDataWithBaseURL("https://kick.com", "<html><body></body></html>", "text/html", "UTF-8", null)
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