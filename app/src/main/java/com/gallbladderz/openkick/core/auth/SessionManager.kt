/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AppMode { UNDECIDED, AUTONOMOUS, AUTHENTICATED }

data class OAuthTokens(val accessToken: String, val refreshToken: String, val expiresAt: Long)

data class AccountIdentity(val username: String, val avatarUrl: String, val profileSlug: String)

class SessionManager(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "openkick_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _mode = MutableStateFlow(readMode())
    val mode: StateFlow<AppMode> = _mode
    private val _sessionGeneration = MutableStateFlow(0L)
    val sessionGeneration: StateFlow<Long> = _sessionGeneration

    @Synchronized
    fun selectAutonomous() {
        require(_mode.value != AppMode.AUTHENTICATED) { "Sign out before switching modes" }
        check(preferences.edit().putString(MODE, AppMode.AUTONOMOUS.name).commit()) {
            "Could not persist app mode"
        }
        _mode.value = AppMode.AUTONOMOUS
    }

    @Synchronized
    fun saveTokens(tokens: OAuthTokens, newSession: Boolean = false) {
        require(tokens.accessToken.isNotBlank() && tokens.refreshToken.isNotBlank() && tokens.expiresAt > 0) {
            "Invalid OAuth tokens"
        }
        val accountChanged = newSession || _mode.value != AppMode.AUTHENTICATED
        val editor = preferences.edit()
            .putString(ACCESS, tokens.accessToken)
            .putString(REFRESH, tokens.refreshToken)
            .putLong(EXPIRES, tokens.expiresAt)
            .putString(MODE, AppMode.AUTHENTICATED.name)
        if (accountChanged) {
            editor.remove(ACCOUNT_USERNAME).remove(ACCOUNT_AVATAR).remove(ACCOUNT_SLUG)
        }
        check(editor.commit()) { "Could not persist OAuth session" }
        if (accountChanged) _sessionGeneration.value++
        _mode.value = AppMode.AUTHENTICATED
    }

    @Synchronized
    fun cachedAccount(): AccountIdentity? {
        if (_mode.value != AppMode.AUTHENTICATED) return null
        val username = preferences.getString(ACCOUNT_USERNAME, null)?.takeIf { it.isNotBlank() }
            ?: return null
        val slug = preferences.getString(ACCOUNT_SLUG, null)?.takeIf { it.isNotBlank() }
            ?: return null
        return AccountIdentity(
            username = username,
            avatarUrl = preferences.getString(ACCOUNT_AVATAR, null).orEmpty(),
            profileSlug = slug
        )
    }

    @Synchronized
    fun cacheAccount(account: AccountIdentity, expectedGeneration: Long): Boolean {
        if (_mode.value != AppMode.AUTHENTICATED || _sessionGeneration.value != expectedGeneration) return false
        if (account.username.isBlank() || account.profileSlug.isBlank()) return false
        return preferences.edit()
            .putString(ACCOUNT_USERNAME, account.username)
            .putString(ACCOUNT_AVATAR, account.avatarUrl)
            .putString(ACCOUNT_SLUG, account.profileSlug)
            .commit()
    }

    @Synchronized
    fun saveWebSession(
        sessionToken: String,
        xsrfToken: String?,
        rawCookies: String,
        userAgent: String
    ) {
        preferences.edit()
            .putString(COOKIES, rawCookies)
            .putString(XSRF, xsrfToken)
            .putString(USER_AGENT, userAgent)
            .commit()

        saveTokens(
            tokens = OAuthTokens(
                accessToken = sessionToken,
                refreshToken = "web_session",
                expiresAt = System.currentTimeMillis() + 31536000_000L
            ),
            newSession = true
        )
    }

    @Synchronized
    fun tokens(): OAuthTokens? {
        val access = preferences.getString(ACCESS, null) ?: return null
        val refresh = preferences.getString(REFRESH, null) ?: return null
        val expires = preferences.getLong(EXPIRES, 0)
        return if (access.isNotBlank() && refresh.isNotBlank() && expires > 0) {
            OAuthTokens(access, refresh, expires)
        } else null
    }

    @Synchronized
    fun cookies(): String? = preferences.getString(COOKIES, null)

    @Synchronized
    fun xsrfToken(): String? = preferences.getString(XSRF, null)

    @Synchronized
    fun userAgent(): String =
        preferences.getString(USER_AGENT, DEFAULT_USER_AGENT) ?: DEFAULT_USER_AGENT

    @Synchronized
    fun signOut() {
        check(
            preferences.edit()
                .remove(ACCESS)
                .remove(REFRESH)
                .remove(EXPIRES)
                .remove(COOKIES)
                .remove(XSRF)
                .remove(ACCOUNT_USERNAME)
                .remove(ACCOUNT_AVATAR)
                .remove(ACCOUNT_SLUG)
                .putString(MODE, AppMode.AUTONOMOUS.name)
                .commit()
        ) { "Could not clear OAuth session" }
        _sessionGeneration.value++
        _mode.value = AppMode.AUTONOMOUS
    }

    @Synchronized
    fun savePendingPkce(request: PkceRequest) {
        check(
            preferences.edit()
                .putString(PENDING_VERIFIER, request.verifier)
                .putString(PENDING_STATE, request.state)
                .commit()
        ) { "Could not persist OAuth request" }
    }

    @Synchronized
    fun consumePendingPkce(state: String): String? {
        val storedState = preferences.getString(PENDING_STATE, null)
        val verifier = preferences.getString(PENDING_VERIFIER, null)
        if (storedState.isNullOrBlank() || verifier.isNullOrBlank() || storedState != state) return null
        preferences.edit().remove(PENDING_STATE).remove(PENDING_VERIFIER).apply()
        return verifier
    }

    private fun readMode(): AppMode {
        val stored = preferences.getString(MODE, null)
        val parsed = AppMode.entries.firstOrNull { it.name == stored } ?: AppMode.UNDECIDED
        return if (parsed == AppMode.AUTHENTICATED && tokens() == null) AppMode.AUTONOMOUS else parsed
    }

    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        private const val MODE = "mode"
        private const val ACCESS = "access_token"
        private const val REFRESH = "refresh_token"
        private const val EXPIRES = "expires_at"
        private const val COOKIES = "raw_cookies"
        private const val XSRF = "xsrf_token"
        private const val USER_AGENT = "user_agent"
        private const val ACCOUNT_USERNAME = "account_username"
        private const val ACCOUNT_AVATAR = "account_avatar"
        private const val ACCOUNT_SLUG = "account_slug"
        private const val PENDING_VERIFIER = "pending_pkce_verifier"
        private const val PENDING_STATE = "pending_oauth_state"
    }
}
