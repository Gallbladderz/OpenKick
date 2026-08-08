package com.gallbladderz.openkick.features.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.features.profile.StreamerProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClipPlayerViewModel(
    private val profileRepository: StreamerProfileRepository,
    private val clipRepository: ClipRepository
) : ViewModel() {

    private val _avatarUrl = MutableStateFlow("")
    val avatarUrl = _avatarUrl.asStateFlow()

    private val _activeClip = MutableStateFlow<com.gallbladderz.openkick.features.home.ClipUiModel?>(null)
    val activeClip = _activeClip.asStateFlow()

    private var loadedSlug: String? = null

    fun loadClip(clipId: String) {
        val clip = clipRepository.getClipById(clipId)
        if (clip != null) {
            _activeClip.value = clip
            loadAvatar(clip.streamerName)
        }
    }

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
