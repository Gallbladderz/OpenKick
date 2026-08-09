/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.features.player.models.ChatMessage
import com.gallbladderz.openkick.features.player.models.ChatToken
import kotlinx.coroutines.launch

@Composable
fun ChatList(chatMessages: List<ChatMessage>) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var isAutoScrolling by remember { mutableStateOf(true) }

    val interaction by listState.interactionSource.interactions.collectAsState(initial = null)
    LaunchedEffect(interaction) {
        if (interaction is DragInteraction.Start) {
            isAutoScrolling = false
        }
    }

    val isAtBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    LaunchedEffect(isAtBottom) {
        if (isAtBottom) {
            isAutoScrolling = true
        }
    }

    LaunchedEffect(chatMessages.size, chatMessages.firstOrNull()?.id) {
        if (isAutoScrolling && chatMessages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(chatMessages, key = { it.id }) { message ->
                ChatMessageItem(message)
            }
        }

        AnimatedVisibility(
            visible = !isAutoScrolling,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(
                onClick = {
                    isAutoScrolling = true
                    coroutineScope.launch {
                        listState.scrollToItem(0)
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Scroll to bottom",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val defaultColor = MaterialTheme.colorScheme.primary
    val nameColor = remember(message.senderColor, defaultColor) {
        if (message.senderColor.isNotBlank()) {
            try {
                Color(android.graphics.Color.parseColor(message.senderColor))
            } catch (e: Exception) {
                defaultColor
            }
        } else {
            defaultColor
        }
    }

    val outlineShadow = Shadow(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        offset = Offset(1f, 1f),
        blurRadius = 3f
    )

    val fontSize = 14.sp
    val emoteSize = 22.sp
    val inlineContentMap = mutableMapOf<String, InlineTextContent>()

    val annotatedString = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                color = nameColor,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                shadow = outlineShadow
            )
        ) {
            append(message.sender)
        }
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        ) {
            append(": ")
        }
        for (token in message.tokens) {
            when (token) {
                is ChatToken.Text -> {
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = fontSize
                        )
                    ) {
                        append(token.text)
                    }
                }

                is ChatToken.Emote -> {
                    val inlineId = "emote_${token.emoteId}"
                    appendInlineContent(inlineId, "[${token.emoteName}]")
                    if (!inlineContentMap.containsKey(inlineId)) {
                        inlineContentMap[inlineId] = InlineTextContent(
                            Placeholder(
                                width = emoteSize,
                                height = emoteSize,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                            )
                        ) {
                            AsyncImage(
                                model = "https://files.cdn.kick.com/emotes/${token.emoteId}/fullsize?width=96&format=webp",
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
        text = annotatedString,
        inlineContent = inlineContentMap,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                clipboardManager.setText(AnnotatedString(message.content))
                Toast.makeText(
                    context,
                    context.getString(R.string.copied_to_clipboard),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .padding(horizontal = 12.dp, vertical = 3.dp),
        lineHeight = 20.sp
    )
}