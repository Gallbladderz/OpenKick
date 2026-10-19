/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.gallbladderz.openkick.BuildConfig
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.network.api.OAuthApi
import com.gallbladderz.openkick.core.network.dto.auth.OAuthTokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLDecoder

class OAuthManager(
    private val context: Context,
    private val api: OAuthApi,
    private val sessionManager: SessionManager
) {
    fun beginLogin() {
        KickWebLoginActivity.activeSessionManager = sessionManager
        val intent = Intent(context, KickWebLoginActivity::class.java).apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    suspend fun handleCallback(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        if (uri.toString().substringBefore('?') != BuildConfig.KICK_REDIRECT_URI) {
            return@withContext Result.failure(IllegalArgumentException("Unexpected OAuth redirect"))
        }
        val state = uri.getQueryParameter("state")
            ?: return@withContext Result.failure(IllegalArgumentException("Missing OAuth state"))
        val verifier = sessionManager.consumePendingPkce(state)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid OAuth state"))
        val error = uri.getQueryParameter("error")
        if (error != null) return@withContext Result.failure(IllegalStateException(error))
        val code = uri.getQueryParameter("code")
            ?: return@withContext Result.failure(IllegalArgumentException("Missing authorization code"))
        runCatching {
            val response = api.exchangeCode(
                code = code,
                clientId = BuildConfig.KICK_CLIENT_ID,
                redirectUri = BuildConfig.KICK_REDIRECT_URI,
                codeVerifier = verifier
            )
            sessionManager.saveTokens(response.toTokens(null), newSession = true)
        }
    }

    suspend fun refresh(): Result<Unit> = withContext(Dispatchers.IO) {
        val current = sessionManager.tokens()
            ?: return@withContext Result.failure(IllegalStateException("No OAuth session"))
        runCatching {
            val response =
                api.refreshToken("refresh_token", current.component2(), BuildConfig.KICK_CLIENT_ID)
            sessionManager.saveTokens(response.toTokens(current.component2()))
        }
    }

    private fun OAuthTokenResponse.toTokens(previous: String?): OAuthTokens {
        require(expiresIn > 0 && accessToken.isNotBlank()) { "Invalid OAuth response" }
        return OAuthTokens(
            accessToken,
            refreshToken ?: previous.orEmpty(),
            System.currentTimeMillis() + expiresIn * 1000L
        )
    }
}

class KickWebLoginActivity : ComponentActivity() {
    companion object {
        @Volatile
        var activeSessionManager: SessionManager? = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = activeSessionManager ?: SessionManager(applicationContext)
        val userAgent = SessionManager.DEFAULT_USER_AGENT

        val root = FrameLayout(this)
        val baseToolbarHeight = (48 * resources.displayMetrics.density).toInt()
        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = userAgent

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val cookies = cookieManager.getCookie("https://kick.com") ?: return
                    val cookieMap = cookies.split(";")
                        .map { it.trim().split("=", limit = 2) }
                        .filter { it.size == 2 }
                        .associate { it[0] to it[1] }

                    val rawToken = cookieMap["session_token"]
                    if (!rawToken.isNullOrBlank()) {
                        val decodedToken = URLDecoder.decode(rawToken, "UTF-8")
                        val decodedXsrf =
                            cookieMap["XSRF-TOKEN"]?.let { URLDecoder.decode(it, "UTF-8") }
                        manager.saveWebSession(
                            sessionToken = decodedToken,
                            xsrfToken = decodedXsrf,
                            rawCookies = cookies,
                            userAgent = userAgent
                        )
                        finish()
                    }
                }
            }
            loadUrl("https://kick.com/login")
        }

        val toolbar = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(22, 22, 26))
            elevation = 4 * resources.displayMetrics.density
        }

        val closeButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            background = null
            contentDescription = getString(R.string.close_authorization)
            setOnClickListener { finish() }
        }

        val webViewParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        root.addView(webView, webViewParams)
        toolbar.addView(
            closeButton,
            FrameLayout.LayoutParams(baseToolbarHeight, baseToolbarHeight).apply {
                gravity = Gravity.BOTTOM or Gravity.END
            })
        val toolbarParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            baseToolbarHeight
        ).apply { gravity = Gravity.TOP }
        root.addView(toolbar, toolbarParams)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val totalToolbarHeight = baseToolbarHeight + topInset
            toolbarParams.height = totalToolbarHeight
            toolbar.layoutParams = toolbarParams
            toolbar.setPadding(0, topInset, 0, 0)
            webViewParams.topMargin = totalToolbarHeight
            webView.layoutParams = webViewParams
            insets
        }
        setContentView(root)
        ViewCompat.requestApplyInsets(root)
    }
}
