package com.gallbladderz.openkick.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "kick_settings")

class SettingsRepository(private val context: Context) {
    private val HIDE_CATEGORIES_KEY = booleanPreferencesKey("hide_categories")

    val hideCategoriesFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HIDE_CATEGORIES_KEY] ?: false
    }

    suspend fun setHideCategories(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIDE_CATEGORIES_KEY] = hide
        }
    }
}