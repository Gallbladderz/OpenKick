/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.features.profile


import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.auth.AccountIdentity
import com.gallbladderz.openkick.core.auth.AppMode
import com.gallbladderz.openkick.core.auth.OAuthManager
import com.gallbladderz.openkick.core.auth.SessionManager
import com.gallbladderz.openkick.core.datastore.AppTheme
import com.gallbladderz.openkick.core.network.api.AccountApi
import com.gallbladderz.openkick.core.ui.components.KickAvatar
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SettingsRoute(
    onProfileClick: (String) -> Unit,
    onLanguageSettingsClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onContentSettingsClick: () -> Unit,
    onThemeSettingsClick: () -> Unit,
    onAboutAppClick: () -> Unit,
    mainViewModel: MainViewModel = koinViewModel(),
    session: SessionManager = koinInject(),
    oauth: OAuthManager = koinInject(),
    accountApi: AccountApi = koinInject()
) {
    val selectedLanguages by mainViewModel.selectedLanguages.collectAsStateWithLifecycle()
    val appTheme by mainViewModel.appTheme.collectAsStateWithLifecycle()
    val useDynamicColors by mainViewModel.useDynamicColors.collectAsStateWithLifecycle()
    val mode by session.mode.collectAsStateWithLifecycle()
    val sessionGeneration by session.sessionGeneration.collectAsStateWithLifecycle()
    val cachedAccount = remember(mode, sessionGeneration) {
        if (mode == AppMode.AUTHENTICATED) session.cachedAccount() else null
    }
    var account by remember(mode, sessionGeneration) { mutableStateOf(cachedAccount) }
    var accountLoading by remember(mode, sessionGeneration) {
        mutableStateOf(mode == AppMode.AUTHENTICATED && cachedAccount == null)
    }
    var accountLoadFailed by remember(mode, sessionGeneration) { mutableStateOf(false) }
    var refreshAccount by remember { mutableIntStateOf(0) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mode, sessionGeneration, refreshAccount) {
        accountLoadFailed = false
        if (mode == AppMode.AUTHENTICATED) {
            session.cachedAccount()?.let {
                account = it
                accountLoading = false
                return@LaunchedEffect
            }
            account = null
            accountLoading = true
            try {
                val loaded = accountApi.currentUser().toIdentity()
                if (session.mode.value == AppMode.AUTHENTICATED &&
                    session.sessionGeneration.value == sessionGeneration
                ) {
                    account = loaded
                    accountLoadFailed = loaded == null
                    loaded?.let { session.cacheAccount(it, sessionGeneration) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                accountLoadFailed = true
            } finally {
                accountLoading = false
            }
        } else {
            accountLoading = false
        }
    }

    SettingsScreen(
        mode = mode,
        account = account,
        accountLoading = accountLoading,
        accountLoadFailed = accountLoadFailed,
        selectedLanguages = selectedLanguages,
        appTheme = appTheme,
        useDynamicColors = useDynamicColors,
        onLanguageSettingsClick = onLanguageSettingsClick,
        onNotificationSettingsClick = onNotificationSettingsClick,
        onContentSettingsClick = onContentSettingsClick,
        onThemeSettingsClick = onThemeSettingsClick,
        onAboutAppClick = onAboutAppClick,
        onProfileClick = onProfileClick,
        onRetryAccount = { refreshAccount++ },
        onSignIn = { runCatching { oauth.beginLogin() }.onFailure { authError = it.message } },
        onSignOut = { runCatching { session.signOut() }.onFailure { authError = it.message } }
    )

    authError?.let { message ->
        AlertDialog(
            onDismissRequest = { authError = null },
            title = { Text(stringResource(R.string.auth_error)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    authError = null
                }) { Text(stringResource(R.string.done_desc)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mode: AppMode,
    account: AccountIdentity?,
    accountLoading: Boolean,
    accountLoadFailed: Boolean,
    selectedLanguages: Set<String>,
    appTheme: AppTheme,
    useDynamicColors: Boolean,
    onLanguageSettingsClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onContentSettingsClick: () -> Unit,
    onThemeSettingsClick: () -> Unit,
    onAboutAppClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    onRetryAccount: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {

    val availableLanguages = mapOf(
        "ru" to stringResource(R.string.russian_lang),
        "en" to stringResource(R.string.english_lang),
        "es" to stringResource(R.string.language_spanish),
        "pt" to stringResource(R.string.language_portuguese),
        "de" to stringResource(R.string.language_german),
        "tr" to stringResource(R.string.language_turkish),
        "fr" to stringResource(R.string.language_french),
        "or" to stringResource(R.string.language_odia)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            SettingsAccountCard(
                mode = mode,
                account = account,
                loading = accountLoading,
                loadFailed = accountLoadFailed,
                onProfileClick = onProfileClick,
                onRetry = onRetryAccount,
                onSignIn = onSignIn,
                onSignOut = onSignOut
            )
        }
        item {
            SettingsGroupHeader(stringResource(R.string.content_group_header))
        }
        item {
            SettingsListItem(
                headline = stringResource(R.string.content_settings_title),
                supporting = stringResource(R.string.content_settings_desc),
                icon = Icons.Default.Visibility,
                onClick = onContentSettingsClick
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            SettingsGroupHeader(stringResource(R.string.appearance))
        }

        item {
            val isDynamicColorsActive =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && useDynamicColors

            val supportingText = if (isDynamicColorsActive) {
                val baseState = when (appTheme) {
                    AppTheme.LIGHT, AppTheme.CATPPUCCIN_LATTE -> stringResource(R.string.light)
                    AppTheme.DARK, AppTheme.CATPPUCCIN_FRAPPE, AppTheme.CATPPUCCIN_MACCHIATO, AppTheme.CATPPUCCIN_MOCHA -> stringResource(
                        R.string.dark
                    )

                    else -> stringResource(R.string.system_default)
                }
                stringResource(R.string.material_you_with_base, baseState)
            } else {
                when (appTheme) {
                    AppTheme.SYSTEM -> stringResource(R.string.system_default)
                    AppTheme.LIGHT -> stringResource(R.string.light)
                    AppTheme.DARK -> stringResource(R.string.dark)
                    AppTheme.CATPPUCCIN_LATTE -> stringResource(R.string.theme_catppuccin_latte)
                    AppTheme.CATPPUCCIN_FRAPPE -> stringResource(R.string.theme_catppuccin_frappe)
                    AppTheme.CATPPUCCIN_MACCHIATO -> stringResource(R.string.theme_catppuccin_macchiato)
                    AppTheme.CATPPUCCIN_MOCHA -> stringResource(R.string.theme_catppuccin_mocha)
                }
            }

            SettingsListItem(
                headline = stringResource(R.string.theme_settings),
                supporting = supportingText,
                icon = Icons.Default.Edit,
                onClick = onThemeSettingsClick
            )
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            SettingsGroupHeader(stringResource(R.string.application_title))
        }

        item {
            SettingsListItem(
                headline = stringResource(R.string.notifications),
                supporting = stringResource(R.string.notifications_settings_desc),
                icon = Icons.Default.Notifications,
                onClick = onNotificationSettingsClick
            )

            SettingsListItem(
                headline = stringResource(R.string.language_and_region),
                supporting = if (selectedLanguages.isEmpty()) {
                    stringResource(R.string.all_languages)
                } else {
                    selectedLanguages
                        .mapNotNull { availableLanguages[it] ?: it }
                        .joinToString(", ")
                },
                icon = Icons.Default.LocationOn,
                onClick = onLanguageSettingsClick
            )
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            SettingsGroupHeader(stringResource(R.string.about_project))
        }

        item {
            SettingsListItem(
                headline = stringResource(R.string.about_app),
                supporting = null,
                icon = Icons.Default.Info,
                onClick = onAboutAppClick
            )
        }
    }
}

@Composable
private fun SettingsAccountCard(
    mode: AppMode,
    account: AccountIdentity?,
    loading: Boolean,
    loadFailed: Boolean,
    onProfileClick: (String) -> Unit,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        if (mode == AppMode.AUTHENTICATED) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = account != null || loadFailed) {
                            if (account != null) onProfileClick(account.profileSlug) else onRetry()
                        },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (account?.avatarUrl?.isNotBlank() == true) {
                        KickAvatar(
                            avatarUrl = account.avatarUrl,
                            streamerName = account.username,
                            size = 48.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account?.username ?: stringResource(R.string.settings_account),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                loading -> stringResource(R.string.settings_account_loading)
                                loadFailed -> stringResource(R.string.settings_account_retry)
                                else -> stringResource(R.string.settings_view_profile)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                TextButton(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSignIn)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Login,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.sign_in),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(
            start = 16.dp,
            top = 16.dp,
            bottom = 8.dp
        )
    )
}

@Composable
fun SettingsListItem(
    headline: String,
    supporting: String?,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(headline) },
        supportingContent = if (supporting != null) {
            { Text(supporting) }
        } else null,
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = headline,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.clickable { onClick() }
    )
}
