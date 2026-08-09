/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

val Context.dataStore by preferencesDataStore(name = "kick_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val HIDE_SLOTS_KEY = booleanPreferencesKey("hide_slots")
        private val HIDE_POOLS_KEY = booleanPreferencesKey("hide_pools")
        private val HIDE_CRYPTO_KEY = booleanPreferencesKey("hide_crypto")
        private val FOLLOWED_CHANNELS = stringSetPreferencesKey("followed_channels")
        private val SELECTED_LANGUAGES = stringSetPreferencesKey("selected_languages")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val APP_THEME = stringPreferencesKey("app_theme")
        private val APP_ACCENT = stringPreferencesKey("app_accent")
        private val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
        private val HOME_GRID_MODE = booleanPreferencesKey("home_grid_mode")
        private val HOME_STREAM_SORT = stringPreferencesKey("home_stream_sort")
    }

    val homeGridModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HOME_GRID_MODE] ?: false
    }

    suspend fun setHomeGridMode(isGridMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HOME_GRID_MODE] = isGridMode
        }
    }

    val homeStreamSortFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[HOME_STREAM_SORT] ?: "viewer_count_desc"
    }

    suspend fun setHomeStreamSort(sort: String) {
        context.dataStore.edit { preferences ->
            preferences[HOME_STREAM_SORT] = sort
        }
    }

    val appThemeFlow: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        AppTheme.valueOf(preferences[APP_THEME] ?: AppTheme.DARK.name)
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[APP_THEME] = theme.name
        }
    }

    val appAccentFlow: Flow<AppAccent> = context.dataStore.data.map { preferences ->
        val accentName = preferences[APP_ACCENT] ?: AppAccent.MAUVE.name
        try {
            AppAccent.valueOf(accentName)
        } catch (e: IllegalArgumentException) {
            AppAccent.MAUVE
        }
    }

    suspend fun setAppAccent(accent: AppAccent) {
        context.dataStore.edit { preferences ->
            preferences[APP_ACCENT] = accent.name
        }
    }

    val useDynamicColorsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_DYNAMIC_COLORS] ?: false
    }

    suspend fun setUseDynamicColors(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_DYNAMIC_COLORS] = use
        }
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED] ?: true
    }


    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    val hideSlotsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HIDE_SLOTS_KEY] ?: false
    }

    suspend fun setHideSlots(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIDE_SLOTS_KEY] = hide
        }
    }

    val hidePoolsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HIDE_POOLS_KEY] ?: false
    }

    suspend fun setHidePools(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIDE_POOLS_KEY] = hide
        }
    }

    val hideCryptoFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HIDE_CRYPTO_KEY] ?: false
    }

    suspend fun setHideCrypto(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIDE_CRYPTO_KEY] = hide
        }
    }

    val followedChannelsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[FOLLOWED_CHANNELS] ?: emptySet()
    }

    suspend fun toggleFollow(slug: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[FOLLOWED_CHANNELS] ?: emptySet()

            if (current.contains(slug)) {
                preferences[FOLLOWED_CHANNELS] = current - slug
            } else {
                preferences[FOLLOWED_CHANNELS] = current + slug
            }
        }
    }

    val selectedLanguagesFlow: Flow<Set<String>> =
        context.dataStore.data.map { preferences ->
            preferences[SELECTED_LANGUAGES]
                ?: setOf(Locale.getDefault().language)
        }

    suspend fun updateSelectedLanguages(languages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_LANGUAGES] = languages
        }
    }
}