/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.gallbladderz.openkick.features.player.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.datastore.ChatSettings
import com.gallbladderz.openkick.core.domain.chat.ChatBadge
import com.gallbladderz.openkick.core.domain.chat.ChatBadgeKind
import com.gallbladderz.openkick.core.domain.chat.ChatMessage
import com.gallbladderz.openkick.core.domain.chat.ChatToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsSheet(
    settings: ChatSettings,
    onSettingsChange: (ChatSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val dividerColor = colors.outlineVariant.copy(alpha = 0.4f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetGesturesEnabled = true,
        containerColor = colors.surface,
        contentColor = colors.onSurface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.chat_settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.chat_settings_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.back))
                }
            }

            ChatPreview(
                settings = settings,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ChatSettingsSection(title = stringResource(R.string.chat_readability)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.chat_sample_letter),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Slider(
                            value = settings.textScale,
                            onValueChange = { onSettingsChange(settings.copy(textScale = it)) },
                            valueRange = 0.85f..1.3f,
                            steps = 8,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = colors.primary,
                                activeTrackColor = colors.primary,
                                activeTickColor = colors.onPrimary,
                                inactiveTrackColor = colors.surfaceVariant,
                                inactiveTickColor = colors.outline
                            )
                        )
                        Text(
                            stringResource(R.string.chat_sample_letter),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.chat_text_size_value,
                            (settings.textScale * 100).toInt()
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                    HorizontalDivider(color = dividerColor)
                    ChatSettingSwitch(
                        title = stringResource(R.string.chat_use_message_name_colors),
                        description = stringResource(R.string.chat_use_message_name_colors_desc),
                        checked = settings.useOfficialNameColor,
                        onCheckedChange = { onSettingsChange(settings.copy(useOfficialNameColor = it)) }
                    )
                    HorizontalDivider(color = dividerColor)
                    ChatSettingSwitch(
                        title = stringResource(R.string.chat_animated_emotes),
                        description = stringResource(R.string.chat_animated_emotes_desc),
                        checked = settings.animatedEmotes,
                        onCheckedChange = { onSettingsChange(settings.copy(animatedEmotes = it)) }
                    )
                    HorizontalDivider(color = dividerColor)
                    ChatSettingSwitch(
                        title = stringResource(R.string.chat_seven_tv_emotes),
                        description = stringResource(R.string.chat_seven_tv_emotes_desc),
                        checked = settings.sevenTvEmotes,
                        onCheckedChange = { onSettingsChange(settings.copy(sevenTvEmotes = it)) }
                    )
                }

                ChatSettingsSection(title = stringResource(R.string.chat_badges_section)) {
                    ChatSettingSwitch(
                        title = stringResource(R.string.chat_global_badges),
                        description = stringResource(R.string.chat_global_badges_desc),
                        checked = settings.showGlobalBadges,
                        onCheckedChange = { onSettingsChange(settings.copy(showGlobalBadges = it)) }
                    )
                    HorizontalDivider(color = dividerColor)
                    ChatSettingSwitch(
                        title = stringResource(R.string.chat_channel_badges),
                        description = stringResource(R.string.chat_channel_badges_desc),
                        checked = settings.showChannelBadges,
                        onCheckedChange = { onSettingsChange(settings.copy(showChannelBadges = it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatSettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceVariant.copy(alpha = 0.35f),
        contentColor = colors.onSurface
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun ChatPreview(settings: ChatSettings, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val sender = stringResource(R.string.chat_preview_author)
    val content = stringResource(R.string.chat_preview_message)
    val resourceBase = "android.resource://${LocalContext.current.packageName}"
    val verifiedBadge = stringResource(R.string.chat_preview_badge_verified)
    val subscriberBadge = stringResource(R.string.chat_preview_badge_subscriber)
    val previewMessage = remember(sender, content, resourceBase, verifiedBadge, subscriberBadge) {
        ChatMessage(
            id = "chat-settings-preview",
            sender = sender,
            senderColor = "#53FC18",
            content = content,
            tokens = listOf(ChatToken.Text(content)),
            badges = listOf(
                ChatBadge(
                    name = verifiedBadge,
                    kind = ChatBadgeKind.GLOBAL,
                    imageUrl = "$resourceBase/${R.drawable.ic_chat_preview_verified}"
                ),
                ChatBadge(
                    name = subscriberBadge,
                    kind = ChatBadgeKind.CHANNEL,
                    imageUrl = "$resourceBase/${R.drawable.ic_chat_preview_subscriber}"
                )
            )
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.background,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f))
    ) {
        ChatMessageItem(
            message = previewMessage,
            settings = settings,
            animateEmotes = settings.animatedEmotes,
            canReply = false,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun ChatSettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier
            .weight(1f)
            .padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary,
                uncheckedThumbColor = colors.outline,
                uncheckedTrackColor = colors.surfaceVariant,
                uncheckedBorderColor = colors.outline
            )
        )
    }
}
