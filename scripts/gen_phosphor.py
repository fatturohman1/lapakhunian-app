"""Gen Phosphor map v4: compact data-string (hindari Method too large)."""
import re, os

TMP = os.environ.get("TEMP", "/tmp")
duo = open(f"{TMP}/css-duo.css").read()
fill = open(f"{TMP}/css-fill.css").read()
reg = open(f"{TMP}/css-reg.css").read()

d1 = dict(re.findall(r"\.ph-duotone\.ph-([a-z0-9-]+):before\s*{\s*content:\s*\"(\\e[0-9a-f]+)\"", duo))  # icon utama
d2 = dict(re.findall(r"\.ph-duotone\.ph-([a-z0-9-]+):after\s*{\s*content:\s*\"(\\e[0-9a-f]+)\"", duo))   # layer accent
f1 = dict(re.findall(r"\.ph-fill\.ph-([a-z0-9-]+):before\s*{\s*content:\s*\"(\\e[0-9a-f]+)\"", fill))
r1 = dict(re.findall(r"\.ph-([a-z0-9-]+):before\s*{\s*content:\s*\"(\\e[0-9a-f]+)\"", reg))

def data_str(m):
    # entry: name <SEP \u0001> codepoint(\uXXXX escape) <NL \u0002>
    # v = "\eb9a" (css escape) -> strip SATU backslash saja -> "eb9a"
    parts = []
    for k, v in sorted(m.items()):
        cp = int(v[1:], 16)
        parts.append(k + "\\u0001" + "\\u" + format(cp, "04x") + "\\u0002")
    return "".join(parts)

def esc(s):  # escape untuk Kotlin string literal
    return s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$")

kt = f'''package com.lapakhunian.app.core.designsystem

/**
 * Codepoint Phosphor v2.1.1 per-weight (auto-generate — jangan edit manual).
 * Regenerate: scripts/gen_phosphor.py
 * PENTING: codepoint TIDAK sama antar weight.
 */
object PhosphorCodepoints {{
    private const val SEP = "\\u0001"
    private const val NL = "\\u0002"

{data_str(r1) and ""}
    private val REG_DATA = "{data_str(r1)}"
    private val FILL_DATA = "{data_str(f1)}"
    private val DUO_PRIMARY_DATA = "{data_str(d1)}"
    private val DUO_SECONDARY_DATA = "{data_str(d2)}"

    private fun parse(data: String): Map<String, String> =
        data.split(NL).filter {{ it.isNotEmpty() }}.associate {{
            val i = it.indexOf(SEP)
            it.substring(0, i) to it.substring(i + 1)
        }}

    val REGULAR by lazy {{ parse(REG_DATA) }}
    val FILL by lazy {{ parse(FILL_DATA) }}
    val DUO_PRIMARY by lazy {{ parse(DUO_PRIMARY_DATA) }}
    val DUO_SECONDARY by lazy {{ parse(DUO_SECONDARY_DATA) }}
}}
'''
open(r"D:/Libraries/Desktop/All Project/android lapakhunian/app/src/main/java/com/lapakhunian/app/core/designsystem/PhosphorCodepoints.kt", "w", encoding="utf-8").write(kt)
print("reg", len(r1), "fill", len(f1), "duo", len(d1))
