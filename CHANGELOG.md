# Changelog LapakHunian Android

Catatan perubahan aplikasi Android, bukan website. Riwayat versi mengikuti tag Git yang tersedia; bagian **Belum dirilis** berisi perubahan lokal yang belum masuk ke APK rilis.

## Belum dirilis

### Diubah

- **Dashboard Saya didesain ulang**: header gradien hijau berisi tombol kembali,
  judul, ringkasan statistik (Aktif · Review · Dilihat) dan tombol Pasang Iklan —
  tombol mengambang lama yang menutupi kartu dihapus. Kartu properti dirapikan:
  foto di-crop penuh (garis hijau atas/bawah hilang), nama kota dengan ikon pin,
  harga ringkas di baris status, jumlah dilihat dengan ikon mata. Aksi
  Edit/Terjual/Hapus/Perpanjang tidak berubah.

### Ditambahkan

- **Analytics pengunjung dari aplikasi**: membuka detail properti kini mencatat
  kunjungan ke halaman Analytics Pengunjung web dengan sumber "Aplikasi Android"
  dan path format web (/properti/{slug}) sehingga agregatnya menyatu dengan web.
  Lokasi perkiraan (coarse) ikut dilaporkan HANYA bila izin lokasi sudah pernah
  diberikan (tanpa memunculkan dialog izin baru) — tanpa izin, lokasi memakai
  perkiraan GeoIP seperti web. Semua request kini membawa penanda versi
  X-Client-App untuk kebutuhan statistik/debug server.

## 1.0.0-beta.14

### Diperbaiki

- Ikon di dalam kotak tint (menu profil, pengaturan, dashboard, spesifikasi, tombol
  lingkaran detail) menggambar penuh kotak sehingga kelihatan terlalu besar dan tebal —
  ukuran ikon kini dihormati di dalam `Box` pembungkus (constraint loose), kotak dan
  posisi tengah tetap. Perbaikan berlaku global untuk semua pemakaian
  `PhIcon(size = x, modifier = Modifier.size(kotak).background(...))`.

## 1.0.0-beta.13

### Diubah

- Seluruh ikon aplikasi kini dirender sebagai vektor murni (`ImageVector` yang digenerate
  dari font Phosphor untuk 62 ikon yang dipakai, Regular + Fill) — bentuk ikon identik dan
  presisi di semua perangkat, tidak lagi terpengaruh metrik font, skala font sistem, atau
  quirk OEM. Menutup tuntas keluhan ikon terpotong/tidak presisi yang berulang.
- Splash sistem (Android 12+) kini putih polos tanpa logo statis — logo lama di splash
  sistem dihapus; brand splash dalam app muncul setelahnya dengan fade-in halus.
- Launcher icon baru: logo LapakHunian dengan stroke di adaptive icon, logo diperkecil
  di dalam safe-zone supaya tidak mepet ke tepi mask; PNG foreground lama dihapus dan
  ikon notifikasi (FCM + lokal) ikut memakai vektor baru.

### Diperbaiki

- Nama agen di halaman profil tidak lagi terpotong bagian atas (lineHeight eksplisit
  27sp untuk teks 21sp); identitas disusun ulang — hanya avatar yang overlap banner,
  nama dan pill link dipindah ke bawah avatar sehingga tidak ada teks di area banner.

## 1.0.0-beta.12

### Diperbaiki

- Nama agen di halaman profil terpotong bagian atas (lineHeight bawaan style lebih kecil
  dari teks 21sp); perbaikan ini disempurnakan lagi di beta.13 dengan penataan ulang.

## 1.0.0-beta.11

### Ditambahkan

- **Splash brand dalam app** sesuai mockup: logo + wordmark LapakHunian.com + bar loading
  beranimasi + tagline "Temukan hunian impianmu", tampil ±1,2 detik saat cold start
  dengan fade-in/out halus; warm start langsung masuk tanpa splash.

### Diubah

