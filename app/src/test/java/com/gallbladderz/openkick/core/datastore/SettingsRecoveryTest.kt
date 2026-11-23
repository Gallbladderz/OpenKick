/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRecoveryTest {
    @Test
    fun corruptFileGetsEmptyReplacementPreferences() = runBlocking {
        val replacement = settingsCorruptionHandler().handleCorruption(CorruptionException("Invalid preferences"))
        assertTrue(replacement.asMap().isEmpty())
    }

    @Test
    fun wrongTypeDoesNotDiscardOtherSettings() {
        val theme = stringPreferencesKey("app_theme")
        val settings = preferencesOf(booleanPreferencesKey("app_theme") to true, stringPreferencesKey("sort") to "viewer_count_desc")
        assertNull(settings.readSetting(theme))
        assertEquals("viewer_count_desc", settings.readSetting(stringPreferencesKey("sort")))
    }
}
