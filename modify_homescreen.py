import re

with open("app/src/main/java/com/gallbladderz/openkick/features/home/HomeScreen.kt", "r") as f:
    content = f.read()

# Imports to add
imports = """
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
"""

# Insert imports after package or first import
content = re.sub(r'import androidx.compose.foundation.background', imports.strip() + '\nimport androidx.compose.foundation.background', content)

# 1. Scroll hiding logic
scroll_logic_search = """
    val pullRefreshState = rememberPullToRefreshState()

    if (pullRefreshState.isRefreshing) {
"""

scroll_logic_replace = """
    val pullRefreshState = rememberPullToRefreshState()

    val density = LocalDensity.current
    val filterChipsMaxHeightPx = with(density) { 56.dp.toPx() }
    var filterChipsHeightOffset by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = filterChipsHeightOffset + delta
                filterChipsHeightOffset = newOffset.coerceIn(-filterChipsMaxHeightPx, 0f)
                return Offset.Zero
            }
        }
    }

    if (pullRefreshState.isRefreshing) {
"""

content = content.replace(scroll_logic_search, scroll_logic_replace)

# 2. Wrapping HomeFilterChipsRow
filter_row_search = """
        HomeFilterChipsRow(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it },
            isGridMode = isGridMode,
            onGridModeChange = { onGridModeChange(it) },
            onFilterClick = onFilterClick
        )
"""

filter_row_replace = """
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { (filterChipsMaxHeightPx + filterChipsHeightOffset).toDp() })
                .clipToBounds(),
            contentAlignment = Alignment.BottomCenter
        ) {
            HomeFilterChipsRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                isGridMode = isGridMode,
                onGridModeChange = { onGridModeChange(it) },
                onFilterClick = onFilterClick
            )
        }
"""
content = content.replace(filter_row_search, filter_row_replace)

# 3. Add nestedScrollConnection to the main Box
main_box_search = """
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
"""

main_box_replace = """
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .nestedScroll(nestedScrollConnection)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
"""
content = content.replace(main_box_search, main_box_replace)

# 4. rememberSaveable for LazyListState
saveable_search = "val listState = remember(selectedFilter) { LazyListState() }"
saveable_replace = "val listState = rememberSaveable(selectedFilter, saver = LazyListState.Saver) { LazyListState() }"
content = content.replace(saveable_search, saveable_replace)


with open("app/src/main/java/com/gallbladderz/openkick/features/home/HomeScreen.kt", "w") as f:
    f.write(content)
