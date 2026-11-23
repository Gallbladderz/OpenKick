/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.gallbladderz.openkick.core.domain.chat.ChatDisplayEmote
import com.gallbladderz.openkick.core.domain.chat.ChatDisplayToken
import com.gallbladderz.openkick.core.domain.chat.ChatEmoteParser
import com.gallbladderz.openkick.core.domain.chat.ChatToken
import com.gallbladderz.openkick.core.domain.chat.SevenTvEmote
import com.gallbladderz.openkick.core.domain.chat.tokenizeChatDisplay
import kotlin.math.roundToInt

@Composable
internal fun ChatEmotePreview(
    prefix: AnnotatedString,
    content: String,
    animateEmotes: Boolean,
    style: TextStyle,
    color: Color,
    emoteSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val sevenTv = LocalSevenTvCatalog.current?.byName.orEmpty()
    val (preview, inlineContent) = remember(prefix, content, animateEmotes, emoteSize, sevenTv) {
        val images = mutableMapOf<String, InlineTextContent>()
        val text = buildAnnotatedString {
            append(prefix)
            appendChatEmotes(ChatEmoteParser.parseEmotes(content), sevenTv, images, emoteSize, animateEmotes)
        }
        text to images
    }
    Text(
        text = preview,
        modifier = modifier,
        color = color,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        inlineContent = inlineContent
    )
}

internal fun AnnotatedString.Builder.appendChatEmotes(
    tokens: List<ChatToken>,
    sevenTv: Map<String, SevenTvEmote>,
    inlineContent: MutableMap<String, InlineTextContent>,
    size: TextUnit,
    animateEmotes: Boolean
) {
    tokenizeChatDisplay(tokens, sevenTv).forEachIndexed { index, token ->
        when (token) {
            is ChatDisplayToken.Text -> append(token.text)
            is ChatDisplayToken.Emote -> {
                val key = "emote_$index"
                val width = size * (token.base.sevenTv?.aspectRatio ?: 1f)
                val description = (listOf(token.base) + token.overlays).joinToString(" ") { it.name }
                appendInlineContent(key, description)
                inlineContent[key] = InlineTextContent(
                    Placeholder(width, size, PlaceholderVerticalAlign.TextCenter)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ChatEmoteImage(token.base, animateEmotes, Modifier.fillMaxSize())
                        for (overlay in token.overlays) {
                            ChatEmoteImage(overlay, animateEmotes, Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

private const val DraftEmotePlaceholder = "█"
private val draftEmoteSize = 22.sp

internal data class DraftEmote(
    val sourceRange: IntRange,
    val displayOffset: Int,
    val id: String,
    val name: String
)

internal fun draftEmotes(source: CharSequence): List<DraftEmote> {
    var removed = 0
    return ChatEmoteParser.tokenPattern.findAll(source).map { match ->
        val emote = DraftEmote(
            sourceRange = match.range,
            displayOffset = match.range.first - removed,
            id = match.groupValues[1],
            name = match.groupValues[2]
        )
        removed += match.value.length - DraftEmotePlaceholder.length
        emote
    }.toList()
}

internal val chatEmoteOutput = OutputTransformation {
    val emotes = draftEmotes(asCharSequence())
    for (emote in emotes.asReversed()) {
        replace(emote.sourceRange.first, emote.sourceRange.last + 1, DraftEmotePlaceholder)
    }
    val style = SpanStyle(color = Color.Transparent, fontSize = draftEmoteSize)
    for (emote in emotes) {
        addStyle(style, emote.displayOffset, emote.displayOffset + DraftEmotePlaceholder.length)
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal val chatEmoteInput = InputTransformation {
    if (changes.changeCount == 1) {
        val original = changes.getOriginalRange(0)
        val edited = changes.getRange(0)
        var start = original.min
        var end = original.max
        for (match in ChatEmoteParser.tokenPattern.findAll(originalText)) {
            val range = match.range
            if (range.first >= original.max) break
            if (original.min > range.first && original.min <= range.last) start = range.first
            if (original.max > range.first && original.max <= range.last) end = range.last + 1
        }
        if (start != original.min || end != original.max) {
            val inserted = asCharSequence().subSequence(edited.min, edited.max).toString()
            replace(edited.min - (original.min - start), edited.max + (end - original.max), inserted)
        }
    }
}

@Composable
internal fun ChatDraftEmoteLayer(
    emotes: List<DraftEmote>,
    layout: TextLayoutResult,
    scroll: ScrollState,
    animateEmotes: Boolean
) {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier = Modifier.fillMaxWidth().height(with(density) { layout.size.height.toDp() }),
        contentAlignment = AbsoluteAlignment.TopLeft
    ) {
        for (emote in emotes) {
            val offset = emote.displayOffset
            if (layout.layoutInput.text.text.getOrNull(offset) != DraftEmotePlaceholder.single()) continue
            val bounds = layout.getBoundingBox(offset)
            val line = layout.getLineForOffset(offset)
            val centerY = (layout.getLineTop(line) + layout.getLineBottom(line)) / 2
            val sizePx = minOf(bounds.width, with(density) { draftEmoteSize.toPx() })
            key(offset, emote.id) {
                ChatEmoteImage(
                    emote = ChatDisplayEmote(emote.id, emote.name),
                    animated = animateEmotes,
                    modifier = Modifier
                        .absoluteOffset {
                            val scrollX = if (rtl) scroll.maxValue - scroll.value else scroll.value
                            IntOffset(
                                (bounds.center.x - sizePx / 2).roundToInt() - scrollX,
                                (centerY - sizePx / 2).roundToInt()
                            )
                        }
                        .size(with(density) { sizePx.toDp() })
                )
            }
        }
    }
}

@Composable
internal fun ChatEmoteImage(emote: ChatDisplayEmote, animated: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val request = remember(context, emote, animated) {
        val sevenTv = emote.sevenTv
        if (sevenTv == null) {
            kickEmoteImageRequest(context, emote.id, animated)
        } else {
            ImageRequest.Builder(context)
                .data(sevenTv.imageUrl)
                .crossfade(false)
                .apply {
                    if (sevenTv.animated && !animated) {
                        memoryCacheKey("${sevenTv.imageUrl}:static")
                        decoderFactory(BitmapFactoryDecoder.Factory())
                        allowHardware(false)
                    }
                }
                .build()
        }
    }
    AsyncImage(
        model = request,
        contentDescription = emote.name,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
