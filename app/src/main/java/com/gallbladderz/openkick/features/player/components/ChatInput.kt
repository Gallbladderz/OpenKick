/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.gallbladderz.openkick.features.player.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.domain.chat.ChatDisplayEmote
import com.gallbladderz.openkick.core.domain.chat.ChatEmote
import com.gallbladderz.openkick.core.domain.chat.ChatEmoteSet
import com.gallbladderz.openkick.core.domain.chat.ChatMessage
import com.gallbladderz.openkick.core.domain.chat.toDisplayEmote
import com.gallbladderz.openkick.features.player.ChatSendState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun PinnedMessageToggle(visible: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Icon(
            imageVector = if (visible) Icons.Filled.PushPin else Icons.Outlined.PushPin,
            contentDescription = stringResource(
                if (visible) R.string.chat_pin_hide else R.string.chat_pin_show
            ),
            tint = if (visible) colors.primary else colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatInput(
    replyToMessage: ChatMessage?,
    onClearReply: () -> Unit,
    chatSendState: ChatSendState,
    isAuthenticated: Boolean,
    isEnabled: Boolean,
    emoteSets: List<ChatEmoteSet> = emptyList(),
    animateEmotes: Boolean = true,
    disabledMessage: String? = null,
    onSendMessage: (String, ChatMessage?) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    slowModeSeconds: Int = 0,
    pinnedMessageVisible: Boolean? = null,
    onTogglePinnedMessage: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    if (!isAuthenticated) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(start = 14.dp, end = 10.dp, top = 2.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.chat_sign_in_hint),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            if (pinnedMessageVisible != null) {
                PinnedMessageToggle(pinnedMessageVisible, onTogglePinnedMessage)
            }
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.chat_settings_title),
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        return
    }

    val inputValue = rememberTextFieldState()
    val inputScroll = rememberScrollState()
    var inputLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val inputText = inputValue.text
    val inputEmotes = remember(inputText) { draftEmotes(inputText) }
    var emojiMenuExpanded by remember { mutableStateOf(false) }
    var isEmoteSearchFocused by remember { mutableStateOf(false) }
    var recentEmotes by remember { mutableStateOf<List<ChatEmote>>(emptyList()) }
    var emoteUsage by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val inputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val imeHeight = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    val imeVisible = imeHeight > 0.dp
    val imeTargetHeightPx = WindowInsets.imeAnimationTarget.getBottom(density)
    var lastImeHeightPx by rememberSaveable(configuration.orientation) { mutableIntStateOf(0) }
    var imeWasVisible by remember { mutableStateOf(imeVisible) }
    var replacingKeyboard by remember { mutableStateOf(false) }
    val fullPanelHeight = if (lastImeHeightPx > 0) with(density) { lastImeHeightPx.toDp() }
    else minOf(320.dp, (configuration.screenHeightDp * 0.42f).dp)
    val panelHeight = if (replacingKeyboard) (fullPanelHeight - imeHeight).coerceAtLeast(0.dp)
    else fullPanelHeight
    val isSending = chatSendState is ChatSendState.Sending
    var lastSendAtMillis by remember { mutableLongStateOf(-1L) }
    var secondsRemaining by remember { mutableIntStateOf(0) }
    val isCoolingDown = slowModeSeconds > 0 && secondsRemaining > 0
    val action = when {
        isSending -> ChatInputAction.Sending
        isEnabled && inputValue.text.isNotBlank() -> ChatInputAction.Send
        else -> ChatInputAction.Settings
    }
    val inputStyle = MaterialTheme.typography.bodyMedium

    LaunchedEffect(imeTargetHeightPx) {
        if (imeTargetHeightPx > 0) lastImeHeightPx = imeTargetHeightPx
    }

    LaunchedEffect(imeVisible) {
        if (imeVisible && !imeWasVisible && emojiMenuExpanded && !isEmoteSearchFocused) {
            emojiMenuExpanded = false
        }
        if (!imeVisible) replacingKeyboard = false
        imeWasVisible = imeVisible
    }

    LaunchedEffect(emojiMenuExpanded) {
        if (!emojiMenuExpanded) isEmoteSearchFocused = false
    }

    LaunchedEffect(lastSendAtMillis, slowModeSeconds, chatSendState) {
        if (slowModeSeconds <= 0 || chatSendState is ChatSendState.Error) {
            lastSendAtMillis = -1L
            secondsRemaining = 0
            return@LaunchedEffect
        }
        if (lastSendAtMillis < 0) return@LaunchedEffect

        val deadline = lastSendAtMillis + slowModeSeconds * 1_000L
        while (true) {
            val millisRemaining = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            secondsRemaining = ((millisRemaining + 999L) / 1_000L).toInt()
            if (millisRemaining == 0L) break
            delay((millisRemaining - 1L) % 1_000L + 1L)
        }
    }

    fun send() {
        val message = inputValue.text.toString().trim()
        if (!isEnabled || isSending || message.isEmpty()) return
        if (slowModeSeconds > 0) {
            val now = SystemClock.elapsedRealtime()
            val cooldownMillis = slowModeSeconds * 1_000L
            if (lastSendAtMillis >= 0 && now - lastSendAtMillis < cooldownMillis) return
            lastSendAtMillis = now
            secondsRemaining = slowModeSeconds
        }
        onSendMessage(message, replyToMessage)
        inputValue.clearText()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
    ) {
        HorizontalDivider(color = colors.onSurface.copy(alpha = 0.08f))

        AnimatedContent(
            targetState = replyToMessage,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it?.id },
            label = "Chat reply preview"
        ) { reply ->
            if (reply != null) ChatReplyPreview(reply, animateEmotes, onClearReply)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .height(40.dp)
                .background(colors.surfaceContainerHigh, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                state = inputValue,
                inputTransformation = chatEmoteInput,
                outputTransformation = chatEmoteOutput,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(inputFocusRequester)
                    .pointerInput(emojiMenuExpanded) {
                        if (!emojiMenuExpanded) return@pointerInput
                        awaitPointerEventScope {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        }
                        emojiMenuExpanded = false
                    }
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && emojiMenuExpanded) {
                            emojiMenuExpanded = false
                        }
                    },
                enabled = !isSending,
                readOnly = !isEnabled,
                lineLimits = TextFieldLineLimits.SingleLine,
                // Editor layout adapted from QuinKick (https://github.com/kaws3664/QuinKick)
                // by OpenKick Translation and kaws3664, licensed under GPL-3.0-or-later.
                textStyle = inputStyle.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                scrollState = inputScroll,
                onTextLayout = { getResult -> inputLayout = getResult() },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                onKeyboardAction = { send() },
                decorator = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 14.dp, end = if (pinnedMessageVisible == null) 72.dp else 108.dp)
                            .clipToBounds(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = disabledMessage ?: stringResource(R.string.chat_message_hint),
                                style = inputStyle,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (inputEmotes.isNotEmpty()) {
                            inputLayout?.let { layout ->
                                ChatDraftEmoteLayer(inputEmotes, layout, inputScroll, animateEmotes)
                            }
                        }
                        Box(Modifier.fillMaxWidth(), propagateMinConstraints = true) {
                            innerTextField()
                        }
                    }
                }
            )

            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = {
                        if (emojiMenuExpanded) {
                            emojiMenuExpanded = false
                            inputFocusRequester.requestFocus()
                            keyboardController?.show()
                        } else {
                            replacingKeyboard = imeVisible
                            emojiMenuExpanded = true
                            keyboardController?.hide()
                        }
                    },
                    enabled = isEnabled && !isSending,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mood,
                        contentDescription = stringResource(R.string.chat_emoji_menu),
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (pinnedMessageVisible != null) {
                    PinnedMessageToggle(pinnedMessageVisible, onTogglePinnedMessage)
                }
                IconButton(
                    onClick = {
                        when (action) {
                            ChatInputAction.Send -> send()
                            ChatInputAction.Settings -> onSettingsClick()
                            ChatInputAction.Sending -> Unit
                        }
                    },
                    enabled = !isSending && (action != ChatInputAction.Send || !isCoolingDown),
                    modifier = Modifier.size(34.dp)
                ) {
                    AnimatedContent(
                        targetState = action,
                        transitionSpec = { (scaleIn() + fadeIn()) togetherWith fadeOut() },
                        contentAlignment = Alignment.Center,
                        label = "Chat input action"
                    ) { currentAction ->
                        ChatInputActionIcon(
                            action = currentAction,
                            secondsRemaining = if (isCoolingDown) secondsRemaining else 0
                        )
                    }
                }
            }
        }

        if (emojiMenuExpanded) {
            ChatEmotePickerPanel(
                emoteSets = emoteSets,
                recentEmotes = recentEmotes,
                height = panelHeight,
                searchFocused = isEmoteSearchFocused,
                animateEmotes = animateEmotes,
                onSearchFocusChanged = { isEmoteSearchFocused = it },
                onEmoteSelected = selectEmote@{ selected ->
                    if (selected.sevenTv != null) {
                        inputValue.edit {
                            val start = selection.min
                            val end = selection.max
                            val text = asCharSequence()
                            val prefix = if (start > 0 && !text[start - 1].isWhitespace()) " " else ""
                            val suffix = if (end == length || !text[end].isWhitespace()) " " else ""
                            val insertion = prefix + selected.name + suffix
                            replace(start, end, insertion)
                            selection = TextRange(start + insertion.length)
                        }
                        return@selectEmote
                    }
                    val emote = ChatEmote(selected.id, selected.name)
                    val updatedText = appendEmote(inputValue.text, emote)
                    inputValue.setTextAndPlaceCursorAtEnd(updatedText)
                    val updatedUsage = emoteUsage.toMutableMap().apply {
                        this[emote.id] = (this[emote.id] ?: 0) + 1
                    }
                    emoteUsage = updatedUsage
                    val recentOrder = listOf(emote) + recentEmotes.filterNot { it.id == emote.id }
                    recentEmotes = recentOrder
                        .sortedWith(
                            compareByDescending<ChatEmote> { updatedUsage[it.id] ?: 0 }
                                .thenBy { recentOrder.indexOf(it) }
                        )
                        .take(MAX_RECENT_EMOTES)
                }
            )
        }

        if (chatSendState is ChatSendState.Error) {
            Text(
                text = chatSendState.message.asString(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.error,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
    }
}

private enum class ChatInputAction {
    Sending,
    Send,
    Settings
}

@Composable
private fun ChatInputActionIcon(action: ChatInputAction, secondsRemaining: Int) {
    val colors = MaterialTheme.colorScheme
    if (action == ChatInputAction.Sending) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = colors.primary
        )
        return
    }

    val isSend = action == ChatInputAction.Send
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = if (isSend) Icons.AutoMirrored.Filled.Send else Icons.Rounded.Settings,
            contentDescription = stringResource(
                if (isSend) R.string.chat_send else R.string.chat_settings_title
            ),
            tint = if (isSend && secondsRemaining == 0) colors.primary else colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        if (secondsRemaining > 0) {
            Text(
                text = stringResource(R.string.chat_slow_mode_countdown, secondsRemaining),
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 12.sp),
                color = colors.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ChatReplyPreview(reply: ChatMessage, animateEmotes: Boolean, onClearReply: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val replyLabel = stringResource(R.string.chat_reply_to, reply.sender)
    val prefix = remember(replyLabel, reply.content.isNotBlank(), colors.primary) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = colors.primary, fontWeight = FontWeight.SemiBold)) {
                append(replyLabel)
            }
            if (reply.content.isNotBlank()) append(" · ")
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .padding(top = 6.dp)
            .background(colors.surfaceContainerHigh, RoundedCornerShape(12.dp))
            .padding(start = 10.dp, end = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(16.dp)
        )
        ChatEmotePreview(
            prefix = prefix,
            content = reply.content,
            animateEmotes = animateEmotes,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            emoteSize = 16.sp
        )
        IconButton(onClick = onClearReply, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.chat_cancel_reply),
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private const val MAX_RECENT_EMOTES = 24

