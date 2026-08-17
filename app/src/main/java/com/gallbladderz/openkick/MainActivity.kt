/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gallbladderz.openkick.features.player.DraggablePlayerContainer
import com.gallbladderz.openkick.features.player.GlobalPlayerController
import com.gallbladderz.openkick.features.player.LocalGlobalPlayerController
import com.gallbladderz.openkick.features.player.PlayerExpandedState
import com.gallbladderz.openkick.features.player.PlayerRoute
import com.gallbladderz.openkick.features.profile.MainViewModel
import com.gallbladderz.openkick.navigation.CategoryDetailsRoute
import com.gallbladderz.openkick.navigation.OpenKickNavHost
import com.gallbladderz.openkick.navigation.StreamerProfileRoute
import com.gallbladderz.openkick.ui.theme.OpenKickTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

val LocalBottomBarOffset = compositionLocalOf<() -> Float> { { 0f } }

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appTheme by mainViewModel.appTheme.collectAsStateWithLifecycle()
            val appAccent by mainViewModel.appAccent.collectAsStateWithLifecycle()
            val useDynamicColors by mainViewModel.useDynamicColors.collectAsStateWithLifecycle()

            RequestPermissions()

            OpenKickTheme(
                appTheme = appTheme,
                appAccent = appAccent,
                useDynamicColors = useDynamicColors
            ) {
                OpenKickAppContent()
            }
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
fun OpenKickAppContent() {
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
