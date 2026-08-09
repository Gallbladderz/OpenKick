/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.profile

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.core.datastore.AppAccent
import com.gallbladderz.openkick.core.datastore.AppTheme
import com.gallbladderz.openkick.core.datastore.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val hideSlots: StateFlow<Boolean> =
        settingsRepository.hideSlotsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )

    val hidePools: StateFlow<Boolean> =
        settingsRepository.hidePoolsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )

    val hideCrypto: StateFlow<Boolean> =
        settingsRepository.hideCryptoFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )

    val selectedLanguages: StateFlow<Set<String>> =
        settingsRepository.selectedLanguagesFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptySet()
            )

    val appTheme: StateFlow<AppTheme> =
        settingsRepository.appThemeFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AppTheme.DARK
            )

    val useDynamicColors: StateFlow<Boolean> =
        settingsRepository.useDynamicColorsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )

    val appLanguage: String
        get() = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            .let { if (it.isEmpty()) "en" else it.split("-")[0] }

    fun updateAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.setAppTheme(theme)
        }
    }

    val appAccent: StateFlow<AppAccent> =
        settingsRepository.appAccentFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AppAccent.MAUVE
            )

    fun updateAppAccent(accent: AppAccent) {
        viewModelScope.launch {
            settingsRepository.setAppAccent(accent)
        }
    }

    fun updateUseDynamicColors(use: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUseDynamicColors(use)
        }
    }

    fun toggleSlots(hide: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideSlots(hide)
        }
    }

    fun togglePools(hide: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHidePools(hide)
        }
    }

    fun toggleCrypto(hide: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideCrypto(hide)
        }
    }

    fun toggleLanguage(code: String, isChecked: Boolean) {
        viewModelScope.launch {
            val currentSelection = selectedLanguages.value
            val newSelection = if (isChecked) {
                currentSelection + code
            } else {
                currentSelection - code
            }
            settingsRepository.updateSelectedLanguages(newSelection)
        }
    }

    val notificationsEnabled: StateFlow<Boolean> =
        settingsRepository.notificationsEnabledFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true
            )

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }
}