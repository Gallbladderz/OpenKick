/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.features.profile.components


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.domain.model.ChannelLink

@Composable
fun DescriptionTab(bio: String, links: List<ChannelLink>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (bio.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    LinkifiedText(
                        text = bio,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        if (links.isEmpty() && bio.isBlank()) {
            item {

                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.streamer_wrote_nothing),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(links) { link ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (link.imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = link.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                        if (link.title.isNotEmpty()) {
                            Text(
                                text = link.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                            )
                        }
                        if (link.description.isNotEmpty()) {
                            LinkifiedText(
                                text = link.description,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        if (link.link.isNotEmpty()) {
                            LinkifiedText(
                                text = link.link,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 16.dp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkifiedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    val annotatedText = remember(text, color, style, linkColor) {
        val urlRegex = Regex("(?i)(https?://[^\\s<>]+|www\\.[^\\s<>]+)")
        buildAnnotatedString {
            var cursor = 0
            urlRegex.findAll(text).forEach { match ->
                append(text.substring(cursor, match.range.first))
                val rawUrl = match.value.trimEnd('.', ',', '!', '?', ';', ':', ')', ']')
                val start = length
                withStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(rawUrl)
                }
                addStringAnnotation(
                    tag = "URL",
                    annotation = if (rawUrl.startsWith(
                            "www.",
                            ignoreCase = true
                        )
                    ) "https://$rawUrl" else rawUrl,
                    start = start,
                    end = length
                )
                if (rawUrl.length < match.value.length) append(match.value.drop(rawUrl.length))
                cursor = match.range.last + 1
            }
            append(text.substring(cursor))
        }
    }

    ClickableText(
        text = annotatedText,
        modifier = modifier,
        style = style.copy(color = color),
        onClick = { offset ->
            annotatedText
                .getStringAnnotations("URL", offset, offset)
                .firstOrNull()
                ?.let { uriHandler.openUri(it.item) }
        }
    )
}

