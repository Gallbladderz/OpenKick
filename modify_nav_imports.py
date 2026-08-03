import re

with open("app/src/main/java/com/gallbladderz/openkick/navigation/OpenKickNavHost.kt", "r") as f:
    content = f.read()

# Add Settings icon import
imports = """
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Settings
"""
content = re.sub(r'import androidx.compose.material.icons.filled.Home', imports.strip() + '\nimport androidx.compose.material.icons.filled.Home', content)

with open("app/src/main/java/com/gallbladderz/openkick/navigation/OpenKickNavHost.kt", "w") as f:
    f.write(content)
