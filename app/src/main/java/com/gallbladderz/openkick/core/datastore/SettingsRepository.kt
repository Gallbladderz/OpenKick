/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.core.datastore

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Locale

/*
 * Settings recovery adapted from QuinKick (https://github.com/kaws3664/QuinKick),
 * by kaws3664, licensed under GPL-3.0-or-later.
 */
internal fun settingsCorruptionHandler(): ReplaceFileCorruptionHandler<Preferences> =
    ReplaceFileCorruptionHandler { emptyPreferences() }

val Context.dataStore by preferencesDataStore(
    name = "kick_settings",
    corruptionHandler = settingsCorruptionHandler()
)

internal inline fun <reified T> Preferences.readSetting(key: Preferences.Key<T>): T? =
    asMap()[key] as? T

class SettingsRepository(private val context: Context) {
    private val settings = context.dataStore.data.catch { error ->
        if (error !is IOException) throw error
        emit(emptyPreferences())
    }

    companion object {
        private val HIDE_SLOTS_KEY = booleanPreferencesKey("hide_slots")
        private val HIDE_POOLS_KEY = booleanPreferencesKey("hide_pools")
        private val HIDE_CRYPTO_KEY = booleanPreferencesKey("hide_crypto")
        private val SELECTED_LANGUAGES = stringSetPreferencesKey("selected_languages")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val APP_THEME = stringPreferencesKey("app_theme")
        private val APP_ACCENT = stringPreferencesKey("app_accent")
        private val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
        private val HOME_GRID_MODE = booleanPreferencesKey("home_grid_mode")
        private val HOME_STREAM_SORT = stringPreferencesKey("home_stream_sort")
        private val CHAT_TEXT_SCALE = intPreferencesKey("chat_text_scale_percent")
        private val CHAT_GLOBAL_BADGES = booleanPreferencesKey("chat_global_badges")
        private val CHAT_CHANNEL_BADGES = booleanPreferencesKey("chat_channel_badges")
        private val CHAT_OFFICIAL_NAME_COLOR = booleanPreferencesKey("chat_official_name_color")
        private val CHAT_ANIMATED_EMOTES = booleanPreferencesKey("chat_animated_emotes")
        private val CHAT_SEVEN_TV_EMOTES = booleanPreferencesKey("chat_seven_tv_emotes")
    }

    val chatSettingsFlow: Flow<ChatSettings> = settings.map { preferences ->
        ChatSettings(
            textScale = ((preferences.readSetting(CHAT_TEXT_SCALE) ?: 100) / 100f).coerceIn(0.85f, 1.3f),
            showGlobalBadges = preferences.readSetting(CHAT_GLOBAL_BADGES) ?: true,
            showChannelBadges = preferences.readSetting(CHAT_CHANNEL_BADGES) ?: true,
            useOfficialNameColor = preferences.readSetting(CHAT_OFFICIAL_NAME_COLOR) ?: true,
            animatedEmotes = preferences.readSetting(CHAT_ANIMATED_EMOTES) ?: true,
            sevenTvEmotes = preferences.readSetting(CHAT_SEVEN_TV_EMOTES) ?: true
        )
    }

    suspend fun setChatSettings(settings: ChatSettings) {
        context.dataStore.edit {
            it[CHAT_TEXT_SCALE] = (settings.textScale * 100).toInt().coerceIn(85, 130)
            it[CHAT_GLOBAL_BADGES] = settings.showGlobalBadges
            it[CHAT_CHANNEL_BADGES] = settings.showChannelBadges
            it[CHAT_OFFICIAL_NAME_COLOR] = settings.useOfficialNameColor
            it[CHAT_ANIMATED_EMOTES] = settings.animatedEmotes
            it[CHAT_SEVEN_TV_EMOTES] = settings.sevenTvEmotes
        }
    }

    val homeGridModeFlow: Flow<Boolean> = settings.map { preferences ->
        preferences.readSetting(HOME_GRID_MODE) ?: false
    }

    suspend fun setHomeGridMode(isGridMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HOME_GRID_MODE] = isGridMode
        }
    }

    val homeStreamSortFlow: Flow<String> = settings.map { preferences ->
        preferences.readSetting(HOME_STREAM_SORT) ?: "viewer_count_desc"
    }

    suspend fun setHomeStreamSort(sort: String) {
        context.dataStore.edit { preferences ->
            preferences[HOME_STREAM_SORT] = sort
        }
    }

    val appThemeFlow: Flow<AppTheme> = settings.map { preferences ->
        val stored = preferences.readSetting(APP_THEME)
        AppTheme.entries.firstOrNull { it.name == stored } ?: AppTheme.DARK
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[APP_THEME] = theme.name
        }
    }

    val appAccentFlow: Flow<AppAccent> = settings.map { preferences ->
        val stored = preferences.readSetting(APP_ACCENT)
        AppAccent.entries.firstOrNull { it.name == stored } ?: AppAccent.MAUVE
    }

    suspend fun setAppAccent(accent: AppAccent) {
        context.dataStore.edit { preferences ->
            preferences[APP_ACCENT] = accent.name
        }
    }

    val useDynamicColorsFlow: Flow<Boolean> = settings.map { preferences ->
        preferences.readSetting(USE_DYNAMIC_COLORS) ?: false
    }

    suspend fun setUseDynamicColors(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_DYNAMIC_COLORS] = use
        }
    }

    val notificationsEnabledFlow: Flow<Boolean> = settings.map { preferences ->
        preferences.readSetting(NOTIFICATIONS_ENABLED) ?: true
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    val hideSlotsFlow: Flow<Boolean> = settings.map { preferences ->
        preferences.readSetting(HIDE_SLOTS_KEY) ?: false
    }

    suspend fun setHideSlots(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIDE_SLOTS_KEY] = hide
        }
    }

    val hidePoolsFlow: Flow<Boolean> = settings.map { preferences ->
        preferences.readSetting(HIDE_POOLS_KEY) ?: false
    }

    suspend fun setHidePools(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIDE_POOLS_KEY] = hide
        }
    }

    val hideCryptoFlow: Flow<Boolean> = settings.map { preferences ->
        preferences.readSetting(HIDE_CRYPTO_KEY) ?: false
    }

    suspend fun setHideCrypto(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIDE_CRYPTO_KEY] = hide
        }
    }

    val selectedLanguagesFlow: Flow<Set<String>> =
        settings.map { preferences ->
            preferences.readSetting(SELECTED_LANGUAGES)
                ?: setOf(Locale.getDefault().language)
        }

    suspend fun updateSelectedLanguages(languages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_LANGUAGES] = languages
        }
    }
}
