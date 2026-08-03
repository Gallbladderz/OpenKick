1. **Hiding Filter Chips on Scroll (HomeScreen.kt):**
   - Create a `filterChipsHeightPx` state to track the visible height of `HomeFilterChipsRow`. We'll animate this or just track it via `NestedScrollConnection`. A typical implementation uses `var filterChipsOffsetHeightPx by remember { mutableFloatStateOf(0f) }` with bounds `[ -maxHeight, 0 ]`.
   - Wrap the main list in a `Box` that uses `Modifier.nestedScroll(nestedScrollConnection)` where the custom connection updates `filterChipsOffsetHeightPx`.
   - Note that `PullToRefreshContainer` is already using a `nestedScrollConnection`. We need to chain them or handle them appropriately. A simpler way is to wrap `HomeFilterChipsRow` inside a layout that gets moved up. Actually, we can attach the `nestedScrollConnection` to the `Box` containing the `LazyColumn`. Wait, the user said "Ensure it works seamlessly with the existing `PullToRefreshContainer`."
   - Alternatively, we can use `LazyListState.firstVisibleItemScrollOffset` instead of a nested scroll connection? The prompt says "Implementation tip: You can use `Modifier.nestedScroll` with a custom `NestedScrollConnection` to track the scroll delta and animate the translation Y or height of the chips row."
   - We will implement a custom `NestedScrollConnection` that adjusts an offset for the chips row. Since the prompt states "The Title Row ("OpenKick" + Search) MUST remain strictly pinned at the top and never hide", the layout should be:
     `Column { TitleRow; Box { FilterChips(offset); Box(nestedScroll) { LazyColumn } } }` ... Wait, we can animate the height of `HomeFilterChipsRow` from its original height to 0.

2. **Restore Scroll Position on Back Navigation (HomeScreen.kt):**
   - In `HomeScreen.kt`, change `val listState = remember(selectedFilter) { LazyListState() }` to `val listState = rememberSaveable(selectedFilter, saver = LazyListState.Saver) { LazyListState() }`.
   - The user also said: "Do the exact same thing for the Grid state: use `rememberSaveable(selectedFilter, saver = LazyGridState.Saver) { LazyGridState() }` instead of `rememberLazyGridState()`. Apply this to both Streams and Clips feeds."
   - Wait, `HomeScreen.kt` doesn't have a `LazyVerticalGrid`. Instead it implements a "GridMode" by doing `.chunked(2)` on a `LazyColumn`. Let's re-read the code. Ah! The code chunks the list to emulate a grid! `itemsIndexed(streamRows) { _, rowItems -> Row { ... } }`. There is NO `LazyGridState` in `HomeScreen.kt`. The user's instruction says: "Do the exact same thing for the Grid state: use `rememberSaveable(selectedFilter, saver = LazyGridState.Saver) { LazyGridState() }` instead of `rememberLazyGridState()`. Apply this to both Streams and Clips feeds."
   - Let me search the entire codebase for `rememberLazyGridState()` again. Ah, wait, maybe the user wants me to *add* it to `HomeScreen.kt` even if it wasn't there? No, the user might be referring to `HomeScreen.kt` but perhaps there's another file with grid state? The prompt explicitly mentions `HomeScreen.kt`. Let me double-check `HomeScreen.kt` completely.

3. **"Settings" Tab instead of "Profile" (OpenKickNavHost.kt & Strings):**
   - Update `MainTab` in `OpenKickNavHost.kt`.
   - Update `strings.xml`.
