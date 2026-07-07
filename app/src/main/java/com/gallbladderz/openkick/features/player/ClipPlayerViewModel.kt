package com.gallbladderz.openkick.features.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.features.profile.StreamerProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClipPlayerViewModel(
    private val profileRepository: StreamerProfileRepository
) : ViewModel() {

    private val _avatarUrl = MutableStateFlow("")
    val avatarUrl = _avatarUrl.asStateFlow()

    private var loadedSlug: String? = null

    fun loadAvatar(slug: String) {
        if (slug.isBlank() || loadedSlug == slug) return
        loadedSlug = slug

        viewModelScope.launch {
            profileRepository.fetchProfileInfo(slug)
                .getOrNull()
                ?.avatarUrl
                ?.replace("\\/", "/")
                ?.takeIf { it.isNotBlank() }
                ?.let { _avatarUrl.value = it }
        }
    }
}
