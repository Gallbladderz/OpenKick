/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.gallbladderz.openkick.features.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gallbladderz.openkick.core.datastore.ChatSettings
import com.gallbladderz.openkick.core.domain.chat.ChatEmoteSet
import com.gallbladderz.openkick.core.domain.chat.ChatMessage
import com.gallbladderz.openkick.core.domain.chat.PinnedChatMessage
import com.gallbladderz.openkick.core.domain.chat.ChatMode
import com.gallbladderz.openkick.core.domain.model.ChannelLink
import com.gallbladderz.openkick.features.player.ChatSendState

@Suppress("UNUSED_PARAMETER")
@Composable
fun PlayerTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<String>,
    chatMessages: List<ChatMessage>,
    pinnedMessage: PinnedChatMessage?,
    onTogglePinnedMessage: () -> Unit,
    channelLinks: List<ChannelLink>,
    onSendMessage: (String, ChatMessage?) -> Unit,
    chatSendState: ChatSendState,
    chatEmoteSets: List<ChatEmoteSet> = emptyList(),
    chatMode: ChatMode = ChatMode(),
    isAuthenticated: Boolean,
    chatSettings: ChatSettings = ChatSettings(),
    onChatSettingsClick: () -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
    ) {
        ChatList(
            chatMessages = chatMessages,
            pinnedMessage = pinnedMessage,
            onTogglePinnedMessage = onTogglePinnedMessage,
            onSendMessage = onSendMessage,
            chatSendState = chatSendState,
            emoteSets = chatEmoteSets,
            chatMode = chatMode,
            isAuthenticated = isAuthenticated,
            chatSettings = chatSettings,
            onSettingsClick = onChatSettingsClick,
            onProfileClick = onProfileClick
        )
    }
}
