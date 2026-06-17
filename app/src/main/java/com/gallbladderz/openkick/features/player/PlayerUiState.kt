package com.gallbladderz.openkick.features.player

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Playing(
        val url: String,
        val avatarUrl: String,
        val viewers: Int,
        val title: String
    ) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}
