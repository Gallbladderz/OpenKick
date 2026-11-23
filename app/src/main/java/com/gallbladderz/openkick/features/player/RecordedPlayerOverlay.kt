/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gallbladderz.openkick.core.player.PlayerExpandedState
import com.gallbladderz.openkick.core.player.PlayerMedia
import com.gallbladderz.openkick.features.downloads.OfflineDownloadScreen
import com.gallbladderz.openkick.features.player.vod.VodPlayerRoute

@Composable
internal fun RecordedPlayerOverlay(
    recording: PlayerMedia.Recording,
    dragState: AnchoredDraggableState<PlayerExpandedState>,
    onClose: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    key(recording) {
        val owner = remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
        DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
            when (recording) {
                is PlayerMedia.Recording.Clip -> ClipPlayerRoute(
                    recording.id, dragState, onClose, onProfileClick
                )
                is PlayerMedia.Recording.Vod -> VodPlayerRoute(
                    recording.id, recording.channelSlug, dragState, onClose, onProfileClick
                )
                is PlayerMedia.Recording.Download -> OfflineDownloadScreen(
                    recording.id, dragState, onClose
                )
            }
        }
    }
}
