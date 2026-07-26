package com.gallbladderz.openkick.features.player.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gallbladderz.openkick.features.player.models.ChatMessage
import com.gallbladderz.openkick.features.player.models.ChatToken

@Composable
fun ChatList(chatMessages: List<ChatMessage>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        reverseLayout = true,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(chatMessages, key = { it.id }) { message ->
            ChatMessageItem(message)
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val defaultColor = MaterialTheme.colorScheme.primary
    val nameColor = remember(message.senderColor, defaultColor) {
        val colorString = message.senderColor
        if (colorString.isNotBlank()) {
            try {
                Color(android.graphics.Color.parseColor(colorString))
            } catch (e: Exception) {
                defaultColor
            }
        } else {
            defaultColor
        }
    }

    val inlineContentMap = mutableMapOf<String, InlineTextContent>()

    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(color = nameColor, fontWeight = FontWeight.Bold)) {
            append("${message.sender}: ")
        }

        for (token in message.tokens) {
            when (token) {
                is ChatToken.Text -> {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                        append(token.text)
                    }
                }

                is ChatToken.Emote -> {
                    val inlineId = "emote_${token.emoteId}"
                    appendInlineContent(inlineId, "[${token.emoteName}]")

                    if (!inlineContentMap.containsKey(inlineId)) {
                        inlineContentMap[inlineId] = InlineTextContent(
                            Placeholder(
                                width = 24.sp,
                                height = 24.sp,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                            )
                        ) {
                            AsyncImage(
                                model = "https://files.cdn.kick.com/emotes/${token.emoteId}/fullsize?width=96&format=webp",
                                contentDescription = token.emoteName,
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
        modifier = Modifier.padding(vertical = 4.dp),
        lineHeight = 24.sp
    )
}
