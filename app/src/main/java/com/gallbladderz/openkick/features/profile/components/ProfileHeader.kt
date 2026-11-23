/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.profile.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.ui.components.KickAvatar
import com.gallbladderz.openkick.features.player.components.CompactFollowButton
import com.gallbladderz.openkick.features.profile.LastBroadcastTime
import com.gallbladderz.openkick.features.profile.ProfileInfoUi
import com.gallbladderz.openkick.features.profile.lastBroadcastTime
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId

@Composable
fun ProfileHeader(
    info: ProfileInfoUi,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onWatchClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val numberFormat = remember(locale) { NumberFormat.getIntegerInstance(locale) }

    Column(Modifier.fillMaxWidth().background(colors.background)) {
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(128.dp).background(colors.surfaceContainer)) {
                if (info.bannerUrl.isNotBlank()) {
                    AsyncImage(
                        model = info.bannerUrl,
                        contentDescription = stringResource(R.string.banner_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.48f to Color.Transparent,
                            1f to colors.background
                        )
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 100.dp, end = 16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(colors.background)
                        .border(2.dp, if (info.isLive) colors.primary else colors.outlineVariant, CircleShape)
                        .then(if (info.isLive) Modifier.clickable(role = Role.Button, onClick = onWatchClick) else Modifier)
                        .padding(4.dp)
                ) {
                    KickAvatar(info.avatarUrl, info.username, size = 64.dp)
                }
                Spacer(Modifier.weight(1f))
                if (info.isLive) {
                    Button(
                        onClick = onWatchClick,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.profile_watch), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
                CompactFollowButton(isFollowing, onFollowClick)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    info.username,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (info.isVerified) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.verified_desc),
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (info.isLive) {
                ProfileLiveStatus(info, numberFormat, onWatchClick)
            } else {
                ProfileOfflineStatus(info.lastBroadcastAt)
            }

            Text(
                stringResource(R.string.followers_count, numberFormat.format(info.followers)),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileLiveStatus(info: ProfileInfoUi, numberFormat: NumberFormat, onWatchClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val streamDetails = listOfNotNull(info.liveCategory, info.liveTitle).distinct().joinToString(" · ")
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onWatchClick),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.live).uppercase(locale),
                modifier = Modifier.background(colors.error.copy(alpha = 0.14f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium,
                color = colors.error,
                fontWeight = FontWeight.Bold
            )
            info.liveViewers?.let {
                Text(
                    stringResource(R.string.viewers_count, numberFormat.format(it)),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (streamDetails.isNotEmpty()) {
            Text(
                streamDetails,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileOfflineStatus(endedAt: Instant?) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val last = when (val time = lastBroadcastTime(endedAt, Instant.now(), ZoneId.systemDefault())) {
        null -> null
        LastBroadcastTime.JustNow -> stringResource(R.string.profile_last_live_now)
        is LastBroadcastTime.Minutes ->
            pluralStringResource(R.plurals.profile_last_live_minutes, time.amount, time.amount)
        is LastBroadcastTime.Hours ->
            pluralStringResource(R.plurals.profile_last_live_hours, time.amount, time.amount)
        is LastBroadcastTime.Days ->
            pluralStringResource(R.plurals.profile_last_live_days, time.amount, time.amount)
        is LastBroadcastTime.Date -> {
            val pattern = DateFormat.getBestDateTimePattern(locale, if (time.includeYear) "yMMMd" else "MMMd")
            stringResource(R.string.profile_last_live_date, time.format(pattern, locale))
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(6.dp).background(colors.onSurfaceVariant, CircleShape))
        Text(
            stringResource(R.string.profile_offline),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )
        if (last != null) {
            Text(
                "· $last",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}
