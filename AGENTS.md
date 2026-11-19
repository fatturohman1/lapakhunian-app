# Aturan Khusus Project LapakHunian

## Pertanyaan vs Eksekusi
- Ada pertanyaan (tanda tanya) **tanpa** instruksi implementasi → **jawab saja**. Jangan eksekusi, jangan edit file.
- Ada pertanyaan **dan** instruksi eksekusi → **jawab pertanyaannya dulu**, baru eksekusi.
- Kalau ragu apakah itu pertanyaan atau perintah → tanya konfirmasi, jangan eksekusi.

## Repo & Deploy (rilis APK)
- GitHub repo: `fatturohman1/lapakhunian-app` (public), branch utama `main`, remote `origin`.
- Repo web (privat, terpisah): `D:\Libraries\Desktop\hously` — server lapakhunian.com (Coolify/VPS). Kebijakan bisnis (paywall lead, anti-spam, kompensasi) hidup di sana, bukan di app.
- Halaman unduh: `https://lapakhunian.com/app` — tombolnya menuju `/app/download`,
  route proxy web yang menyajikan APK release terbaru dari GitHub.
  Link GitHub permanen: `https://github.com/fatturohman1/lapakhunian-app/releases/latest/download/lapakhunian-beta.apk`.
  **Update app tidak perlu deploy web.**
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

### Aturan push (WAJIB — hindari bentrok di GitHub)
- Commit banyak & push **jadi satu boleh** — satu `git push` berisi beberapa commit tidak masalah.
- **Beberapa push yang harus masuk JANGAN barengan**: satu push → tunggu selesai
  (remote ter-update) → baru push berikutnya. Contoh: repo app + repo server (hously)
  keduanya punya perubahan → push server dulu, selesai, baru push app.
- Sama untuk tag rilis: `git push origin main` selesai dulu, baru `git push origin v<versi>`.

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
- Firebase (`lapakhunian-e4654`): `app/google-services.json` **boleh di-commit**
  (bukan secret; CI release butuh). Service account json (`*firebase-adminsdk*.json`)
  = **SECRET, gitignored, tidak pernah di-commit** — nilainya hidup di env Coolify
  repo hously (`FIREBASE_PROJECT_ID`, `FIREBASE_CLIENT_EMAIL`, `FIREBASE_PRIVATE_KEY`);
  env kosong → server skip push diam-diam (app tetap jalan).

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
  KEDUANYA harus terdaftar sebagai Android OAuth client di GCP. SHA-1 yang belum terdaftar
  adalah salah satu kemungkinan penyebab gagal login, bukan diagnosis otomatis.
- Pada 2026-11-16 pengguna mengonfirmasi login APK GitHub sudah normal, termasuk konfigurasi
  Firebase/Google dan SHA. Ini konfirmasi pengguna, bukan tes login langsung oleh asisten.
- **Jangan ubah konfigurasi Firebase atau Google Auth yang sudah normal tanpa permintaan eksplisit.**
  Temuan audit login sebelumnya menyangkut UX alur akun, bukan konfigurasi autentikasi;
  jangan membuka kembali diagnosis SHA lama tanpa bukti kegagalan baru.

## Catatan fitur penting (konteks singkat)
- Notifikasi: FCM (`FcmService`, data payload `{title, body}`) + fallback polling
  WorkManager 15 menit (`ChatNotificationWorker`). Token per device via
  `POST /api/notifications/device-token` (logout → DELETE). Snapshot unread di
  `NotificationPrefs` disync ChatListViewModel + worker supaya tak ada notif ganda.
- Kontak pembeli TANPA login: `POST /api/properties/{id}/guest-contact` (nama + WA) —
  endpoint publik web; lead tetap kena paywall unlock di sisi agen.
- Favorit: respons `/api/favorites` berbentuk nested `{ id, property: {...} }` —
  parse via `FavoriteItemDto`, jangan flat.
- Upload foto: part multipart dibangun MANUAL (`PartData.BinaryItem` + satu Content-Disposition
  lengkap) — `formData{}` Ktor menghasilkan dua header disposition → Next.js gagal parse.
- Share link properti wajib pakai **slug** (`/properti/{slug}`), bukan id.
- Ikon ikon ikon Phosphor: lookup by name di `PhosphorCodepoints.kt`; ikon baru → tambahkan
  konstanta di `AppIcons` (mis. `WhatsApp = "whatsapp-logo"`, `Camera`, `Image`).

