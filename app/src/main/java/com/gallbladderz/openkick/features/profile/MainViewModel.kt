package com.gallbladderz.openkick.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.core.datastore.SettingsRepository
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

    fun toggleCategories(hide: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideCategories(hide)
        }
    }

    fun updateSelectedLanguages(languages: Set<String>) {
        viewModelScope.launch {
            settingsRepository.updateSelectedLanguages(languages)
        }
    }
}