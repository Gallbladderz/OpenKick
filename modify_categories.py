import re

with open("app/src/main/java/com/gallbladderz/openkick/features/categories/CategoryDetailsScreen.kt", "r") as f:
    content = f.read()

# Make sure imports are present
if "rememberSaveable" not in content:
    imports = """
import androidx.compose.runtime.saveable.rememberSaveable
"""
    content = re.sub(r'import androidx.compose.runtime.Composable', imports.strip() + '\nimport androidx.compose.runtime.Composable', content)

# 2. rememberSaveable for LazyGridState
saveable_search = "val gridState = rememberLazyGridState()"
saveable_replace = "val gridState = rememberSaveable(saver = androidx.compose.foundation.lazy.grid.LazyGridState.Saver) { androidx.compose.foundation.lazy.grid.LazyGridState() }"
content = content.replace(saveable_search, saveable_replace)

with open("app/src/main/java/com/gallbladderz/openkick/features/categories/CategoryDetailsScreen.kt", "w") as f:
    f.write(content)
