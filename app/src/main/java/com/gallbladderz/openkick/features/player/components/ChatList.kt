/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.features.player.components


import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.datastore.ChatSettings
import com.gallbladderz.openkick.core.domain.chat.ChatBadge
import com.gallbladderz.openkick.core.domain.chat.ChatBadgeKind
import com.gallbladderz.openkick.core.domain.chat.ChatEmoteParser
import com.gallbladderz.openkick.core.domain.chat.ChatEmoteSet
import com.gallbladderz.openkick.core.domain.chat.ChatMessage
import com.gallbladderz.openkick.core.domain.chat.ChatMode
import com.gallbladderz.openkick.core.domain.chat.ChatModeType
import com.gallbladderz.openkick.core.domain.chat.ChatToken
import com.gallbladderz.openkick.core.network.api.KickApiService
import com.gallbladderz.openkick.core.ui.components.KickAvatar
import com.gallbladderz.openkick.features.player.ChatSendState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatList(
    chatMessages: List<ChatMessage>,
    onSendMessage: (String, ChatMessage?) -> Unit,
    chatSendState: ChatSendState,
    isAuthenticated: Boolean,
    chatMode: ChatMode = ChatMode(),
    emoteSets: List<ChatEmoteSet> = emptyList(),
    chatSettings: ChatSettings = ChatSettings(),
    onSettingsClick: () -> Unit = {},
    onProfileClick: (String) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isAutoScrolling by remember { mutableStateOf(true) }
    var replyToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    val context = LocalContext.current
    val kickApi: KickApiService = koinInject()
    var selectedAvatarUrl by remember { mutableStateOf("") }

    LaunchedEffect(selectedMessage?.id) {
        val message = selectedMessage ?: return@LaunchedEffect
        selectedAvatarUrl = message.avatarUrl
        if (selectedAvatarUrl.isBlank()) {
            selectedAvatarUrl = runCatching {
                kickApi.getChannelStreamInfo(message.sender).user?.profile_pic.orEmpty()
            }.getOrDefault("")
        }
    }

    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) replyToMessage = null
    }

    val interaction by listState.interactionSource.interactions.collectAsState(initial = null)
    LaunchedEffect(interaction) {
        if (interaction is DragInteraction.Start) isAutoScrolling = false
    }

    val isAtBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }
    LaunchedEffect(isAtBottom) {
        if (isAtBottom) isAutoScrolling = true
    }
    LaunchedEffect(chatMessages.size, chatMessages.firstOrNull()?.id) {
        if (isAutoScrolling && chatMessages.isNotEmpty()) listState.scrollToItem(0)
    }

    val canChatSend = isAuthenticated && when (chatMode.type) {
        ChatModeType.OPEN, ChatModeType.SLOW -> true
        ChatModeType.SUBSCRIBERS -> chatMode.isSubscriber == true
        ChatModeType.DISABLED -> false
    }
    val inputDisabledMessage = when {
        !isAuthenticated -> null
        chatMode.type == ChatModeType.SUBSCRIBERS && !canChatSend ->
            stringResource(R.string.chat_subscriber_required)

        chatMode.type == ChatModeType.DISABLED -> stringResource(R.string.chat_disabled_hint)
        else -> null
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .imePadding()) {
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                items(
                    chatMessages,
                    key = { "${it.id}_${chatSettings.animatedEmotes}" }) { message ->
                    ChatMessageItem(
                        message = message,
                        settings = chatSettings,
                        animateEmotes = chatSettings.animatedEmotes,
                        canReply = isAuthenticated,
                        onReplyClick = { selectedMessage = message }
                    )
                }
            }

            if (chatMessages.isEmpty()) {
                Text(
                    text = stringResource(R.string.chat_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = !isAutoScrolling,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 14.dp, end = 14.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = {
                        isAutoScrolling = true
                        coroutineScope.launch { listState.scrollToItem(0) }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.chat_scroll_to_latest),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (chatMode.type != ChatModeType.OPEN) {
            ChatModeBanner(chatMode = chatMode, canSend = canChatSend)
        }

        ChatInput(
            replyToMessage = replyToMessage,
            onClearReply = { replyToMessage = null },
            chatSendState = chatSendState,
            emoteSets = emoteSets,
            animateEmotes = chatSettings.animatedEmotes,
            isEnabled = canChatSend,
            disabledMessage = inputDisabledMessage,
            onSendMessage = { content, reply ->
                onSendMessage(content, reply)
                replyToMessage = null
            },
            onSettingsClick = onSettingsClick
        )
    }

    selectedMessage?.let { message ->
        ModalBottomSheet(
            onDismissRequest = { selectedMessage = null },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KickAvatar(
                        avatarUrl = selectedAvatarUrl,
                        streamerName = message.sender,
                        size = 48.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            visibleBadges(message, chatSettings).forEach { badge ->
                                ChatBadgeView(badge = badge, textScale = chatSettings.textScale)
                            }
                            Text(
                                text = message.sender,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                ChatMessageRichText(
                    message = message,
                    settings = chatSettings,
                    animateEmotes = chatSettings.animatedEmotes,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 18.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = {
                        selectedMessage = null
                        onProfileClick(message.sender)
                    }) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.chat_message_profile_action))
                    }
                    TextButton(onClick = {
                        val clipboard =
                            context.getSystemService(android.content.ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(
                            android.content.ClipData.newPlainText(message.sender, message.content)
                        )
                        selectedMessage = null
                        android.widget.Toast.makeText(
                            context,
                            R.string.copied_to_clipboard,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.chat_message_copy_action))
                    }
                    TextButton(
                        enabled = isAuthenticated,
                        onClick = {
                            replyToMessage = message
                            selectedMessage = null
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.chat_reply_action_short))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatModeBanner(chatMode: ChatMode, canSend: Boolean) {
    val text = when (chatMode.type) {
        ChatModeType.SLOW -> stringResource(
            R.string.chat_slow_mode,
            chatMode.slowModeSeconds.coerceAtLeast(1)
        )

        ChatModeType.SUBSCRIBERS -> if (canSend) {
            stringResource(R.string.chat_subscribers_only)
        } else {
            stringResource(R.string.chat_subscriber_required)
        }

        ChatModeType.DISABLED -> stringResource(R.string.chat_disabled)
        ChatModeType.OPEN -> return
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(horizontal = 16.dp, vertical = 7.dp)
    )
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    settings: ChatSettings = ChatSettings(),
    animateEmotes: Boolean = true,
    canReply: Boolean = true,
    onReplyClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val nameColor = remember(message.senderColor, colors.primary) {
        val sourceColor = message.senderColor
        if (sourceColor.isNotBlank()) {
            runCatching { Color(android.graphics.Color.parseColor(sourceColor)) }
                .getOrDefault(colors.primary)
        } else colors.primary
    }

    if (message.sender == context.getString(R.string.chat_system_sender)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier
                    .background(colors.surface.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
        return
    }

    val visibleBadges = visibleBadges(message, settings)
    val messageFontSize = (14f * settings.textScale).sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReplyClick() }
            .padding(horizontal = 12.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            visibleBadges.forEach { badge ->
                ChatBadgeView(badge = badge, textScale = settings.textScale)
            }
            Text(
                text = message.sender,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = (13f * settings.textScale).sp),
                color = if (settings.useOfficialNameColor) nameColor else colors.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (message.isPending) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = colors.primary
                )
            } else if (canReply) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = stringResource(R.string.chat_reply_action),
                    tint = colors.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .size(14.dp)
                )
            }
        }

        message.replyTo?.let { reply ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 3.dp, bottom = 3.dp)
                    .background(colors.primary.copy(alpha = 0.08f), RoundedCornerShape(7.dp))
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(24.dp)
                        .background(colors.primary, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (reply.senderName.isBlank()) {
                        stringResource(R.string.chat_original_message)
                    } else {
                        stringResource(R.string.chat_message_reply_to, reply.senderName)
                    } + if (reply.content.isNotBlank()) " · ${reply.content}" else "",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = (11f * settings.textScale).sp),
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        ChatMessageRichText(
            message = message,
            settings = settings,
            animateEmotes = animateEmotes,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = messageFontSize),
            color = colors.onSurface.copy(alpha = if (message.isPending) 0.6f else 1f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun visibleBadges(
    message: ChatMessage,
    settings: ChatSettings
): List<ChatBadge> = message.badges.filter { badge ->
    (badge.kind == ChatBadgeKind.GLOBAL && settings.showGlobalBadges) ||
            (badge.kind == ChatBadgeKind.CHANNEL && settings.showChannelBadges)
}

@Composable
private fun ChatMessageRichText(
    message: ChatMessage,
    settings: ChatSettings,
    animateEmotes: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val inlineContentMap = mutableMapOf<String, InlineTextContent>()
    val tokens = if (message.tokens.isEmpty()) {
        ChatEmoteParser.parseEmotes(message.content)
    } else {
        message.tokens
    }
    val messageText = buildAnnotatedString {
        if (tokens.isEmpty()) append(message.content)
        for (token in tokens) {
            when (token) {
                is ChatToken.Text -> append(token.text)
                is ChatToken.Emote -> {
                    val inlineId = "emote_${token.emoteId}"
                    appendInlineContent(inlineId, token.emoteName)
                    if (!inlineContentMap.containsKey(inlineId)) {
                        inlineContentMap[inlineId] = InlineTextContent(
                            Placeholder(
                                width = (22f * settings.textScale).sp,
                                height = (22f * settings.textScale).sp,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                            )
                        ) {
                            AsyncImage(
                                model = kickEmoteImageRequest(
                                    context = context,
                                    emoteId = token.emoteId,
                                    animated = animateEmotes
                                ),
                                contentDescription = token.emoteName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    Text(
        text = messageText,
        inlineContent = inlineContentMap,
        modifier = modifier,
        style = style,
        color = color
    )
}

@Composable
private fun ChatBadgeView(
    badge: com.gallbladderz.openkick.core.domain.chat.ChatBadge,
    textScale: Float
) {
    val colors = MaterialTheme.colorScheme
    if (!badge.imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = badge.imageUrl,
            contentDescription = badge.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(end = 3.dp)
                .size((15f * textScale).dp)
        )
    } else {
        Text(
            text = badge.name.take(1).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = (9f * textScale).sp),
            color = colors.onPrimary,
            modifier = Modifier
                .padding(end = 3.dp)
                .background(colors.primary, RoundedCornerShape(3.dp))
                .padding(horizontal = 3.dp, vertical = 1.dp)
        )
    }
}
