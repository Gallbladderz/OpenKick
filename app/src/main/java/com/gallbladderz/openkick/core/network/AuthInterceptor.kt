/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.network

import com.gallbladderz.openkick.core.auth.OAuthManager
import com.gallbladderz.openkick.core.auth.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionManager: SessionManager,
    private val oauth: OAuthManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val current = sessionManager.tokens()
        if (current != null && current.expiresAt <= System.currentTimeMillis()) {
            synchronized(this) {
                if ((sessionManager.tokens()?.expiresAt ?: 0) <= System.currentTimeMillis()) {
                    kotlinx.coroutines.runBlocking { oauth.refresh() }
                }
            }
        }

        val builder = request.newBuilder()
            .header("User-Agent", sessionManager.userAgent())
            .header("Accept", "application/json")
            .header("Origin", "https://kick.com")
            .header("Referer", "https://kick.com/")

        sessionManager.tokens()?.accessToken?.takeIf { it.isNotBlank() }?.let {
            builder.header("Authorization", "Bearer $it")
        }

        sessionManager.xsrfToken()?.takeIf { it.isNotBlank() }?.let {
            builder.header("X-XSRF-TOKEN", it)
        }
        sessionManager.cookies()?.takeIf { it.isNotBlank() }?.let {
            builder.header("Cookie", it)
        }

        return chain.proceed(builder.build())
    }
}
