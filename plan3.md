Plan:
1. **Hiding Filter Chips on Scroll (HomeScreen.kt):**
   - Import necessary NestedScroll Connection, LocalDensity, offset modifiers.
   - We will animate the `HomeFilterChipsRow` height by modifying its container/wrapper (a Box or modifying its own height) based on a `NestedScrollConnection`.
   - Setup a `var filterChipsHeightOffset by remember { mutableFloatStateOf(0f) }` and `val filterChipsMaxHeightPx = with(LocalDensity.current) { 56.dp.toPx() }` (since it's typically around 40-50dp height, we can use 56.dp).
   - Define a `NestedScrollConnection` that adjusts this offset on scroll.
   - Wrap the main scrolling list area with `Modifier.nestedScroll(nestedScrollConnection)` so that `LazyColumn` scrolling updates the chips' height. Wait, `HomeScreen.kt` has `nestedScroll(pullRefreshState.nestedScrollConnection)`. We can chain them or use `nestedScrollConnection` around the entire `Column` containing the chips and list, but chaining is usually safe: `Modifier.nestedScroll(customConnection).nestedScroll(pullRefreshState.nestedScrollConnection)`. Or applying `nestedScroll` to the main Box containing the list.
   - Actually, an easier layout that preserves `PullToRefresh` is to have the `TitleRow`, then `Column` containing `HomeFilterChipsRow` (with animated negative `offset` or `height`) and the main Box (with weight).

2. **Restore Scroll Position on Back Navigation (HomeScreen.kt):**
   - Update `val listState = remember(selectedFilter) { LazyListState() }` to `val listState = rememberSaveable(selectedFilter, saver = LazyListState.Saver) { LazyListState() }` in `HomeScreen.kt`.
   - Also, the user says "Do the exact same thing for the Grid state: use `rememberSaveable(selectedFilter, saver = LazyGridState.Saver) { LazyGridState() }` instead of `rememberLazyGridState()`. Apply this to both Streams and Clips feeds." Since there is no `LazyGridState` in `HomeScreen.kt` but they emulate a grid using `LazyColumn` and `.chunked(2)`, perhaps the user *wants* us to introduce `LazyGridState` or the instruction implies there is another file, but specifically said "HomeScreen.kt" and "Apply this to both Streams and Clips feeds".
   - Wait, `CategoryDetailsScreen.kt` has `val gridState = rememberLazyGridState()`. I'll update it there just in case, but let's stick to what's written. The user specifically wrote "Restore Scroll Position on Back Navigation (HomeScreen.kt)" and mentioned "Streams and Clips feeds". Since `HomeScreen.kt` doesn't actually use a `LazyGridState`, maybe the user is hallucinating or providing an instruction from a previous iteration of the codebase. To satisfy the prompt, I will replace any instances of `LazyListState` creation in `HomeScreen.kt` with `rememberSaveable`. If there is a `LazyVerticalGrid` anywhere in the scope, I will also update it.

3. **"Settings" Tab instead of "Profile" (OpenKickNavHost.kt & Strings):**
   - Update `MainTab` in `OpenKickNavHost.kt`: rename `PROFILE` to `SETTINGS`, update `R.string.profile` to `R.string.settings_tab`, update icon from `AccountCircle` to `Settings`.
   - Add `settings_tab` to `strings.xml` and `values-ru/strings.xml`.

4. **Testing and Code Review:**
   - Run a Gradle build (`./gradlew assembleDebug`).
   - Run tests (`./gradlew test`).

5. **Pre-commit Steps:**
   - Complete pre-commit steps to ensure proper testing, verification, review, and reflection are done.

6. **Submit:**
   - Commit and submit.
