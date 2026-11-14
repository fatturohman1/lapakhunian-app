"""Batch: Icon(AppIcons.X) -> PhIcon(AppIcons.X) + tambah import PhIcon."""
import os, re

ROOT = r"D:/Libraries/Desktop/All Project/android lapakhunian/app/src/main/java/com/lapakhunian/app"
IMPORT = "import com.lapakhunian.app.core.designsystem.PhIcon\n"

targets = []
for dirpath, _, files in os.walk(ROOT):
    for f in files:
        if f.endswith(".kt"):
            targets.append(os.path.join(dirpath, f))

changed = 0
for path in targets:
    s = open(path, encoding="utf-8").read()
    orig = s
    s = re.sub(r"Icon\(\s*AppIcons\.", "PhIcon(AppIcons.", s)
    if s != orig and "import com.lapakhunian.app.core.designsystem.PhIcon" not in s:
        # sisipkan setelah baris import designsystem terakhir, atau sebelum baris kode pertama
        m = list(re.finditer(r"import com\.lapakhunian\.app\.core\.designsystem[^\n]*\n", s))
        if m:
            last = m[-1]
            s = s[:last.end()] + IMPORT + s[last.end():]
        else:
            m2 = re.search(r"(import [^\n]*\n)(?!import)", s)
            s = re.sub(r"(import com\.lapakhunian\.app\n)", r"\1" + IMPORT, s, count=1) or s
    if s != orig:
        open(path, "w", encoding="utf-8", newline="\n").write(s)
        changed += 1
        print("changed:", os.path.relpath(path, ROOT))
print("total", changed)
