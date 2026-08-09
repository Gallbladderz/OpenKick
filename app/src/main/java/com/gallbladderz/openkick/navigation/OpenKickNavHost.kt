/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@Composable
fun OpenKickNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MainTabsRoute,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)
            )
        }
    ) {
        mainTabsScreen(navController)
        categoryDetailsScreen(navController)
        clipPlayerScreen(navController)
        searchScreen(navController)
        languageSettingsScreen(navController)
        notificationSettingsScreen(navController)
        allFollowsScreen(navController)
        streamerProfileScreen(navController)
        vodPlayerScreen(navController)
        playerScreen(navController)
        contentSettingsScreen(navController)
        themeSettingsScreen(navController)
        aboutAppScreen(navController)
        licensesScreen(navController)
    }
}
