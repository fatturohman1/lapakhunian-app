"""Phase-2 fixes: param rename, sisa referensi, VM suspend."""
import os, re

ROOT = r"D:/Libraries/Desktop/All Project/android lapakhunian/app/src/main/java/com/lapakhunian/app"

def w(path, s):
    open(path, "w", encoding="utf-8", newline="\n").write(s)

def rd(path):
    return open(path, encoding="utf-8").read()

# 1. PhIcon param: contentDescriptionOverride -> contentDescription
p = f"{ROOT}/core/designsystem/Phosphor.kt"
s = rd(p).replace("contentDescriptionOverride", "contentDescription")
w(p, s)

# 2. PropertyCards: rename + PhPhIcon
p = f"{ROOT}/core/designsystem/components/PropertyCards.kt"
s = rd(p).replace("contentDescriptionOverride", "contentDescription").replace("PhPhIcon", "PhIcon")
w(p, s)

# 3. SearchBar: SearchFilled -> Search
p = f"{ROOT}/core/designsystem/components/SearchBar.kt"
s = rd(p).replace("AppIcons.SearchFilled", "AppIcons.Search")
w(p, s)

# 4. SpecGrid: import PhIcon
p = f"{ROOT}/core/designsystem/components/SpecGrid.kt"
s = rd(p)
if "designsystem.PhIcon" not in s:
    s = s.replace("import com.lapakhunian.app.core.designsystem.MutedGray",
                  "import com.lapakhunian.app.core.designsystem.MutedGray\nimport com.lapakhunian.app.core.designsystem.PhIcon")
w(p, s)

# 5. DetailScreen: ChatFilled -> Chat
p = f"{ROOT}/feature/detail/DetailScreen.kt"
s = rd(p).replace("AppIcons.ChatFilled", "AppIcons.Chat")
w(p, s)

# 6. HomeScreen: icon() -> String phosphor
p = f"{ROOT}/feature/home/HomeScreen.kt"
s = rd(p)
s = s.replace(
    """private fun PropertyCategory.icon(): androidx.compose.ui.graphics.vector.ImageVector = when (this) {
    PropertyCategory.HOUSE -> AppIcons.CategoryHouse
    PropertyCategory.APARTMENT -> AppIcons.CategoryApartment
    PropertyCategory.OFFICE -> AppIcons.CategoryOffice
    PropertyCategory.LAND -> AppIcons.CategoryLand
    PropertyCategory.COMMERCIAL -> AppIcons.CategoryCommercial
    PropertyCategory.TOWNHOME -> AppIcons.CategoryTownhome
}""",
    """private fun PropertyCategory.icon(): String = when (this) {
    PropertyCategory.HOUSE -> AppIcons.CategoryHouse
    PropertyCategory.APARTMENT -> AppIcons.CategoryApartment
    PropertyCategory.OFFICE -> AppIcons.CategoryOffice
    PropertyCategory.LAND -> AppIcons.CategoryLand
    PropertyCategory.COMMERCIAL -> AppIcons.CategoryCommercial
    PropertyCategory.TOWNHOME -> AppIcons.CategoryTownhome
}""")
w(p, s)

# 7. VM suspend: bungkus load/start
fixes = {
    f"{ROOT}/feature/chat/ChatViewModels.kt": [
        ("    fun start() {", "    suspend fun start() {"),
        ("    init { start() }", "    init { viewModelScope.launch { start() } }"),
    ],
    f"{ROOT}/feature/favorites/FavoritesScreen.kt": [
        ("    fun load() {", "    suspend fun load() {"),
        ("    init { load() }", "    init { viewModelScope.launch { load() } }"),
    ],
    f"{ROOT}/feature/notifications/NotificationsScreen.kt": [
        ("    fun start() {", "    suspend fun start() {"),
        ("    init { start() }", "    init { viewModelScope.launch { start() } }"),
    ],
    f"{ROOT}/feature/dashboard/DashboardViewModels.kt": [
        ("    fun load() {\n        _state.update { it.copy(loading = true, error = null) }\n        viewModelScope.launch {\n            repo.myListings()",
         "    suspend fun load() {\n        _state.update { it.copy(loading = true, error = null) }\n            repo.myListings()"),
    ],
}
for path, subs in fixes.items():
    s = rd(path)
    for old, new in subs:
        s = s.replace(old, new)
    # DashboardViewModels: init { load() } -> launch for semua VM di file
    s = s.replace("    init { load() }", "    init { viewModelScope.launch { load() } }")
    w(path, s)

print("phase2 done")