private data class EmotePickerSection(
    val key: String,
    val title: String,
    val emotes: List<ChatDisplayEmote>,
    val headerIndex: Int
)

@Composable
private fun ChatEmotePickerPanel(
    emoteSets: List<ChatEmoteSet>,
    recentEmotes: List<ChatEmote>,
    height: Dp,
    searchFocused: Boolean,
    animateEmotes: Boolean,
    onSearchFocusChanged: (Boolean) -> Unit,
    onEmoteSelected: (ChatDisplayEmote) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val sevenTv = LocalSevenTvCatalog.current
    val gridState = rememberLazyGridState()
    val dragging by gridState.interactionSource.collectIsDraggedAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var sevenTvSelected by remember { mutableStateOf(false) }
    val showSevenTv = sevenTv != null && sevenTvSelected
    val searchQuery = query.trim()
    val recentTitle = stringResource(R.string.chat_emoji_recent)
    val sevenTvTitle = stringResource(R.string.chat_seven_tv_tab)
    val sevenTvChannelTitle = stringResource(R.string.chat_seven_tv_channel)
    val sevenTvGlobalTitle = stringResource(R.string.chat_seven_tv_global)
    val availableSets = remember(emoteSets) {
        emoteSets.map { set -> set.copy(emotes = set.emotes.distinctBy { it.id }) }
    }
    val nativeSections = remember(availableSets, recentEmotes, recentTitle, searchQuery) {
        buildList {
            var headerIndex = 1

            fun addSection(key: String, title: String, emotes: List<ChatEmote>) {
                val matches = if (searchQuery.isEmpty()) {
                    emotes
                } else {
                    emotes.filter { it.name.contains(searchQuery, ignoreCase = true) }
                }
                if (matches.isEmpty()) return
                add(EmotePickerSection(key, title, matches.map { ChatDisplayEmote(it.id, it.name) }, headerIndex))
                headerIndex += 1 + matches.size
            }

            addSection("recent", recentTitle, recentEmotes)
            availableSets.forEachIndexed { index, set ->
                addSection("set_${index}_${set.name}", set.name, set.emotes)
            }
        }
    }
    val sections = remember(
        nativeSections, sevenTv, sevenTvChannelTitle, sevenTvGlobalTitle, showSevenTv, searchQuery
    ) {
        buildList {
            if (!showSevenTv || searchQuery.isNotEmpty()) addAll(nativeSections)
            if (sevenTv != null && (showSevenTv || searchQuery.isNotEmpty())) {
                val channel = sevenTv.channel.filter { it.name.contains(searchQuery, ignoreCase = true) }
                val global = sevenTv.global.filter { it.name.contains(searchQuery, ignoreCase = true) }
                if (channel.isNotEmpty()) {
                    add(EmotePickerSection("7tv_channel", sevenTvChannelTitle, channel.map { it.toDisplayEmote() }, 0))
                }
                if (global.isNotEmpty()) {
                    add(EmotePickerSection("7tv_global", sevenTvGlobalTitle, global.map { it.toDisplayEmote() }, 0))
                }
            }
        }
    }
    val statusMessage = when {
        sections.isNotEmpty() -> null
        searchQuery.isNotEmpty() -> R.string.chat_emoji_no_matches
        showSevenTv -> R.string.chat_seven_tv_empty
        availableSets.isEmpty() -> R.string.chat_emoji_loading
        else -> null
    }

    LaunchedEffect(searchQuery) {
        gridState.requestScrollToItem(0)
    }
    LaunchedEffect(dragging) {
        if (dragging && searchFocused) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(colors.surface)
    ) {
        HorizontalDivider(color = colors.onSurface.copy(alpha = 0.08f))
        if ((nativeSections.isNotEmpty() || sevenTv != null) && !searchFocused) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(nativeSections, key = { it.key }, contentType = { "picker_category" }) { section ->
                    EmoteCategoryPill(
                        title = section.title,
                        selected = false,
                        onClick = {
                            if (showSevenTv) {
                                sevenTvSelected = false
                                gridState.requestScrollToItem(section.headerIndex)
                            } else {
                                scope.launch { gridState.animateScrollToItem(section.headerIndex) }
                            }
                        }
                    )
                }
                if (sevenTv != null) {
                    item(key = "7tv", contentType = "picker_category") {
                        EmoteCategoryPill(title = sevenTvTitle, selected = showSevenTv) {
                            if (searchQuery.isNotEmpty()) query = ""
                            sevenTvSelected = !showSevenTv
                            gridState.requestScrollToItem(0)
                        }
                    }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 44.dp),
            state = gridState,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            item(key = "search", span = { GridItemSpan(maxLineSpan) }, contentType = "search") {
                ChatEmoteSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onFocusChanged = onSearchFocusChanged,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                )
            }
            sections.forEach { section ->
                val setTitle = section.key
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "header_$setTitle",
                    contentType = "header"
                ) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp)
                    )
                }
                items(
                    section.emotes,
                    key = { "${setTitle}_${it.key}" },
                    contentType = { "emote" }
                ) { emote ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEmoteSelected(emote) },
                        contentAlignment = Alignment.Center
                    ) {
                        ChatEmoteImage(
                            emote = emote,
                            animated = animateEmotes,
                            modifier = if (emote.sevenTv == null) Modifier.size(30.dp)
                            else Modifier.fillMaxSize().padding(4.dp)
                        )
                    }
                }
            }
            if (statusMessage != null) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "status") {
                    Text(
                        text = stringResource(statusMessage),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmoteCategoryPill(title: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) colors.primaryContainer else colors.surfaceContainerHigh,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant),
        modifier = Modifier.heightIn(min = 32.dp)
    ) {
        Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
            Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun ChatEmoteSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val keyboardController = LocalSoftwareKeyboardController.current
    val label = stringResource(R.string.chat_emoji_search)
    val shape = RoundedCornerShape(12.dp)

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(colors.surfaceContainerHigh, shape)
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .semantics { contentDescription = label },
        textStyle = MaterialTheme.typography.bodySmall.copy(color = colors.onSurface),
        singleLine = true,
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize().padding(start = 10.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.chat_emoji_clear_search),
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    )
}

private fun appendEmote(current: CharSequence, emote: ChatEmote): String {
    val token = "[emote:${emote.id}:${emote.name}]"
    return if (current.isBlank()) token else "$current $token"
}
