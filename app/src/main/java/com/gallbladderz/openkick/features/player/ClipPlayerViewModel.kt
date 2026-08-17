/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallbladderz.openkick.features.home.ClipUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClipPlayerViewModel(
    private val clipRepository: ClipRepository
) : ViewModel() {

    private val _avatarUrl = MutableStateFlow("")
    val avatarUrl = _avatarUrl.asStateFlow()

    private val _activeClip = MutableStateFlow<ClipUiModel?>(null)
    val activeClip = _activeClip.asStateFlow()

    fun loadClip(clipId: String) {
        viewModelScope.launch {
            val result = clipRepository.fetchClip(clipId)
            result.onSuccess { clip ->
                _activeClip.update { clip }
                _avatarUrl.update { clip.streamerAvatarUrl }
            }.onFailure {

            }
        }
    }
}
