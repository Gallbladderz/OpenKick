/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Playing(
        val url: String,
        val avatarUrl: String,
        val viewers: Int,
        val title: String,
        val categoryName: String?,
        val categorySlug: String?
    ) : PlayerUiState

    data class Error(val message: String) : PlayerUiState
}