- Profil agen ditata ulang: avatar 92dp dengan ring di banner, nama 21sp, pill link
  profil publik (ketuk membuka lapakhunian.com/agen/{slug} di browser), statistik dalam
  satu kartu segmented (rating · listing aktif · saldo lead), kartu Tentang & Kredensial
  selalu tampil dengan placeholder saat kosong.
- Dokumentasi aturan push di AGENTS.md (beberapa push ke remote jangan barengan).

### Diperbaiki

- Layout thread chat saat keyboard membuka: memakai pola IME resmi Compose
  (root imePadding + consumeWindowInsets) dan reverseLayout dengan alignment bottom —
  pesan selalu menempel di atas kotak ketik, header tetap terlihat, tanpa gap besar.
- Ikon Phosphor presisi: line box dihitung dari metrik font (ascent 0.938em,
  descent 0.062em) sehingga glyph tidak terpotong.

## 1.0.0-beta.10

### Diubah

- Splash sistem kembali ke pola guideline Android 12+ (ikon statis di lingkaran tengah),
  menggantikan animasi loader; splash brand baru memakai pola ini sebagai basis.

### Diperbaiki

- Layout thread chat saat keyboard membuka (iterasi pertama reverseLayout).
- Ikon Phosphor terpotong (iterasi pertama line box 1.124em).

## 1.0.0-beta.9

### Ditambahkan

- **Status pesan chat ala WhatsApp** pada bubble pesan sendiri: jam saat mengirim,
  ceklis 1 saat terkirim, ceklis 2 biru saat dibaca (read receipt dihitung server dari
  `lastReadAt` lawan chat; server lama tetap aman → ceklis 1); timestamp pindah masuk
  bubble.
- **Badge unread** pada tab Chat di bottom nav (merah dengan jumlah, maks "99+",
  tersembunyi saat belum login; polling 5 detik saat tab terlihat, 30 detik untuk badge).
- **Edit profil agen**: banner, avatar, Tentang, No. HP, Perusahaan, No. Lisensi AREBI
  melalui `PATCH /api/agent/profile` (field whitelist sama dengan dashboard web).
- Routing klik notifikasi ke tujuan native: thread chat, tab chat, halaman agen,
  detail properti, bantuan, riwayat — link yang tidak dikenal tetap hanya ditandai
  dibaca (`NotificationLinkMapper` + unit test).

### Diubah

- Unduhan pembaruan aplikasi lewat `lapakhunian.com/app/download` (proxy web dengan
  filename benar), bukan link GitHub langsung; cek versi tetap GitHub API.
- Peta di halaman properti dihapus (peta hanya di web); alamat properti tetap tampil.
- Thread chat bottom-aligned; pesan selalu dekat kotak ketik.
- Splash sistem memakai ikon statis sesuai guideline Android 12+ (menggantikan
  animasi loader AVD di beta.9 awal).

### Catatan

- Server (hously): field `read` per pesan pada GET/POST `/api/chat/messages`,
  dihitung dari `lastReadAt` lawan chat; backward compatible, tanpa migration.
- Konfigurasi Firebase dan Google Auth tidak diubah.

## 1.0.0-beta.8

### Diperbaiki

- Form kontak dengan budget angka (mis. 99000) tidak lagi gagal kirim dengan error
  "Serializing collections of different element types"; request kini memakai DTO bertipe
  jelas sehingga budget dikirim sebagai JSON number sesuai kontrak server.
- Metadata channel notifikasi FCM dipindah ke level `<application>` di manifest (sebelumnya
  salah di dalam `<service>`); notifikasi background kini memakai channel heads-up yang benar.
- Token push FCM diregistrasi ulang saat aplikasi dibuka (jika sudah login) dan setelah login,
  termasuk ketika token cached hilang setelah logout; sebelumnya token tidak pernah dikirim
  ulang sampai Firebase kebetulan memanggil onNewToken.
- Registrasi token di FcmService tidak lagi memblokir thread dengan runBlocking.

### Catatan

- Konfigurasi Firebase dan Google Auth tidak diubah.
- Agen ke agen tetap bisa chat; error sebelumnya bukan pembatasan peran melainkan bug serialisasi.

