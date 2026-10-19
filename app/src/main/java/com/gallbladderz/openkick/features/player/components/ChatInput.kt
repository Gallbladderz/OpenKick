/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.features.player.components


import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.domain.chat.ChatEmote
import com.gallbladderz.openkick.core.domain.chat.ChatEmoteParser
import com.gallbladderz.openkick.core.domain.chat.ChatEmoteSet
import com.gallbladderz.openkick.core.domain.chat.ChatMessage
import com.gallbladderz.openkick.features.player.ChatSendState

@Composable
fun ChatInput(
    replyToMessage: ChatMessage?,
    onClearReply: () -> Unit,
    chatSendState: ChatSendState,
    isEnabled: Boolean,
    emoteSets: List<ChatEmoteSet> = emptyList(),
    animateEmotes: Boolean = true,
    disabledMessage: String? = null,
    onSendMessage: (String, ChatMessage?) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputValue by remember { mutableStateOf(TextFieldValue()) }
    var emojiMenuExpanded by remember { mutableStateOf(false) }
    var recentEmotes by remember { mutableStateOf<List<ChatEmote>>(emptyList()) }
    var emoteUsage by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var emojiPanelHeight by remember { mutableStateOf(300.dp) }
    var inputWidthPx by remember { mutableStateOf(0) }
    val inputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeHeight = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    var imeWasVisible by remember { mutableStateOf(imeHeight > 0.dp) }
    val isSending = chatSendState is ChatSendState.Sending
    val messageText = inputValue.text
    val canSend = isEnabled && !isSending && messageText.isNotBlank()
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val inputVisual = remember(inputValue.text, emoteSets, animateEmotes) {
        buildEmoteInputVisual(context, inputValue.text, animateEmotes)
    }

    LaunchedEffect(imeHeight) {
        val imeIsVisible = imeHeight > 0.dp
        if (imeHeight > 0.dp) {
            if (imeIsVisible && !imeWasVisible && emojiMenuExpanded) {
                emojiMenuExpanded = false
            }
            emojiPanelHeight = imeHeight
        }
        imeWasVisible = imeIsVisible
    }

    fun send() {
        if (!canSend) return
        onSendMessage(messageText.trim(), replyToMessage)
        inputValue = TextFieldValue()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
    ) {
        HorizontalDivider(color = colors.onSurface.copy(alpha = 0.08f))

        AnimatedVisibility(
            visible = replyToMessage != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            replyToMessage?.let { reply ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, top = 8.dp)
                        .background(
                            colors.surfaceVariant.copy(alpha = 0.55f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(start = 9.dp, end = 3.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .size(width = 3.dp, height = 32.dp)
                            .background(colors.primary, RoundedCornerShape(2.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.chat_reply_to, reply.sender),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = reply.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onClearReply, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.chat_cancel_reply),
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = normalizeEmoteEdit(inputValue, it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(inputFocusRequester)
                        .onSizeChanged { inputWidthPx = it.width }
                        .pointerInput(emojiMenuExpanded, inputWidthPx) {
                            if (!emojiMenuExpanded) return@pointerInput
                            awaitPointerEventScope {
                                val trailingActionsWidth = with(density) { 80.dp.toPx() }
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val down = event.changes.firstOrNull { it.changedToDown() }
                                    if (down != null && down.position.x < inputWidthPx - trailingActionsWidth) {
                                        emojiMenuExpanded = false
                                        break
                                    }
                                }
                            }
                        }
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused && emojiMenuExpanded) {
                                emojiMenuExpanded = false
                            }
                        },
                    enabled = !isSending,
                    readOnly = !isEnabled,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Transparent),
                    visualTransformation = EmoteTokenVisualTransformation,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    placeholder = {
                        Text(
                            text = disabledMessage
                                ?: if (isEnabled) stringResource(R.string.chat_message_hint)
                                else stringResource(R.string.chat_sign_in_hint),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (emojiMenuExpanded) {
                                        emojiMenuExpanded = false
                                        keyboardController?.show()
                                    } else {
                                        emojiMenuExpanded = true
                                        keyboardController?.hide()
                                    }
                                },
                                enabled = isEnabled && !isSending,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Mood,
                                    contentDescription = stringResource(R.string.chat_emoji_menu),
                                    tint = colors.onSurfaceVariant,
                                    modifier = Modifier.size(23.dp)
                                )
                            }
                            IconButton(
                                onClick = { if (canSend) send() else onSettingsClick() },
                                enabled = !isSending,
                                modifier = Modifier.size(40.dp)
                            ) {
                                if (isSending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (canSend) Icons.Default.Send else Icons.Rounded.Settings,
                                        contentDescription = stringResource(
                                            if (canSend) R.string.chat_send else R.string.chat_settings_title
                                        ),
                                        tint = if (canSend) colors.primary else colors.onSurfaceVariant,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceVariant.copy(alpha = 0.55f),
                        unfocusedContainerColor = colors.surfaceVariant.copy(alpha = 0.55f),
                        disabledContainerColor = colors.surfaceVariant.copy(alpha = 0.35f),
                        focusedBorderColor = colors.primary.copy(alpha = 0.75f),
                        unfocusedBorderColor = colors.onSurface.copy(alpha = 0.1f),
                        cursorColor = colors.primary
                    )
                )

                if (inputValue.text.isNotEmpty()) {
                    Text(
                        text = inputVisual.text,
                        inlineContent = inputVisual.inlineContent,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp, end = 88.dp)
                    )
                }
            }
        }

        if (emojiMenuExpanded) {
            ChatEmotePickerPanel(
                emoteSets = emoteSets,
                recentEmotes = recentEmotes,
                height = emojiPanelHeight,
                animateEmotes = animateEmotes,
                onEmoteSelected = { emote ->
                    val updatedText = appendEmote(inputValue.text, emote)
                    inputValue = inputValue.copy(
                        text = updatedText,
                        selection = TextRange(updatedText.length)
                    )
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

private const val MAX_RECENT_EMOTES = 24
private const val EMOTE_GRID_COLUMNS = 8

@Composable
private fun ChatEmotePickerPanel(
    emoteSets: List<ChatEmoteSet>,
    recentEmotes: List<ChatEmote>,
    height: Dp,
    animateEmotes: Boolean,
    onEmoteSelected: (ChatEmote) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val panelHeight = height.coerceIn(240.dp, 420.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .background(colors.surface)
    ) {
        HorizontalDivider(color = colors.onSurface.copy(alpha = 0.08f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            if (recentEmotes.isNotEmpty()) {
                EmoteSection(
                    title = stringResource(R.string.chat_emoji_recent),
                    emotes = recentEmotes,
                    animateEmotes = animateEmotes,
                    onEmoteSelected = onEmoteSelected
                )
            }

            if (emoteSets.isEmpty()) {
                Text(
                    text = stringResource(R.string.chat_emoji_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
                )
            } else {
                emoteSets.forEach { set ->
                    EmoteSection(
                        title = set.name,
                        emotes = set.emotes,
                        animateEmotes = animateEmotes,
                        onEmoteSelected = onEmoteSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun EmoteSection(
    title: String,
    emotes: List<ChatEmote>,
    animateEmotes: Boolean,
    onEmoteSelected: (ChatEmote) -> Unit
) {
    val context = LocalContext.current
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp)
    )
    emotes.chunked(EMOTE_GRID_COLUMNS).forEach { rowEmotes ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            repeat(EMOTE_GRID_COLUMNS) { index ->
                val emote = rowEmotes.getOrNull(index)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clickable(enabled = emote != null) {
                            emote?.let(onEmoteSelected)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    emote?.let {
                        AsyncImage(
                            model = kickEmoteImageRequest(
                                context = context,
                                emoteId = it.id,
                                animated = animateEmotes
                            ),
                            contentDescription = it.name,
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class EmoteInputVisual(
    val text: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>
)

private fun buildEmoteInputVisual(
    context: Context,
    source: String,
    animateEmotes: Boolean
): EmoteInputVisual {
    val inlineContent = mutableMapOf<String, InlineTextContent>()
    val annotated = buildAnnotatedString {
        ChatEmoteParser.parseEmotes(source).forEachIndexed { index, token ->
            when (token) {
                is com.gallbladderz.openkick.core.domain.chat.ChatToken.Text -> append(token.text)
                is com.gallbladderz.openkick.core.domain.chat.ChatToken.Emote -> {
                    val inlineId = "input_emote_${index}_${token.emoteId}"
                    appendInlineContent(inlineId, token.emoteName)
                    inlineContent[inlineId] = InlineTextContent(
                        Placeholder(22.sp, 22.sp, PlaceholderVerticalAlign.TextCenter)
                    ) {
                        AsyncImage(
                            model = kickEmoteImageRequest(
                                context = context,
                                emoteId = token.emoteId,
                                animated = animateEmotes
                            ),
                            contentDescription = token.emoteName,
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
    return EmoteInputVisual(annotated, inlineContent)
}

private object EmoteTokenVisualTransformation : VisualTransformation {
    private val tokenRegex = Regex("\\[emote:(\\d+):([^\\]]+)\\]")

    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        val originalToTransformed = IntArray(source.length + 1)
        val transformedToOriginal = mutableListOf(0)
        val transformed = buildAnnotatedString {
            var originalIndex = 0
            var transformedIndex = 0
            tokenRegex.findAll(source).forEach { match ->
                while (originalIndex < match.range.first) {
                    originalToTransformed[originalIndex] = transformedIndex
                    append(source[originalIndex])
                    originalIndex++
                    transformedIndex++
                    transformedToOriginal += originalIndex
                }
                for (index in match.range) originalToTransformed[index] = transformedIndex
                append("🙂")
                transformedIndex++
                transformedToOriginal += match.range.last + 1
                originalIndex = match.range.last + 1
                originalToTransformed[originalIndex] = transformedIndex
            }
            while (originalIndex < source.length) {
                originalToTransformed[originalIndex] = transformedIndex
                append(source[originalIndex])
                originalIndex++
                transformedIndex++
                transformedToOriginal += originalIndex
            }
            originalToTransformed[source.length] = transformedIndex
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                originalToTransformed[offset.coerceIn(0, source.length)]

            override fun transformedToOriginal(offset: Int): Int =
                transformedToOriginal[offset.coerceIn(0, transformedToOriginal.lastIndex)]
        }
        return TransformedText(transformed, mapping)
    }
}

private fun appendEmote(current: String, emote: ChatEmote): String {
    val token = "[emote:${emote.id}:${emote.name}]"
    return if (current.isBlank()) token else "$current $token"
}

private val completeEmoteToken = Regex("\\[emote:\\d+:[^\\]]+\\]")
private val incompleteEmoteToken = Regex("\\[emote:\\d+:[^\\]]*$")

private fun normalizeEmoteEdit(previous: TextFieldValue, updated: TextFieldValue): TextFieldValue {
    if (updated.text.length >= previous.text.length || !completeEmoteToken.containsMatchIn(previous.text)) {
        return updated
    }

    val incomplete = incompleteEmoteToken.find(updated.text) ?: return updated
    val cleanedText = updated.text.removeRange(incomplete.range.first, updated.text.length)
    return updated.copy(
        text = cleanedText,
        selection = TextRange(cleanedText.length)
    )
}

