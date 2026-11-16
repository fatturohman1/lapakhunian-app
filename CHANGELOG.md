# Changelog LapakHunian Android

Catatan perubahan aplikasi Android, bukan website. Riwayat versi mengikuti tag Git yang tersedia; bagian **Belum dirilis** berisi perubahan lokal yang belum masuk ke APK rilis.

## Belum dirilis

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
