/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.ui.components.KickAvatar
import com.gallbladderz.openkick.core.ui.utils.formatViews
import com.gallbladderz.openkick.features.following.FollowingRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
internal fun RecordedFollowButton(slug: String) {
    if (slug.isBlank()) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val following = koinInject<FollowingRepository>()
    val isFollowed by remember(slug, following) { following.isStreamerFollowed(slug) }
        .collectAsStateWithLifecycle(initialValue = false)
    CompactFollowButton(isFollowed) {
        scope.launch {
            try {
                following.toggleStreamerFollow(slug, isFollowed)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Toast.makeText(context, R.string.load_error, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
internal fun RecordedInfoPanel(
    title: String,
    channelName: String,
    avatarUrl: String,
    metadata: List<String>,
    categoryName: String? = null,
    onProfile: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit
) {
    val channel = channelName.ifBlank { stringResource(R.string.anonymous) }
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = title.ifBlank { stringResource(R.string.untitled) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (metadata.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = metadata.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!categoryName.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val profileModifier = if (onProfile == null) Modifier else Modifier.clickable(onClick = onProfile)
                KickAvatar(avatarUrl, channel, size = 40.dp, modifier = profileModifier)
                Text(
                    text = channel,
                    modifier = Modifier.weight(1f).then(profileModifier),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, content = actions)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    }
}

@Composable
internal fun recordedMetadata(views: Int, duration: String, date: String?, creator: String? = null): List<String> {
    val locale = LocalConfiguration.current.locales[0]
    val published = date?.let {
        runCatching {
            LocalDate.parse(it.take(10)).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
        }.getOrNull()
    }
    val creatorLabel = creator?.takeIf { it.isNotBlank() }?.let {
        stringResource(R.string.clip_created_by, it)
    }
    return listOfNotNull(
        stringResource(R.string.recorded_views, formatViews(views)),
        duration.takeIf { it.isNotBlank() },
        published,
        creatorLabel
    )
}

@Composable
internal fun RecordedShareButton(onShare: () -> Unit, description: Int = R.string.share_desc, enabled: Boolean = true) {
    IconButton(onClick = onShare, modifier = Modifier.size(40.dp), enabled = enabled) {
        Icon(Icons.Default.Share, contentDescription = stringResource(description),
            tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun CompactFollowButton(isFollowed: Boolean, onToggleFollow: () -> Unit) {
    if (isFollowed) {
        FilledTonalIconButton(onClick = onToggleFollow, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Favorite, contentDescription = stringResource(R.string.unfollow_action),
                modifier = Modifier.size(20.dp))
        }
    } else {
        Button(
            onClick = onToggleFollow,
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(stringResource(R.string.follow_action), style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