## 1.0.0-beta.7

### Ditambahkan

- **Properti terdekat** di Jelajah: tombol pin peta mengurutkan listing berdasarkan jarak dari lokasi perangkat dan menampilkan label jarak pada kartu; listing tanpa koordinat ditaruh di akhir.
- Izin lokasi coarse diminta hanya saat fitur Terdekat dipakai; lokasi dibaca sekali di perangkat dan tidak dikirim ke server.
- Baris **Notifikasi** di Pengaturan: meminta ulang izin notifikasi bila dulu ditolak, atau membuka pengaturan notifikasi sistem bila sudah aktif.

### Diperbaiki

- Form kontak tidak lagi menampilkan error mentah "Illegal input … $.data.deducted" saat server membalas `deducted` berupa boolean; pengiriman sukses langsung membuka thread chat.
- Notifikasi chat/lead tampil sebagai banner heads-up bersuara (channel IMPORTANCE_HIGH) ala aplikasi pesan instan, bukan masuk tray diam-diam.
- Notifikasi FCM saat app background memakai channel heads-up milik app (default channel di manifest), bukan channel fallback FCM.
- Pesan baru dari percakapan berbeda menumpuk sebagai notifikasi terpisah, bukan saling menggantikan.

### Catatan

- Urutan Terdekat memakai maksimal 100 kandidat dari satu request API (batas server) yang diurutkan di sisi app.
- Konfigurasi Firebase dan Google Auth tidak diubah.

## 1.0.0-beta.6

### Ditambahkan

- Cek update manual dengan mengetuk **Pengaturan > Versi Aplikasi**.
- Indikator saat pemeriksaan berlangsung serta pesan jika versi sudah terbaru atau pemeriksaan gagal.
- Cek update otomatis ketika aplikasi dibuka atau kembali aktif, maksimal sekali per 24 jam setelah pemeriksaan berhasil.
- Dialog ketika ada versi lebih baru, menampilkan versi terpasang dan versi terbaru dengan pilihan **Unduh Update** atau **Nanti**.
- Pemeriksaan versi terbaru melalui GitHub Releases menggunakan koneksi terpisah yang tidak membawa token sesi pengguna.

- Alur lupa password melalui email menggunakan endpoint yang sudah tersedia; tautan reset dibuka dari email.
- Layar konfirmasi setelah registrasi dengan alamat email tujuan, tombol buka email, kirim ulang verifikasi, dan kembali ke login.
- Jeda 60 detik untuk permintaan ulang email dari layar pemulihan/verifikasi.
- Galeri foto layar penuh dengan zoom pinch dan double-tap (maksimal 4x), geser foto saat zoom, serta tombol tutup eksplisit di kanan atas.
- Baris **Terakhir dilihat** di Beranda dari riwayat lokal perangkat (maksimal 10, terbaru di depan, tanpa login).
- Favorit sementara tanpa login: ikon hati menyimpan pilihan di perangkat; tab Favorit tamu menampilkan daftar sementara dengan banner ajakan masuk.
- Penggabungan favorit sementara ke akun saat login; item yang gagal digabung tetap tersimpan lokal dan dapat dicoba lagi.
- Mode bandingkan di Jelajah (pilih 2-3 properti) dan layar **Bandingkan Properti**: harga, estimasi cicilan KPR (DP 20%, 7,5%, 15 tahun), LT/LB, KT/KM, sertifikat, dan kota berdampingan.
- Dialog update menampilkan ringkasan perubahan rilis (catatan rilis GitHub yang dirapikan) serta panduan singkat pemasangan APK.

### Diperbaiki

