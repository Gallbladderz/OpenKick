package com.gallbladderz.openkick.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.core.datastore.SettingsRepository
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val hideCategories: StateFlow<Boolean> =
        settingsRepository.hideCategoriesFlow
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

    val appLanguage: String
        get() = AppCompatDelegate.getApplicationLocales().toLanguageTags().let { if (it.isEmpty()) "en" else it.split("-")[0] }

    fun toggleCategories(hide: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideCategories(hide)
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

    fun changeAppLanguage(code: String) {
        val appLocale = LocaleListCompat.forLanguageTags(code)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}