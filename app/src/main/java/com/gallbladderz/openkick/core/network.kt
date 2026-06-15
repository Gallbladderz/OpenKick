package com.gallbladderz.openkick.core.network

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class CloudflareSession(
    val userAgent: String,
    val cfClearance: String
)

class CloudflareBypasser(private val context: Context) {

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun getSession(url: String = "https://kick.com"): CloudflareSession? = withContext(Dispatchers.Main) {
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        val defaultUserAgent = webView.settings.userAgentString
        val cleanUserAgent = defaultUserAgent.replace("; wv", "")
        webView.settings.userAgentString = cleanUserAgent

        webView.webViewClient = WebViewClient()
        webView.loadUrl(url)

        var session: CloudflareSession? = null

        for (i in 1..20) {
            delay(500)
            val cookies = CookieManager.getInstance().getCookie(url)
            val cfClearance = extractCfClearance(cookies)

            if (cfClearance != null) {
                session = CloudflareSession(cleanUserAgent, cfClearance)
                break
            }
        }

        webView.destroy()

        return@withContext session
    }

    private fun extractCfClearance(cookieString: String?): String? {
        if (cookieString == null) return null
        val cookies = cookieString.split(";")
        for (cookie in cookies) {
            val parts = cookie.trim().split("=")
            if (parts.size >= 2 && parts[0] == "cf_clearance") {
                return parts[1]
            }
        }
        return null
    }
}