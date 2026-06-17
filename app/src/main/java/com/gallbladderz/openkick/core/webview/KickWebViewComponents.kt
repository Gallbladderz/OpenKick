package com.gallbladderz.openkick.core.webview

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gallbladderz.openkick.core.WebViewScripts

class KickJsBridge(private val onJsonReceived: (String) -> Unit) {
    @android.webkit.JavascriptInterface
    fun sendDataToAndroid(json: String) {
        onJsonReceived(json)
    }
}

class CategoriesJsBridge(private val onJsonReceived: (String) -> Unit) {
    @android.webkit.JavascriptInterface
    fun sendDataToAndroid(json: String) {
        onJsonReceived(json)
    }
}

class PlayerJsBridge(private val onJsonReceived: (String) -> Unit) {
    @android.webkit.JavascriptInterface
    fun sendDataToAndroid(json: String) {
        onJsonReceived(json)
    }
}

class SearchJsBridge(private val onJsonReceived: (String) -> Unit) {
    @android.webkit.JavascriptInterface
    fun sendDataToAndroid(json: String) {
        onJsonReceived(json)
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
                    val jsScript = WebViewScripts.CLOUDFLARE_BYPASS_SCRIPT
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
                    val jsScript = WebViewScripts.CATEGORIES_BYPASS_SCRIPT
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChannelBypassWebView(streamerName: String, onBypassSuccess: (String) -> Unit) {
    val context = LocalContext.current
    val currentOnBypassSuccess by rememberUpdatedState(onBypassSuccess)

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = settings.userAgentString.replace("; wv", "")

            addJavascriptInterface(PlayerJsBridge { json ->
                post { currentOnBypassSuccess(json) }
            }, "AndroidBridge")

            webViewClient = object : WebViewClient() {
                @SuppressLint("WebViewClientOnReceivedSslError")
                override fun onReceivedSslError(view: WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                    handler?.proceed()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val jsScript = WebViewScripts.getChannelBypassScript(streamerName)
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
                    val jsScript = WebViewScripts.SEARCH_BYPASS_SCRIPT
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
