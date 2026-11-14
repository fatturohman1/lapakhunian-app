# Aturan Khusus Project LapakHunian

## Pertanyaan vs Eksekusi
- Ada pertanyaan (tanda tanya) **tanpa** instruksi implementasi → **jawab saja**. Jangan eksekusi, jangan edit file.
- Ada pertanyaan **dan** instruksi eksekusi → **jawab pertanyaannya dulu**, baru eksekusi.
- Kalau ragu apakah itu pertanyaan atau perintah → tanya konfirmasi, jangan eksekusi.

## Repo & Deploy (rilis APK)
- GitHub repo: `fatturohman1/lapakhunian-app` (public), branch utama `main`, remote `origin`.
- Repo web (privat, terpisah): `D:\Libraries\Desktop\hously` — server lapakhunian.com (Coolify/VPS). Kebijakan bisnis (paywall lead, anti-spam, kompensasi) hidup di sana, bukan di app.
- Halaman unduh: `https://lapakhunian.com/app` — tombolnya menunjuk link permanen
  `https://github.com/fatturohman1/lapakhunian-app/releases/latest/download/lapakhunian-beta.apk`
  yang OTOMATIS menunjuk release terbaru. **Update app tidak perlu deploy web.**
- Halaman /app tidak menuliskan nomor versi → tidak ada teks basi antar rilis.

### Alur rilis (SATU perintah)
```
git tag v<versi> && git push origin v<versi>
```
- Tag `v*` memicu workflow **Release APK** (`.github/workflows/release.yml`):
  build signed → GitHub Release otomatis dibuat dengan asset `lapakhunian-beta.apk`.
- `versionName` diambil dari nama tag (strip `v`); `versionCode` = jumlah tag `v*` (selalu naik).
- Nama file APK release dikunci `lapakhunian-beta.apk` (applicationVariants di build.gradle.kts).
- **Jangan dorong tag/commit baru sebelum run CI selesai** — cek dengan
  `gh run list --limit 3` / `gh run watch <id>`. Kalau gagal: perbaiki, hapus tag remote
  (`git push origin :refs/tags/v<versi>`), tag ulang dari commit yang benar, push.

### Rilis manual lokal (jarang, mis. untuk uji cepat)
```
./gradlew assembleRelease
# hasil: app/build/outputs/apk/release/lapakhunian-beta.apk
```

## Signing & Secrets (JANGAN rusak)
- Keystore: `lapakhunian.keystore` di root project — **gitignored, tidak pernah di-commit**.
- Password & alias dibaca dari `local.properties` (`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`,
  `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`) — juga gitignored.
- **WAJIB backup keystore + password ke password manager** — hilang = tidak bisa update
  aplikasi yang sudah terpasang (user harus uninstall + install baru).
- GitHub Secrets (untuk CI): `ANDROID_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`,
  `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` (dipakai release.yml), plus
  `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` (dipakai job sign di android.yml).
- `GOOGLE_WEB_CLIENT_ID` ada di `gradle.properties` (bukan secret; embedded di APK itu wajar).
  Ini client **Web** OAuth — jangan diganti client Android.

## Jebakan CI yang pernah terjadi (jangan diulang)
- `gradlew` WAJIB executable di git (`git update-index --chmod=+x gradlew`) — commit dari
  Windows tidak otomatis menyimpan bit +x; tanpa itu runner Linux gagal exit 126.
- `org.gradle.java.home` TIDAK boleh ada di `gradle.properties` yang di-commit —
  path Windows invalid di runner Linux; lokal cukup pakai `JAVA_HOME`.
- Error Google Sign-In dibedakan tipenya di LoginScreen (NoCredential / batal /
  konfigurasi SHA-1). Android OAuth client (SHA-1 debug) + Web client sudah terdaftar
  di Google Cloud project `lapakhunian`.
- Google Sign-In per-SHA-1 keystore: APK **debug** (SHA-1 05:5A:18:…) dan APK **release**
  (keystore `lapakhunian.keystore`, SHA-1 F7:FB:6B:60:26:89:7A:CE:ED:20:D9:18:B6:CB:C0:DF:4E:CF:8D:FB)
  KEDUANYA harus terdaftar sebagai Android OAuth client di GCP. Release APK dari GitHub
  yang gagal login Google = SHA-1 release belum terdaftar.

## Catatan fitur penting (konteks singkat)
- Kontak pembeli TANPA login: `POST /api/properties/{id}/guest-contact` (nama + WA) —
  endpoint publik web; lead tetap kena paywall unlock di sisi agen.
- Favorit: respons `/api/favorites` berbentuk nested `{ id, property: {...} }` —
  parse via `FavoriteItemDto`, jangan flat.
- Upload foto: part multipart dibangun MANUAL (`PartData.BinaryItem` + satu Content-Disposition
  lengkap) — `formData{}` Ktor menghasilkan dua header disposition → Next.js gagal parse.
- Share link properti wajib pakai **slug** (`/properti/{slug}`), bukan id.
- Ikon ikon ikon Phosphor: lookup by name di `PhosphorCodepoints.kt`; ikon baru → tambahkan
  konstanta di `AppIcons` (mis. `WhatsApp = "whatsapp-logo"`, `Camera`, `Image`).
