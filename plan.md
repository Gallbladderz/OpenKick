1. **Refactor SearchScreen**
    * In `SearchScreen.kt`: Create `SearchRoute` composable calling `koinViewModel()`. Pass `state` and event lambdas (`onSearch`) to `SearchScreen`. Remove `koinViewModel()` from `SearchScreen`.
    * In `OpenKickNavHost.kt`: Update `composable<SearchRoute>` to use `SearchRoute` wrapper.
2. **Verify SearchScreen**
    * Use `read_file` on `SearchScreen.kt` and `OpenKickNavHost.kt` to confirm changes.

3. **Refactor NotificationSettingsScreen**
    * In `NotificationSettingsScreen.kt`: Create `NotificationSettingsRoute`. Pass `notificationsEnabled`, `backgroundKeepalive`, and lambdas `onToggleNotifications`, `onToggleBackgroundKeepalive` to `NotificationSettingsScreen`.
    * In `OpenKickNavHost.kt`: Update route.
4. **Verify NotificationSettingsScreen**
    * Use `read_file` on `NotificationSettingsScreen.kt` and `OpenKickNavHost.kt`.

5. **Refactor LanguageSettingsScreen**
    * In `LanguageSettingsScreen.kt`: Create `LanguageSettingsRoute`. Pass `selectedLanguages` and `onToggleLanguage` to `LanguageSettingsScreen`.
    * In `OpenKickNavHost.kt`: Update route.
6. **Verify LanguageSettingsScreen**
    * Use `read_file` on `LanguageSettingsScreen.kt` and `OpenKickNavHost.kt`.

7. **Refactor ContentSettingsScreen**
    * In `ContentSettingsScreen.kt`: Create `ContentSettingsRoute`. Pass `hideCategories` and `onToggleCategories`.
    * In `OpenKickNavHost.kt`: Update route.
8. **Verify ContentSettingsScreen**
    * Use `read_file` on `ContentSettingsScreen.kt` and `OpenKickNavHost.kt`.

9. **Refactor ThemeSettingsScreen**
    * In `ThemeSettingsScreen.kt`: Create `ThemeSettingsRoute`. Pass `appTheme`, `useDynamicColors`, `onUpdateTheme`, `onUpdateDynamicColors`.
    * In `OpenKickNavHost.kt`: Update route.
10. **Verify ThemeSettingsScreen**
    * Use `read_file` on `ThemeSettingsScreen.kt` and `OpenKickNavHost.kt`.

11. **Refactor SettingsScreen**
    * In `SettingsScreen.kt`: Create `SettingsRoute`. Pass `selectedLanguages`, `appTheme`, `useDynamicColors`.
    * In `OpenKickNavHost.kt`: Update route.
12. **Verify SettingsScreen**
    * Use `read_file` on `SettingsScreen.kt` and `OpenKickNavHost.kt`.

13. **Refactor StreamerProfileScreen**
    * In `StreamerProfileScreen.kt`: Create `StreamerProfileRoute`. Pass `state`, `isRefreshing`, `loadingVideoId`, and lambdas (`onLoadProfile`, `onRefresh`, `onToggleFollow`, `onLoadVideoPlaybackUrl`).
    * In `OpenKickNavHost.kt`: Update route.
14. **Verify StreamerProfileScreen**
    * Use `read_file` on `StreamerProfileScreen.kt` and `OpenKickNavHost.kt`.

15. **Refactor PlayerScreen**
    * In `PlayerScreen.kt`: Create `PlayerRoute`. Pass `state`, `chatMessages`, `channelLinks`, `isFollowed`, `playWhenReady`, `playbackState`, `availableQualities`, `selectedQuality`, `playerManagerPlayer` (from `viewModel.playerManager.player`) and lambdas (`onPause`, `onPlay`, `onPlayerManagerRelease`, `onLoadStreamInfo`, `onLoadChannelLinks`, `onPlayerManagerPause`, `onPlayerManagerResume`, `onToggleFollow`, `onSetVideoQuality`).
    * In `OpenKickNavHost.kt`: Update route.
16. **Verify PlayerScreen**
    * Use `read_file` on `PlayerScreen.kt` and `OpenKickNavHost.kt`.

17. **Refactor ClipPlayerScreen**
    * In `ClipPlayerScreen.kt`: Create `ClipPlayerRoute`. Pass `isFollowed`, `fetchedAvatarUrl` and lambdas (`onToggleFollow`, `onLoadAvatar`).
    * In `OpenKickNavHost.kt`: Update route.
18. **Verify ClipPlayerScreen**
    * Use `read_file` on `ClipPlayerScreen.kt` and `OpenKickNavHost.kt`.

19. **Refactor HomeScreen**
    * In `HomeScreen.kt`: Create `HomeRoute`. Pass `state`, `isRefreshing` and lambdas (`onRefresh`, `onLoadMoreStreams`, `onFetchHomeData`).
    * In `OpenKickNavHost.kt`: Update route.
20. **Verify HomeScreen**
    * Use `read_file` on `HomeScreen.kt` and `OpenKickNavHost.kt`.

21. **Refactor FollowingScreen**
    * In `FollowingScreen.kt`: Create `FollowingRoute`. Pass `state`, `isRefreshing` and lambdas (`onRefresh`).
    * In `OpenKickNavHost.kt`: Update route.
22. **Verify FollowingScreen**
    * Use `read_file` on `FollowingScreen.kt` and `OpenKickNavHost.kt`.

23. **Refactor AllFollowsScreen**
    * In `AllFollowsScreen.kt`: Create `AllFollowsRoute`. Pass `state` and lambdas (`onUnfollowStreamer`).
    * In `OpenKickNavHost.kt`: Update route.
24. **Verify AllFollowsScreen**
    * Use `read_file` on `AllFollowsScreen.kt` and `OpenKickNavHost.kt`.

25. **Refactor CategoriesScreen**
    * In `CategoriesScreen.kt`: Create `CategoriesRoute`. In `CategoryCard`, remove `viewModel`, pass `isFollowed` state and `onToggleCategoryFollow` lambda. Pass `state` and lambdas (`onLoadMoreCategories`, `onToggleCategoryFollow`).
    * In `OpenKickNavHost.kt`: Update route.
26. **Verify CategoriesScreen**
    * Use `read_file` on `CategoriesScreen.kt` and `OpenKickNavHost.kt`.

27. **Refactor CategoryDetailsScreen**
    * In `CategoryDetailsScreen.kt`: Create `CategoryDetailsRoute`. Pass `state` and lambdas (`onLoadCategory`).
    * In `OpenKickNavHost.kt`: Update route.
28. **Verify CategoryDetailsScreen**
    * Use `read_file` on `CategoryDetailsScreen.kt` and `OpenKickNavHost.kt`.

29. **Run Tests**
    * Run `./gradlew test assembleDebug` to ensure no regressions.

30. **Pre-commit Steps**
    * Complete pre-commit steps to ensure proper testing, verification, review, and reflection are done.
