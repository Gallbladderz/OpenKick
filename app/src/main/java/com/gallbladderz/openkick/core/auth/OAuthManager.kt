/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.gallbladderz.openkick.BuildConfig
import com.gallbladderz.openkick.core.network.api.OAuthApi
import com.gallbladderz.openkick.core.network.dto.auth.OAuthTokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
