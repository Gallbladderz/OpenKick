/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.gallbladderz.openkick.features.player.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
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
import com.gallbladderz.openkick.core.domain.chat.PinnedChatMessage
import com.gallbladderz.openkick.core.domain.chat.ChatMode
import com.gallbladderz.openkick.core.domain.chat.ChatModeType
import com.gallbladderz.openkick.core.network.api.KickApiService
import com.gallbladderz.openkick.core.ui.components.KickAvatar
import com.gallbladderz.openkick.features.player.ChatSendState
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * True while chat is rendered directly over video (fullscreen floating
 * panel in Transparent or Semi opacity mode). ChatMessageItem then
 * renders white text with a black drop shadow instead of theme colours,
 * so messages stay legible over bright frames in either theme.
 * Defaults to false: portrait chat and the live chat list are untouched.
 * Provided by FloatingChatOverlay; consumed by ChatMessageItem and by
 * the replay panels (which skip their duplicate title in this mode).
 */
internal val LocalChatReadableOverVideo = compositionLocalOf { false }

/** Black drop shadow used for chat text rendered directly over video. */
internal val ChatOverVideoShadow = Shadow(
    color = Color.Black,
    offset = Offset(1f, 1f),
    blurRadius = 4f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatList(
    chatMessages: List<ChatMessage>,
    pinnedMessage: PinnedChatMessage?,
    onTogglePinnedMessage: () -> Unit,
    onSendMessage: (String, ChatMessage?) -> Unit,
    chatSendState: ChatSendState,
    isAuthenticated: Boolean,
    chatMode: ChatMode = ChatMode(),
    emoteSets: List<ChatEmoteSet> = emptyList(),
    chatSettings: ChatSettings = ChatSettings(),
    onSettingsClick: () -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    isCompactOverlay: Boolean = false
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isAutoScrolling by remember { mutableStateOf(true) }
    var isReturningToLatest by remember { mutableStateOf(false) }
    var unreadCount by remember { mutableIntStateOf(0) }
    var previousMessages by remember { mutableStateOf(emptyList<ChatMessage>()) }
    var replyToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    val latestMessageId = chatMessages.firstOrNull()?.id
    val topFade = remember {
        Brush.verticalGradient(0f to Color.Transparent, 0.06f to Color.Black)
    }
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

    LaunchedEffect(listState) {
        launch {
            listState.interactionSource.interactions.collect { interaction ->
                if (interaction is DragInteraction.Start &&
                    (listState.canScrollForward || listState.canScrollBackward)
                ) {
                    isAutoScrolling = false
                }
            }
        }
        snapshotFlow {
            !listState.isScrollInProgress &&
                listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset == 0
        }.collect { isAtBottom ->
            if (isAtBottom) isAutoScrolling = true
        }
    }

    LaunchedEffect(latestMessageId, isAutoScrolling) {
        if (isAutoScrolling || chatMessages.isEmpty()) {
            unreadCount = 0
        } else if (latestMessageId != previousMessages.firstOrNull()?.id) {
            val previousIds = previousMessages.mapTo(HashSet(previousMessages.size)) { it.id }
            unreadCount += chatMessages.count { !it.isPending && it.id !in previousIds }
        }
        previousMessages = chatMessages

        if (isAutoScrolling && chatMessages.isNotEmpty()) {
            listState.requestScrollToItem(0)
        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        if (pinnedMessage != null && !pinnedMessage.isHidden) {
            PinnedMessageBanner(
                pin = pinnedMessage,
                settings = chatSettings,
                compact = isCompactOverlay,
                onSenderClick = { selectedMessage = pinnedMessage.message }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = topFade, blendMode = BlendMode.DstIn)
                    },
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = if (isCompactOverlay) 6.dp else 10.dp)
            ) {
                items(chatMessages, key = { it.id }) { message ->
                    ChatMessageItem(
                        message = message,
                        settings = chatSettings,
                        animateEmotes = chatSettings.animatedEmotes,
                        canReply = isAuthenticated,
                        onReplyClick = { selectedMessage = message },
                        modifier = Modifier.animateItem(),
                        onSwipeReply = { replyToMessage = message }
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
                visible = !isAutoScrolling && chatMessages.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    enabled = !isReturningToLatest,
                    onClick = {
                        isReturningToLatest = true
                        coroutineScope.launch {
                            try {
                                if (listState.firstVisibleItemIndex < 15) {
                                    listState.animateScrollToItem(0)
                                } else {
                                    listState.scrollToItem(0)
                                }
                                isAutoScrolling = true
                            } finally {
                                isReturningToLatest = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.chat_scroll_to_latest),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (unreadCount > 0) {
                                "Новых сообщений: $unreadCount"
                            } else {
                                "Чат на паузе"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        ChatModeBanner(chatMode = chatMode, canSend = canChatSend)
        if (isAuthenticated && chatMode.followersOnly && chatMode.isFollower == false &&
            chatMode.type != ChatModeType.DISABLED
        ) {
            ChatStatusBanner(Icons.Default.Lock, stringResource(R.string.chat_followers_only))
        }

        ChatInput(
            replyToMessage = replyToMessage,
            onClearReply = { replyToMessage = null },
            chatSendState = chatSendState,
            isAuthenticated = isAuthenticated,
            emoteSets = emoteSets,
            animateEmotes = chatSettings.animatedEmotes,
            isEnabled = canChatSend,
            disabledMessage = inputDisabledMessage,
            onSendMessage = { content, reply ->
                onSendMessage(content, reply)
                replyToMessage = null
            },
            onSettingsClick = onSettingsClick,
            pinnedMessageVisible = pinnedMessage?.let { !it.isHidden },
            onTogglePinnedMessage = onTogglePinnedMessage,
            slowModeSeconds = if (chatMode.type == ChatModeType.SLOW) chatMode.slowModeSeconds else 0
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
                    showSender = false,
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
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText(message.sender, message.content)
                        )
                        selectedMessage = null
                        Toast.makeText(
                            context,
                            R.string.copied_to_clipboard,
                            Toast.LENGTH_SHORT
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
    val (icon, text) = when (chatMode.type) {
        ChatModeType.SLOW -> Icons.Default.Timer to stringResource(
            R.string.chat_slow_mode,
            chatMode.slowModeSeconds.coerceAtLeast(1)
        )

        ChatModeType.SUBSCRIBERS -> Icons.Default.Lock to stringResource(
            if (canSend) R.string.chat_subscribers_only else R.string.chat_subscriber_required
        )

        ChatModeType.DISABLED -> Icons.Default.Block to stringResource(R.string.chat_disabled)
        ChatModeType.OPEN -> return
    }
    ChatStatusBanner(icon, text)
}

@Composable
private fun ChatStatusBanner(icon: ImageVector, text: String) {
    Surface(
        modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 2.dp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    settings: ChatSettings = ChatSettings(),
    animateEmotes: Boolean = true,
    canReply: Boolean = true,
    onReplyClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onSwipeReply: (() -> Unit)? = null,
    readableOverVideo: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    // Explicit flag wins; otherwise follow the ambient fullscreen-overlay
    // local (true only inside the transparent/semi floating panel).
    val overVideo = readableOverVideo || LocalChatReadableOverVideo.current

    if (message.sender == stringResource(R.string.chat_system_sender)) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.labelSmall.copy(
                    shadow = if (overVideo) ChatOverVideoShadow else null
                ),
                color = if (overVideo) Color.White else colors.onSurfaceVariant,
                modifier = Modifier
                    .background(
                        if (overVideo) Color.Black.copy(alpha = 0.45f)
                        else colors.surface.copy(alpha = 0.9f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
        return
    }

    val messageAlpha = if (message.isPending) 0.55f else 1f
    val messageLayer = when {
        canReply && onSwipeReply != null -> Modifier.swipeToReply(messageAlpha, onSwipeReply)
        message.isPending -> Modifier.graphicsLayer { alpha = messageAlpha }
        else -> Modifier
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onReplyClick == null) Modifier else Modifier.clickable(onClick = onReplyClick))
            .padding(horizontal = 12.dp)
            .then(messageLayer)
            .padding(vertical = 3.dp)
    ) {
        message.replyTo?.let { reply ->
            val originalSender = if (reply.senderName.isBlank()) {
                stringResource(R.string.chat_original_message)
            } else {
                "@${reply.senderName}"
            }
            val prefix = if (reply.content.isBlank()) originalSender else "$originalSender: "
            Row(
                modifier = Modifier.padding(bottom = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.offset(y = 1.dp).size(12.dp)
                )
                ChatEmotePreview(
                    prefix = AnnotatedString(prefix),
                    content = reply.content,
                    animateEmotes = animateEmotes,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        shadow = if (overVideo) ChatOverVideoShadow else null
                    ),
                    color = if (overVideo) Color.White.copy(alpha = 0.9f)
                    else colors.onSurfaceVariant.copy(alpha = 0.75f),
                    emoteSize = 14.sp
                )
            }
        }

        ChatMessageRichText(
            message = message,
            settings = settings,
            animateEmotes = animateEmotes,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14f * settings.textScale).sp),
            modifier = Modifier.fillMaxWidth(),
            readableOverVideo = overVideo
        )
    }
}

@Composable
private fun Modifier.swipeToReply(messageAlpha: Float, onReply: () -> Unit): Modifier {
    val haptics = LocalHapticFeedback.current
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Ltr) -1f else 1f
    val currentOnReply by rememberUpdatedState(onReply)
    var offset by remember { mutableFloatStateOf(0f) }
    val returnAnimation = remember { Animatable(0f) }

    return pointerInput(haptics, direction) {
        val maxOffset = 56.dp.toPx()
        val replyThreshold = 40.dp.toPx()
        try {
            coroutineScope {
                var returnJob: Job? = null
                var dragDistance = 0f
                var replyTriggered = false

                fun springBack() {
                    returnJob = launch {
                        returnAnimation.snapTo(offset)
                        returnAnimation.animateTo(0f) { offset = value }
                        offset = 0f
                    }
                }

                detectHorizontalDragGestures(
                    onDragStart = {
                        returnJob?.cancel()
                        dragDistance = 0f
                        replyTriggered = false
                    },
                    onDragEnd = ::springBack,
                    onDragCancel = ::springBack
                ) { _, dragAmount ->
                    val directedDelta = dragAmount * direction
                    dragDistance = (dragDistance + directedDelta).coerceIn(0f, maxOffset)
                    offset = (offset * direction + directedDelta)
                        .coerceIn(0f, maxOffset) * direction
                    if (!replyTriggered && dragDistance >= replyThreshold) {
                        replyTriggered = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnReply()
                    }
                }
            }
        } finally {
            offset = 0f
        }
    }.graphicsLayer {
        translationX = offset
        alpha = messageAlpha
    }
}

@Composable
private fun PinnedMessageBanner(
    pin: PinnedChatMessage,
    settings: ChatSettings,
    compact: Boolean,
    onSenderClick: () -> Unit
) {
    var expanded by rememberSaveable(pin.message.id) { mutableStateOf(false) }
    var canExpand by remember(pin.message.id) { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val bodyStyle = MaterialTheme.typography.bodyMedium
    val message = remember(pin.message) { pin.message.copy(badges = emptyList()) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = colors.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                if (pin.pinnedBy != null) {
                    visibleBadges(pin.pinnerBadges, settings).forEach { badge ->
                        ChatBadgeView(badge, settings.textScale)
                    }
                }
                Text(
                    text = if (pin.pinnedBy != null) {
                        stringResource(R.string.chat_pinned_by, pin.pinnedBy)
                    } else {
                        stringResource(R.string.chat_pinned_message)
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (canExpand || expanded) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) {
                                Icons.Filled.KeyboardArrowUp
                            } else {
                                Icons.Filled.KeyboardArrowDown
                            },
                            contentDescription = stringResource(
                                if (expanded) R.string.chat_pin_collapse else R.string.chat_pin_expand
                            )
                        )
                    }
                }
            }
            val bodyModifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            Box(
                modifier = if (expanded) {
                    bodyModifier
                        .heightIn(max = if (compact) 84.dp else 140.dp)
                        .verticalScroll(rememberScrollState())
                } else {
                    bodyModifier
                }
            ) {
                ChatMessageRichText(
                    message = message,
                    settings = settings,
                    animateEmotes = settings.animatedEmotes,
                    style = bodyStyle.copy(fontSize = bodyStyle.fontSize * settings.textScale),
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    onTextLayout = { if (!expanded) canExpand = it.hasVisualOverflow },
                    onSenderClick = onSenderClick
                )
            }
        }
    }
}

private fun visibleBadges(message: ChatMessage, settings: ChatSettings): List<ChatBadge> =
    visibleBadges(message.badges, settings)

private fun visibleBadges(
    badges: List<ChatBadge>,
    settings: ChatSettings
): List<ChatBadge> = badges.filter { badge ->
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
    showSender: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onSenderClick: (() -> Unit)? = null,
    readableOverVideo: Boolean = false
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val nameColor = remember(message.senderColor, colors.primary) {
        if (message.senderColor.isBlank()) {
            colors.primary
        } else {
            runCatching { Color(android.graphics.Color.parseColor(message.senderColor)) }
                .getOrDefault(colors.primary)
        }
    }
    val sevenTv = if (settings.sevenTvEmotes) LocalSevenTvCatalog.current?.byName.orEmpty() else emptyMap()
    val currentOnSenderClick by rememberUpdatedState(onSenderClick)
    val isSenderClickable = onSenderClick != null && message.sender.isNotBlank()
    val (messageText, inlineContentMap) = remember(
        message.id, message.isPending, settings, animateEmotes, nameColor,
        message, showSender, isSenderClickable, colors.primary, colors.onSurfaceVariant, context, sevenTv,
        readableOverVideo
    ) {
        val inlineContent = mutableMapOf<String, InlineTextContent>()
        val tokens = message.tokens.ifEmpty { ChatEmoteParser.parseEmotes(message.content) }
        val messageText = buildAnnotatedString {
            if (showSender) {
                visibleBadges(message, settings).forEachIndexed { index, badge ->
                    val inlineId = "badge_$index"
                    appendInlineContent(inlineId, badge.label(context))
                    val badgeSize = (16f * settings.textScale).sp
                    val badgeWidth = badge.giftedSubscriptions?.let {
                        ((22 + it.toString().length * 7) * settings.textScale).sp
                    } ?: badgeSize
                    inlineContent[inlineId] = InlineTextContent(
                        Placeholder(badgeWidth, badgeSize, PlaceholderVerticalAlign.Center)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                            ChatBadgeView(badge = badge, textScale = settings.textScale)
                        }
                    }
                }
                withStyle(
                    SpanStyle(
                        color = if (settings.useOfficialNameColor) nameColor else colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = (13.5f * settings.textScale).sp,
                        shadow = if (readableOverVideo) ChatOverVideoShadow else null
                    )
                ) {
                    if (isSenderClickable) {
                        withLink(LinkAnnotation.Clickable(tag = "chat_sender") { currentOnSenderClick?.invoke() }) {
                            append(message.sender)
                        }
                    } else {
                        append(message.sender)
                    }
                }
                withStyle(
                    SpanStyle(
                        color = if (readableOverVideo) Color.White else colors.onSurfaceVariant,
                        shadow = if (readableOverVideo) ChatOverVideoShadow else null
                    )
                ) {
                    append(": ")
                }
            }
            appendChatEmotes(tokens, sevenTv, inlineContent, (22f * settings.textScale).sp, animateEmotes)
            if (message.isPending) {
                append(" ")
                appendInlineContent("pending_spinner", "…")
                val spinnerSize = (12f * settings.textScale).sp
                inlineContent["pending_spinner"] = InlineTextContent(
                    Placeholder(spinnerSize, spinnerSize, PlaceholderVerticalAlign.TextCenter)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 1.5.dp,
                        color = colors.primary
                    )
                }
            }
        }
        messageText to inlineContent
    }

    Text(
        text = messageText,
        inlineContent = inlineContentMap,
        modifier = modifier,
        style = if (readableOverVideo) style.copy(shadow = ChatOverVideoShadow) else style,
        // Over video the message body is forced white (the theme's
        // onSurface can be dark in light theme); the shadow above keeps
        // it legible over bright frames.
        color = if (readableOverVideo) Color.White else colors.onSurface,
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
        onTextLayout = onTextLayout
    )
}

@Composable
private fun ChatBadgeView(badge: ChatBadge, textScale: Float) {
    val colors = MaterialTheme.colorScheme
    val giftCount = badge.giftedSubscriptions
    if (giftCount != null) {
        val label = badge.label(LocalContext.current)
        Row(
            modifier = Modifier
                .padding(end = 3.dp)
                .background(colors.primary.copy(alpha = 0.16f), RoundedCornerShape(4.dp))
                .padding(horizontal = 3.dp)
                .clearAndSetSemantics { contentDescription = label },
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CardGiftcard,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size((11f * textScale).dp)
            )
            Text(
                text = giftCount.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (10f * textScale).sp,
                    lineHeight = (14f * textScale).sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = colors.primary,
                maxLines = 1,
                softWrap = false
            )
        }
    } else if (!badge.imageUrl.isNullOrBlank()) {
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

private fun ChatBadge.label(context: Context): String =
    giftedSubscriptions?.let {
        context.resources.getQuantityString(R.plurals.chat_badge_gifted_subs, it, it)
    } ?: name
