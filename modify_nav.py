import re

with open("app/src/main/java/com/gallbladderz/openkick/navigation/OpenKickNavHost.kt", "r") as f:
    content = f.read()

# Update enum definition
enum_search = "PROFILE(R.string.profile, Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)"
enum_replace = "SETTINGS(R.string.settings_tab, Icons.Outlined.Settings, Icons.Filled.Settings)"
content = content.replace(enum_search, enum_replace)

# Update route parsing
route_search = "MainTab.PROFILE -> {"
route_replace = "MainTab.SETTINGS -> {"
content = content.replace(route_search, route_replace)

with open("app/src/main/java/com/gallbladderz/openkick/navigation/OpenKickNavHost.kt", "w") as f:
    f.write(content)

# Update English strings
with open("app/src/main/res/values/strings.xml", "r") as f:
    strings_en = f.read()
if '<string name="settings_tab">Settings</string>' not in strings_en:
    strings_en = strings_en.replace("</resources>", '    <string name="settings_tab">Settings</string>\n</resources>')
with open("app/src/main/res/values/strings.xml", "w") as f:
    f.write(strings_en)

# Update Russian strings
with open("app/src/main/res/values-ru/strings.xml", "r") as f:
    strings_ru = f.read()
if '<string name="settings_tab">Настройки</string>' not in strings_ru:
    strings_ru = strings_ru.replace("</resources>", '    <string name="settings_tab">Настройки</string>\n</resources>')
with open("app/src/main/res/values-ru/strings.xml", "w") as f:
    f.write(strings_ru)
