/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import com.gallbladderz.openkick.core.player.PlayerMedia
import com.gallbladderz.openkick.core.player.PlaybackService
import com.gallbladderz.openkick.core.player.PlayerExpandedState
import com.gallbladderz.openkick.core.player.PlayerManager
import com.gallbladderz.openkick.core.ui.theme.OpenKickTheme
import com.gallbladderz.openkick.features.following.FollowingRepository
import com.gallbladderz.openkick.features.onboarding.WelcomeScreen
import com.gallbladderz.openkick.core.ui.components.KickSignInPrompt
import com.gallbladderz.openkick.features.player.DraggablePlayerContainer
import com.gallbladderz.openkick.features.player.PlayerRoute
import com.gallbladderz.openkick.features.player.RecordedPlayerOverlay
import com.gallbladderz.openkick.features.profile.MainViewModel
import com.gallbladderz.openkick.navigation.CategoryDetailsRoute
import com.gallbladderz.openkick.navigation.ClipPlayerRoute
import com.gallbladderz.openkick.navigation.KickLinkDestination
import com.gallbladderz.openkick.navigation.KickWebLink
import com.gallbladderz.openkick.navigation.OpenKickNavHost
import com.gallbladderz.openkick.navigation.StreamerProfileRoute
import com.gallbladderz.openkick.navigation.VodPlayerRoute
import com.gallbladderz.openkick.navigation.parseKickWebLink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

val LocalBottomBarOffset = compositionLocalOf<() -> Float> { { 0f } }
private const val PendingKickLinkKey = "pendingKickLink"

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
    private var pendingKickLink by mutableStateOf<KickWebLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAuthIntent(intent)
        handlePipAudioIntent(intent)
        if (savedInstanceState == null) {
            handleKickLinkIntent(intent)
        } else {
            pendingKickLink = savedInstanceState.getString(PendingKickLinkKey)?.let(::parseKickWebLink)
        }
        lifecycleScope.launch {
            combine(
                sessionManager.mode,
                sessionManager.sessionGeneration
            ) { mode, generation -> mode to generation }
                .distinctUntilChanged()
                .collectLatest { (mode, generation) ->
                    if (mode == AppMode.AUTHENTICATED) syncAuthenticatedFollowing(generation)
                    else offerMigration.value = false
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
                    { authError.value = it },
                    kickDestination = pendingKickLink?.destination,
                    onKickDestinationOpened = { pendingKickLink = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
        handlePipAudioIntent(intent)
        handleKickLinkIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(PendingKickLinkKey, pendingKickLink?.url)
    }

    private fun handleKickLinkIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        val link = parseKickWebLink(uri.toString()) ?: return
        intent.data = null
        if (link.destination != null) {
            pendingKickLink = link
        } else {
            val viewInBrowser = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
            val chooser = Intent.createChooser(viewInBrowser, null).apply {
                putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(this@MainActivity, MainActivity::class.java)))
            }
            startActivity(chooser)
        }
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
            if (result.isFailure) {
                authError.value =
                    result.exceptionOrNull()?.message ?: getString(R.string.auth_error)
            }
        }
    }

    private suspend fun syncAuthenticatedFollowing(generation: Long) {
        offerMigration.value = false
        try {
            followingRepository.refreshFollowing()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            if (sessionManager.sessionGeneration.value == generation) {
                authError.value = getString(R.string.could_not_load_subscriptions)
            }
        }
        val hasLocalFollows = followingRepository.localStreamers().first().isNotEmpty()
        if (sessionManager.sessionGeneration.value == generation &&
            sessionManager.mode.value == AppMode.AUTHENTICATED
        ) {
            offerMigration.value = hasLocalFollows
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
    showError: (String?) -> Unit,
    kickDestination: KickLinkDestination? = null,
    onKickDestinationOpened: () -> Unit = {}
) {
    val mode by session.mode.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    if (mode == AppMode.UNDECIDED) {
        KickSignInPrompt(
            onContinue = { runCatching { oauth.beginLogin() }.onFailure { showError(it.message) } }
        ) { requestSignIn ->
            WelcomeScreen(
                onSignIn = requestSignIn,
                onAutonomous = { runCatching { session.selectAutonomous() }.onFailure { showError(it.message) } }
            )
        }
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
        LaunchedEffect(kickDestination, navController) {
            if (kickDestination == null) return@LaunchedEffect
            when (kickDestination) {
                is ClipPlayerRoute -> globalPlayerController.openRecording(
                    PlayerMedia.Recording.Clip(kickDestination.clipId)
                )
                is VodPlayerRoute -> globalPlayerController.openRecording(
                    PlayerMedia.Recording.Vod(kickDestination.videoId, kickDestination.channelSlug)
                )
                is CategoryDetailsRoute, is StreamerProfileRoute -> {
                    globalPlayerController.minimizePlayer()
                    navController.navigate(kickDestination)
                }
            }
            onKickDestinationOpened()
        }
    }
    if (showMigration && mode == AppMode.AUTHENTICATED) {
        var migrationFailed by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!migrating) dismissMigration() },
            title = { Text(stringResource(R.string.migrate_follows_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.migrate_follows_description))
                    if (migrationFailed) {
                        Text(stringResource(R.string.migrate_follows_error), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !migrating, onClick = {
                    migrationFailed = false
                    setMigrating(true)
                    scope.launch {
                        val generation = session.sessionGeneration.value
                        try {
                            following.migrateLocalToRemote()
                            if (session.sessionGeneration.value == generation) dismissMigration()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            if (session.sessionGeneration.value == generation) migrationFailed = true
                        } finally {
                            setMigrating(false)
                        }
                    }
                }) { Text(stringResource(R.string.migrate)) }
            },
            dismissButton = {
                TextButton(enabled = !migrating, onClick = dismissMigration) { Text(stringResource(R.string.not_now)) }
            })
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
    val media = globalPlayerController.currentMedia

    if (playerState != PlayerExpandedState.HIDDEN && media != null) {
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
            val openProfile: (String) -> Unit = { slug ->
                globalPlayerController.minimizePlayer()
                navController.navigate(StreamerProfileRoute(slug))
            }
            when (media) {
                is PlayerMedia.Live -> PlayerRoute(
                    streamerName = media.streamerName,
                    dragState = dragState,
                    onBackClick = globalPlayerController::hidePlayer,
                    onAvatarClick = openProfile,
                    onCategoryClick = { slug ->
                        globalPlayerController.minimizePlayer()
                        navController.navigate(CategoryDetailsRoute(slug))
                    }
                )
                is PlayerMedia.Recording -> RecordedPlayerOverlay(
                    recording = media,
                    dragState = dragState,
                    onClose = globalPlayerController::hidePlayer,
                    onProfileClick = openProfile
                )
            }
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
