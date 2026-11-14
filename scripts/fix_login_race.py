"""Fix login-state race: isLoggedIn (sync) -> tokenFlow.first() (async-safe)."""
import os, re

ROOT = r"D:/Libraries/Desktop/All Project/android lapakhunian/app/src/main/java/com/lapakhunian/app"
files = [
    "feature/chat/ChatViewModels.kt",
    "feature/favorites/FavoritesScreen.kt",
    "feature/notifications/NotificationsScreen.kt",
    "feature/dashboard/DashboardViewModels.kt",
]
for rel in files:
    p = os.path.join(ROOT, rel)
    s = open(p, encoding="utf-8").read()
    orig = s
    s = s.replace("if (!session.isLoggedIn) {", "if (session.tokenFlow.first() == null) {")
    if "import kotlinx.coroutines.flow.first" not in s:
        s = re.sub(r"(import kotlinx\.coroutines\.flow\.[^\n]*\n)", r"import kotlinx.coroutines.flow.first\n\1", s, count=1)
    if s != orig:
        open(p, "w", encoding="utf-8", newline="\n").write(s)
        print("fixed:", rel)
    else:
        print("no change:", rel)
