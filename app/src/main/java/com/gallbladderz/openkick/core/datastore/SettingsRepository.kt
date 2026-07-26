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
        private val HIDE_CATEGORIES_KEY = booleanPreferencesKey("hide_categories")
        private val FOLLOWED_CHANNELS = stringSetPreferencesKey("followed_channels")
        private val SELECTED_LANGUAGES = stringSetPreferencesKey("selected_languages")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val BACKGROUND_KEEPALIVE = booleanPreferencesKey("background_keepalive")
        private val APP_THEME = stringPreferencesKey("app_theme")
        private val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
    }

    val appThemeFlow: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        val themeName = preferences[APP_THEME] ?: AppTheme.SYSTEM.name
        try {
            AppTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppTheme.SYSTEM
        }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[APP_THEME] = theme.name
        }
    }

    val useDynamicColorsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_DYNAMIC_COLORS] ?: true
    }

    suspend fun setUseDynamicColors(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_DYNAMIC_COLORS] = use
        }
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED] ?: true 
    }

    val backgroundKeepaliveFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BACKGROUND_KEEPALIVE] ?: false 
    }

    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setBackgroundKeepalive(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BACKGROUND_KEEPALIVE] = enabled
        }
    }

    val hideCategoriesFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HIDE_CATEGORIES_KEY] ?: false
    }

    suspend fun setHideCategories(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIDE_CATEGORIES_KEY] = hide
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