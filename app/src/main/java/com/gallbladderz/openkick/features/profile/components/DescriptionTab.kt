/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.profile.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.domain.model.ChannelLink
import com.gallbladderz.openkick.features.profile.ProfileSocialLink
import com.gallbladderz.openkick.features.profile.SocialPlatform
import com.gallbladderz.openkick.features.profile.normalizeSocialUrl
import com.gallbladderz.openkick.features.profile.webUri

@Composable
fun DescriptionTab(bio: String, socialLinks: List<ProfileSocialLink>, panels: List<ChannelLink>) {
    val context = LocalContext.current
    val openLink: (String) -> Unit = remember(context) {
        { url ->
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, R.string.profile_link_unavailable, Toast.LENGTH_SHORT).show()
            } catch (_: SecurityException) {
                Toast.makeText(context, R.string.profile_link_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (bio.isNotBlank()) {
            item(key = "bio") { ProfileLinkifiedText(bio, openLink) }
        }
        if (socialLinks.isNotEmpty()) {
            item(key = "socials") { ProfileSocialChips(socialLinks, openLink) }
        }
        if (bio.isBlank() && socialLinks.isEmpty() && panels.isEmpty()) {
            item(key = "empty") {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.streamer_wrote_nothing),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(panels, contentType = { "panel" }) { panel ->
            ChannelPanel(panel, openLink)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileSocialChips(links: List<ProfileSocialLink>, onOpen: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        links.forEach { link ->
            SuggestionChip(
                onClick = { onOpen(link.url) },
                label = {
                    Text(link.platform.brandName.ifEmpty { stringResource(R.string.profile_website) })
                },
                icon = {
                    Icon(
                        painterResource(link.platform.icon()),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }
    }
}

private fun SocialPlatform.icon(): Int = when (this) {
    SocialPlatform.YouTube -> R.drawable.ic_social_youtube
    SocialPlatform.Instagram -> R.drawable.ic_social_instagram
    SocialPlatform.X -> R.drawable.ic_social_x
    SocialPlatform.TikTok -> R.drawable.ic_social_tiktok
    SocialPlatform.Discord -> R.drawable.ic_social_discord
    SocialPlatform.Facebook -> R.drawable.ic_social_facebook
    SocialPlatform.Website -> R.drawable.ic_social_link
}

@Composable
private fun ChannelPanel(panel: ChannelLink, onOpen: (String) -> Unit) {
    val panelUrl = remember(panel.link) { normalizeSocialUrl(SocialPlatform.Website, panel.link) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        if (panel.imageUrl.isNotBlank()) {
            AsyncImage(
                model = panel.imageUrl,
                contentDescription = if (panelUrl == null) null
                else panel.title.ifBlank { stringResource(R.string.profile_website) },
                modifier = Modifier.fillMaxWidth()
                    .then(if (panelUrl == null) Modifier else Modifier.clickable(role = Role.Button) { onOpen(panelUrl) }),
                contentScale = ContentScale.FillWidth
            )
        }
        if (panel.title.isNotBlank() || panel.description.isNotBlank() || panel.link.isNotBlank()) {
            Column(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (panel.title.isNotBlank()) {
                    Text(
                        panel.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (panel.description.isNotBlank()) {
                    ProfileLinkifiedText(panel.description, onOpen)
                }
                if (panel.link.isNotBlank()) {
                    ProfileLinkifiedText(panelUrl ?: panel.link, onOpen)
                }
            }
        }
    }
}

private val BIO_LINK = Regex(
    """(?i)\b(?:https?://[^\s<>]+|www\.[^\s<>]+|[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,})"""
)

@Composable
private fun ProfileLinkifiedText(text: String, onOpen: (String) -> Unit) {
    val linkColor = MaterialTheme.colorScheme.primary
    val content = remember(text, linkColor, onOpen) {
        val style = TextLinkStyles(style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
        buildAnnotatedString {
            var cursor = 0
            BIO_LINK.findAll(text).forEach { match ->
                append(text.substring(cursor, match.range.first))
                val display = match.value.trimEnd('.', ',', '!', '?', ';', ':', ')', ']')
                val url = when {
                    display.startsWith("www.", ignoreCase = true) -> webUri("https://$display")?.toASCIIString()
                    display.contains("://") -> webUri(display)?.toASCIIString()
                    else -> "mailto:$display"
                }
                if (url == null) append(display)
                else withLink(LinkAnnotation.Url(url, style) { onOpen(url) }) { append(display) }
                append(match.value.substring(display.length))
                cursor = match.range.last + 1
            }
            append(text.substring(cursor))
        }
    }
    Text(content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
}