## Konteks lintas asisten (2026-11-17)
- Ringkasan ini disimpan di proyek agar dapat dibaca Qoder, zcode, atau asisten lain tanpa
  mengandalkan folder memory internal Qoder. Jika tidak dimuat otomatis, minta asisten membaca `AGENTS.md`.
- Commit, push, dan tag rilis hanya atas perintah eksplisit pengguna untuk pekerjaan terkait;
  izin rilis sebelumnya bukan izin untuk perubahan berikutnya. Verifikasi remote sebelum push.
- Status berikut adalah snapshot bertanggal; cek Git dan `CHANGELOG.md` sebelum pekerjaan baru.

### Riwayat rilis dan verifikasi
- `v1.0.0-beta.6` — commit `974d6ee`, rilis 2026-11-16: audit Android P1/P2/P3,
  cek update, riwayat terakhir dilihat, favorit tamu + merge, pembanding, dan ringkasan update.
- `v1.0.0-beta.7` — commit `e47696f`, rilis 2026-11-17: perbaikan parse kontak,
  notifikasi heads-up, pengaturan izin notifikasi, dan Properti terdekat.
- Pada verifikasi rilis beta.7, Android CI dan Release APK sukses; `/app/download` merespons
  HTTP 200 dengan Content-Length 3.829.572 byte, sama dengan asset GitHub `lapakhunian-beta.apk`.
- Hasil pengujian batch beta.7: 68 unit test + 30 instrumented test lulus; tampilan Terdekat,
  pengaturan Notifikasi, serta channel importance 4 diverifikasi di emulator. Ini hasil saat rilis,
  bukan jaminan hasil pengujian setelah perubahan berikutnya.
- Baseline lint saat audit: 6 error lama di luar scope — `LocalNotifier` MissingPermission ×2
  dan NewApi ×3, serta pembacaan `StateFlow.value` saat composition di rute kontak `AppRoot`.
  Jangan memperluas scope untuk memperbaikinya tanpa diminta; tetap bedakan regresi baru dari baseline.

### Invarian batch beta.7
- `ContactResponseDto.deducted` adalah `Boolean?`, bukan Int; tes regresi memakai payload server
  dengan `deducted: true` agar kontak yang sebenarnya sukses tidak dilaporkan sebagai error parse.
- Channel `chat_heads_up` memakai IMPORTANCE_HIGH; channel lama `chat` dihapus karena importance
  channel tidak bisa dinaikkan in-place. Metadata manifest memilih channel ini untuk FCM background;
  notifikasi lokal dibedakan per `conversationId`. Jangan mengganti konfigurasi proyek Firebase.
- Terdekat meminta ACCESS_COARSE_LOCATION hanya saat dipakai. `DeviceLocator` membaca last-known,
  lalu single update dengan timeout 8 detik bila perlu; lokasi hanya di perangkat, tidak ke server.
  Urutan jarak menggunakan maksimal 100 kandidat API; listing tanpa koordinat ditempatkan di akhir.

### Pengujian Android lokal yang aman
- AVD lokal: `LapakHunian` (layar kecil 320dp). Jangan wipe data atau uninstall aplikasi/emulator.
  Jika emulator berhenti, jalankan `"C:/Users/Resa/AppData/Local/Android/Sdk/emulator/emulator.exe" -avd LapakHunian -no-snapshot-save`.
- Pakai `JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"` saat menjalankan Gradle lokal;
  jangan menambahkan path Java Windows ke konfigurasi build yang di-commit.
- ADB lokal: `C:/Users/Resa/AppData/Local/Android/Sdk/platform-tools/adb.exe`.
  Di Git Bash gunakan `MSYS_NO_PATHCONV=1` untuk perintah dengan path perangkat seperti `/sdcard`.
- Hindari `connectedDebugAndroidTest`: pada pengujian sebelumnya task ini menghapus app terpasang
  setelah run. Pasang APK app dan APK androidTest dengan `adb install -r`, lalu jalankan
  `MSYS_NO_PATHCONV=1 "C:/Users/Resa/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am instrument -w com.lapakhunian.app.test/androidx.test.runner.AndroidJUnitRunner`.
- Pengujian emulator terhadap produksi hanya read-only. Jangan mengirim chat kontak, transaksi,
  atau email sungguhan: kontak dapat memotong kuota lead akun produksi. Gunakan MockEngine untuk tes jaringan.
- Tes Compose: scroll sampai node di luar viewport ter-compose sebelum assertion; untuk teks
  multi-baris gunakan pencocokan substring bila perlu; dialog scrollable perlu `performScrollTo`
  sebelum `assertIsDisplayed`.