- Filter pencarian baru diterapkan setelah tombol Terapkan; Batal tidak mengubah hasil. Harga minimum/maksimum divalidasi, filter aktif diringkas, dan Reset menghapus kriteria pencarian.
- Form iklan memulihkan isian dan langkah dari saved state; Back kembali ke langkah sebelumnya dan keluar melalui Back/tab meminta konfirmasi sebelum membuang perubahan.
- Pengaturan dan Bantuan dapat diakses tanpa login; ubah password tetap khusus pengguna yang sudah masuk, dan layar Favorit tamu menyediakan tombol Masuk.
- Kegagalan memuat favorit ditampilkan sebagai error dengan Coba lagi, bukan daftar kosong; layar mengikuti perubahan sesi login.
- Kontras teks pada tombol/gradasi hijau diperkuat tanpa mengganti identitas warna hijau.
- Chip filter mengumumkan status terpilih; karakter font ikon dekoratif tidak dibacakan sebagai teks.
- Cek update otomatis mengikuti lifecycle foreground dan hanya mencatat interval 24 jam setelah pemeriksaan berhasil.
- Harga pada kartu daftar properti memakai baris selebar kartu dan dapat membungkus teks agar nominal tidak terpotong pada layar kecil atau font besar.
- Pengaturan dapat digulir; versi aplikasi ditempatkan di bawah keterangannya agar akses cek update tetap terjangkau.
- Status pembayaran langganan hanya dinyatakan berhasil jika ID langganan sesuai tagihan yang dibuka dan server mengembalikan status aktif, bukan sekadar karena akun sudah memiliki langganan aktif.
- Pembacaan URL dan token pembayaran disesuaikan dengan nama field respons API yang tersedia.
- Pengiriman chat gagal tidak lagi menimpa draf baru; pesan gagal tetap terlihat dan bisa dicoba ulang tanpa mengosongkan draf.
- Navigasi bawah disembunyikan pada alur yang butuh fokus (percakapan chat, form iklan, pembayaran, kontak, login/registrasi, dan layar pembanding) dan tetap tampil pada tab utama serta halaman jelajah seperti detail properti.
- Saat foto di-zoom, geser horizontal menggeser foto alih-alih pindah halaman; zoom kembali ke 1x mengaktifkan geser halaman lagi, dan zoom diatur ulang setiap ganti foto.

### Catatan

- Unduhan APK dibuka melalui browser; pemasangan update tetap dilakukan manual oleh pengguna.
- Pemberitahuan update berupa dialog di dalam aplikasi, bukan notifikasi push atau pemeriksaan saat aplikasi ditutup.
- Pemeriksaan update otomatis yang gagal tetap senyap dan dapat dicoba lagi saat aplikasi kembali aktif setelah jeda minimal 60 detik; cek manual tetap tersedia.
- Perbaikan audit mencakup P1, P2, dan P3, beserta empat ide tambahan terpilih: riwayat terakhir dilihat, favorit sementara tanpa login, pembanding properti, dan ringkasan perubahan saat update.
- Konfigurasi Firebase dan Google Auth tidak diubah.

## 1.0.0-beta.5

### Ditambahkan

- Unggah maksimal 3 foto properti pada form titip jual.
- Pemilihan foto melalui Android Photo Picker dan pengunggahan melalui endpoint publik, tanpa perlu login.

## 1.0.0-beta.4

### Ditambahkan

- Menu ubah password.
- Daftar dan pengiriman tiket bantuan.
- Form titip jual tanpa login.
- Pelaporan properti palsu atau menyesatkan.
- Dukungan Android App Links untuk membuka tautan detail properti di aplikasi.

## 1.0.0-beta.3

### Ditambahkan

- Notifikasi chat melalui Firebase Cloud Messaging (FCM), dengan pemeriksaan cadangan menggunakan WorkManager.
- Pilihan peran untuk akun Google baru yang belum menyelesaikan onboarding.
- Pengajuan komplain lead.
- Banner promosi di Beranda.

## 1.0.0-beta.2

### Distribusi dan build

- Distribusi APK release bertanda tangan melalui GitHub Releases, dengan nama berkas `lapakhunian-beta.apk`.
- Pembuatan release otomatis melalui tag versi `v*`.
- Perbaikan izin eksekusi Gradle wrapper untuk runner Linux.
- Penghapusan konfigurasi path Java lokal Windows dari konfigurasi build bersama agar proses CI dapat berjalan.
