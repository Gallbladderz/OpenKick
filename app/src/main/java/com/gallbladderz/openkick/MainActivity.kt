/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gallbladderz.openkick.core.auth.AppMode
import com.gallbladderz.openkick.core.auth.OAuthManager
import com.gallbladderz.openkick.core.auth.SessionManager
import com.gallbladderz.openkick.core.player.GlobalPlayerController
import com.gallbladderz.openkick.core.player.LocalGlobalPlayerController
import com.gallbladderz.openkick.core.player.PlaybackService
import com.gallbladderz.openkick.core.player.PlayerExpandedState
import com.gallbladderz.openkick.core.player.PlayerManager
import com.gallbladderz.openkick.core.ui.theme.OpenKickTheme
import com.gallbladderz.openkick.features.following.FollowingRepository
import com.gallbladderz.openkick.features.onboarding.WelcomeScreen
import com.gallbladderz.openkick.features.player.DraggablePlayerContainer
import com.gallbladderz.openkick.features.player.PlayerRoute
import com.gallbladderz.openkick.features.profile.MainViewModel
import com.gallbladderz.openkick.navigation.CategoryDetailsRoute
import com.gallbladderz.openkick.navigation.OpenKickNavHost
import com.gallbladderz.openkick.navigation.StreamerProfileRoute
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

val LocalBottomBarOffset = compositionLocalOf<() -> Float> { { 0f } }

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModel()
    private val sessionManager: SessionManager by inject()
    private val oauthManager: OAuthManager by inject()
    private val followingRepository: FollowingRepository by inject()
    private val playerManager: PlayerManager by inject()
    private val authError = mutableStateOf<String?>(null)
    private val offerMigration = mutableStateOf(false)
    private val isMigrating = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAuthIntent(intent)
        handlePipAudioIntent(intent)
        lifecycleScope.launch {
            combine(
                sessionManager.mode,
                sessionManager.sessionGeneration
            ) { mode, generation -> mode to generation }
                .distinctUntilChanged()
                .collect { (mode, _) ->
                    if (mode == AppMode.AUTHENTICATED) syncAuthenticatedFollowing()
                }
        }
        enableEdgeToEdge()

        setContent {
            val appTheme by mainViewModel.appTheme.collectAsStateWithLifecycle()
            val appAccent by mainViewModel.appAccent.collectAsStateWithLifecycle()
            val useDynamicColors by mainViewModel.useDynamicColors.collectAsStateWithLifecycle()

            OpenKickTheme(
                appTheme = appTheme,
                appAccent = appAccent,
                useDynamicColors = useDynamicColors
            ) {
                OpenKickAppContent(
                    sessionManager, oauthManager, followingRepository,
                    authError.value, { authError.value = null },
                    offerMigration.value, { offerMigration.value = false },
                    isMigrating.value, { isMigrating.value = it },
                    { authError.value = it }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
        handlePipAudioIntent(intent)
    }

    private fun handlePipAudioIntent(intent: Intent?) {
        if (intent?.action != PlaybackService.ACTION_PIP_AUDIO_ONLY) return
        playerManager.setAudioOnly()
        intent.action = null
        moveTaskToBack(true)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val callback = intent?.data ?: return
        if (callback.toString().substringBefore('?') != BuildConfig.KICK_REDIRECT_URI) return
        intent.data = null
        lifecycleScope.launch {
            val result = oauthManager.handleCallback(callback)
            if (result.isSuccess) {
                syncAuthenticatedFollowing()
            } else {
                authError.value =
                    result.exceptionOrNull()?.message ?: getString(R.string.auth_error)
            }
        }
    }

    private suspend fun syncAuthenticatedFollowing() {
        offerMigration.value = followingRepository.localStreamers().first().isNotEmpty()
        runCatching { followingRepository.refreshFollowing() }
            .onFailure {
                authError.value = it.message ?: getString(R.string.could_not_load_subscriptions)
            }
    }
}

@Composable
fun RequestPermissions() {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun OpenKickAppContent(
    session: SessionManager,
    oauth: OAuthManager,
    following: FollowingRepository,
    error: String?,
    clearError: () -> Unit,
    showMigration: Boolean,
    dismissMigration: () -> Unit,
    migrating: Boolean,
    setMigrating: (Boolean) -> Unit,
    showError: (String?) -> Unit
) {
    val mode by session.mode.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    if (mode == AppMode.UNDECIDED) {
        WelcomeScreen(
            onSignIn = { runCatching { oauth.beginLogin() }.onFailure { showError(it.message) } },
            onAutonomous = { runCatching { session.selectAutonomous() }.onFailure { showError(it.message) } }
        )
    } else {
        RequestPermissions()
        val globalPlayerController = remember { GlobalPlayerController() }
        val navController = rememberNavController()
        val bottomBarState = rememberBottomBarState(navController)

        CompositionLocalProvider(
            LocalGlobalPlayerController provides globalPlayerController,
            LocalBottomBarOffset provides { bottomBarState.bottomBarOffsetPx }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(bottomBarState.nestedScrollConnection)
            ) {
                OpenKickNavHost(navController = navController)
                PlayerOverlay(globalPlayerController, navController, bottomBarState)
            }
        }
    }
    if (showMigration && mode == AppMode.AUTHENTICATED) {
        AlertDialog(
            onDismissRequest = dismissMigration,
            title = { Text(stringResource(R.string.migrate_follows_title)) },
            text = { Text(stringResource(R.string.migrate_follows_description)) },
            confirmButton = {
                TextButton(enabled = !migrating, onClick = {
                    setMigrating(true)
                    scope.launch {
                        runCatching { following.migrateLocalToRemote() }
                            .onSuccess { dismissMigration() }
                            .onFailure { showError(it.message) }
                        setMigrating(false)
                    }
                }) { Text(stringResource(R.string.migrate)) }
            },
            dismissButton = { TextButton(onClick = dismissMigration) { Text(stringResource(R.string.not_now)) } })
    }
    if (error != null) {
        AlertDialog(
            onDismissRequest = clearError,
            title = { Text(stringResource(R.string.auth_error)) },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = clearError) { Text(stringResource(R.string.done_desc)) } })
    }
}

