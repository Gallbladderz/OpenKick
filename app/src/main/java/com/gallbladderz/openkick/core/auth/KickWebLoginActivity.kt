/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.auth

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.gallbladderz.openkick.R
import java.net.URLDecoder

class KickWebLoginActivity : ComponentActivity() {
    private lateinit var webView: WebView

    companion object {
        @Volatile
        var activeSessionManager: SessionManager? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = activeSessionManager ?: SessionManager(applicationContext)
        val toolbarHeight = (48 * resources.displayMetrics.density).toInt()
        val toolbar = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(22, 22, 26))
        }
        val close = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            background = null
            contentDescription = getString(R.string.close_authorization)
            setOnClickListener { finish() }
        }
        toolbar.addView(close, FrameLayout.LayoutParams(toolbarHeight, toolbarHeight, Gravity.END))

        webView = createLoginWebView(manager)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(webView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            root.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(root)
        ViewCompat.requestApplyInsets(root)
        webView.loadUrl("https://kick.com/login")
    }

    override fun onDestroy() {
        (webView.parent as? LinearLayout)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createLoginWebView(manager: SessionManager): WebView = WebView(this).apply {
        val userAgent = SessionManager.DEFAULT_USER_AGENT
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString = userAgent

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (isFinishing) return
                val cookies = cookieManager.getCookie("https://kick.com") ?: return
                val cookieMap = cookies.split(";")
                    .map { it.trim().split("=", limit = 2) }
                    .filter { it.size == 2 }
                    .associate { it[0] to it[1] }

                val rawToken = cookieMap["session_token"]?.takeIf { it.isNotBlank() } ?: return
                manager.saveWebSession(
                    sessionToken = URLDecoder.decode(rawToken, "UTF-8"),
                    xsrfToken = cookieMap["XSRF-TOKEN"]?.let { URLDecoder.decode(it, "UTF-8") },
                    rawCookies = cookies,
                    userAgent = userAgent
                )
                finish()
            }
        }
    }
}
