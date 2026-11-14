"""Phase-3 fixes."""
import os

ROOT = r"D:/Libraries/Desktop/All Project/android lapakhunian/app/src/main/java/com/lapakhunian/app"

def w(p, s): open(p, "w", encoding="utf-8", newline="\n").write(s)
def rd(p): return open(p, encoding="utf-8").read()

# 1. semantics shadowing fix
p = f"{ROOT}/core/designsystem/Phosphor.kt"
s = rd(p).replace(
    "modifier.semantics { contentDescription = contentDescription }",
    "modifier.semantics { this.contentDescription = contentDescription }")
w(p, s)

# 2. NotificationsScreen
p = f"{ROOT}/feature/notifications/NotificationsScreen.kt"
s = rd(p).replace("AppIcons.ChatFilled", "AppIcons.Chat").replace(
    "Icon(icon, contentDescription = null, tint = Color(0xFF006D31), modifier = Modifier.size(20.dp))",
    "PhIcon(icon, tint = Color(0xFF006D31), size = 20.dp)")
w(p, s)

# 3. ProfileScreen MenuGroup: ImageVector -> String + PhIcon
p = f"{ROOT}/feature/profile/ProfileScreen.kt"
s = rd(p)
s = s.replace(
    "private fun MenuGroup(\n    items: List<Triple<ImageVector, String, Any?>>,",
    "private fun MenuGroup(\n    items: List<Triple<String, String, Any?>>,")
s = s.replace(
    """                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isLogout) ErrorRed else PrimaryDark,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isLogout) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(13.dp),
                        )
                        .padding(9.dp),
                )""",
    """                PhIcon(
                    icon,
                    tint = if (isLogout) ErrorRed else PrimaryDark,
                    size = 21.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isLogout) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(13.dp),
                        ),
                )""")
w(p, s)

# 4. HomeScreen CategoryTile: ImageVector -> String
p = f"{ROOT}/feature/home/HomeScreen.kt"
s = rd(p)
s = s.replace(
    "private fun CategoryTile(\n    label: String,\n    icon: androidx.compose.ui.graphics.vector.ImageVector,\n    modifier: Modifier = Modifier,",
    "private fun CategoryTile(\n    label: String,\n    icon: String,\n    modifier: Modifier = Modifier,")
s = s.replace(
    """        Icon(
            icon,
            contentDescription = label,
            tint = PrimaryDark,
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp))
                .padding(10.dp),
        )""",
    """        PhIcon(
            icon,
            tint = PrimaryDark,
            size = 24.dp,
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
        )""")
w(p, s)

# 5. DashboardViewModels: load() -> suspend (semua)
p = f"{ROOT}/feature/dashboard/DashboardViewModels.kt"
s = rd(p).replace("    fun load() {", "    suspend fun load() {")
w(p, s)

print("phase3 done")