@Composable
fun PlayerOverlay(
    globalPlayerController: GlobalPlayerController,
    navController: NavHostController,
    bottomBarState: BottomBarState
) {
    val playerState = globalPlayerController.playerState
    val streamerName = globalPlayerController.currentStreamerName

    if (playerState != PlayerExpandedState.HIDDEN && streamerName != null) {
        val density = LocalDensity.current
        val bottomOffsetPx = remember(
            density,
            bottomBarState.isBottomBarVisible,
            bottomBarState.navBarBottom,
            bottomBarState.bottomBarOffsetPx
        ) {
            val bottomBarHeight = if (bottomBarState.isBottomBarVisible) {
                with(density) { 70.dp.toPx() }
            } else 0f
            bottomBarState.navBarBottom + bottomBarHeight - bottomBarState.bottomBarOffsetPx
        }

        DraggablePlayerContainer(
            globalPlayerController = globalPlayerController,
            bottomOffsetPx = bottomOffsetPx
        ) { dragState ->
            PlayerRoute(
                streamerName = streamerName,
                dragState = dragState,
                onBackClick = { globalPlayerController.hidePlayer() },
                onAvatarClick = { slug ->
                    globalPlayerController.minimizePlayer()
                    navController.navigate(StreamerProfileRoute(slug))
                },
                onCategoryClick = { slug ->
                    globalPlayerController.minimizePlayer()
                    navController.navigate(CategoryDetailsRoute(slug))
                }
            )
        }
    }
}

class BottomBarState(
    val bottomBarOffsetPx: Float,
    val nestedScrollConnection: NestedScrollConnection,
    val isBottomBarVisible: Boolean,
    val navBarBottom: Float
)

@Composable
fun rememberBottomBarState(navController: NavHostController): BottomBarState {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val navBarBottom = WindowInsets.navigationBars.getBottom(density)

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val maxBottomBarOffsetPx = remember(density, navBarBottom) {
        with(density) { 70.dp.toPx() } + navBarBottom
    }

    var bottomBarOffsetPx by remember { mutableFloatStateOf(0f) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val isBottomBarVisible = navBackStackEntry?.destination?.hierarchy?.any {
        it.route?.contains("MainTabsRoute", ignoreCase = true) == true
    } ?: false

    LaunchedEffect(isLandscape, isBottomBarVisible) {
        if (!isLandscape || !isBottomBarVisible) {
            bottomBarOffsetPx = 0f
        }
    }

    val nestedScrollConnection = remember(isLandscape, isBottomBarVisible) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isLandscape && isBottomBarVisible) {
                    val delta = available.y
                    bottomBarOffsetPx =
                        (bottomBarOffsetPx - delta).coerceIn(0f, maxBottomBarOffsetPx)
                }
                return Offset.Zero
            }
        }
    }

    return BottomBarState(
        bottomBarOffsetPx = bottomBarOffsetPx,
        nestedScrollConnection = nestedScrollConnection,
        isBottomBarVisible = isBottomBarVisible,
        navBarBottom = navBarBottom.toFloat()
    )
}
