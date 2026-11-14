# LapakHunian Android

Aplikasi Android native (Kotlin + Jetpack Compose, Material 3) untuk **lapakhunian.com** — 100% consume REST API web yang sudah ada, tanpa WebView, tanpa backend baru.

## Fitur (v1.0.0)

| Area | Isi |
|---|---|
| Buyer | Home (unggulan/kategori/kota), Jelajah (search+filter+Paging 3), Detail (pager/spec/peta), Favorit |
| Auth | `POST /auth/mobile-login` → Bearer JWT di DataStore; register + verifikasi email |
| Chat | Contact agent → percakapan, polling 5s (SSE opsional M9), unlock paywall sisi agen |
| Owner | Dashboard (stat + status badge), Pasang Iklan wizard 4 langkah (upload foto → WebP), Perpanjang Rp25rb |
| Agen | Dashboard (saldo lead, slot, paket), Leads (paket+pembelian), Langganan (Basic/Pro/Enterprise) |
| Payment | Midtrans Snap via Chrome Custom Tabs + polling status 3s |
| Notifikasi | List + unread + tandai dibaca (polling) |

## Menjalankan

```bash
# buka di Android Studio (JDK 17 bawaan JBR) atau:
./gradlew assembleDebug          # APK debug
./gradlew bundleRelease          # AAB release (butuh signing utk publish)
```

`local.properties` → `sdk.dir`; base API di `BuildConfig.API_BASE_URL` (default `https://lapakhunian.com`).

## Struktur

```
app/src/main/java/com/lapakhunian/app/
  core/network/     Ktor + PropertyApi (112 endpoint web), DTO envelope {success,data,error,meta}
  core/data/        Room cache, SessionManager (token+user), Repositories
  core/domain/      Model + mapper
  core/designsystem/ Theme (#15A851, League Spartan), komponen (PropertyCard, StatusBadge, SpecGrid, …)
  core/ui/          Navigasi + bottom nav M3-style
  feature/          home · explore · detail · chat · auth · favorites · profile · dashboard · notifications · payments
```

## Referensi

- Preview desain: `preview/index.html` (acuan piksel M0)
- Kontrak API: repo web `hously/docs/API-REFERENCE.md`, `docs/android/API_BRIDGE.md`
- Design token: `core/designsystem/Color.kt` (dari web `globals.css`)

## Catatan deployment

CI: `.github/workflows/android.yml` — build debug + AAB; signed release aktif setelah secrets `ANDROID_KEYSTORE_*` diisi. Package: `com.lapakhunian.app`.
