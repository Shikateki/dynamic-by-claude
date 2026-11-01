# Dynamic Island Pro

Dynamic Island native Android (Kotlin) untuk Redmi 12 · HyperOS 2 · Android 15.

> **Status jujur:** kode ditulis dan di-review terhadap API Android, tetapi **belum pernah dikompilasi
> maupun dijalankan di perangkat** (lingkungan tempat kode ini ditulis tidak bisa mengunduh Android SDK).
> Anggap build pertama sebagai tahap uji. Kirim pesan error apa adanya jika ada, dan akan diperbaiki.

## Cara build dari HP (tanpa PC) — GitHub Actions

1. Buat akun GitHub, buat repository baru (Private juga boleh).
2. Unggah **seluruh isi** folder project ini (termasuk folder tersembunyi `.github`).
   Di aplikasi GitHub mobile / browser: Add file → Upload files. Jika browser HP tidak bisa
   mengunggah folder, unggah ZIP ke tempat lain lalu gunakan "github.dev" atau Termux (`git push`).
3. Buka tab **Actions** → workflow **Build APK** → **Run workflow**.
4. Tunggu ±5–10 menit. Setelah hijau, buka run tersebut → bagian **Artifacts** → unduh
   `DynamicIslandPro-release-apk` (ZIP berisi `app-release.apk`).
5. Ekstrak, pasang APK-nya (izinkan "Install dari sumber tidak dikenal").

Workflow juga menjalankan unit test (`TimerClockTest`) sebelum membangun APK.

APK release ditandatangani dengan kunci debug supaya langsung bisa dipasang untuk pemakaian pribadi.
Untuk Play Store diperlukan keystore sendiri.

## Setelah dipasang (HyperOS 2)

1. Buka aplikasi → tab **Izin**. Baca "Panduan HyperOS 2" dan ikuti urutannya.
   Langkah 1 (Izinkan pengaturan terbatas) wajib sebelum Akses Notifikasi / Accessibility bisa diaktifkan.
2. Berikan: Tampil di atas aplikasi lain, Akses Notifikasi, Notifikasi aplikasi.
3. Tab **Beranda** → **Nyalakan Island** → **Tampilkan contoh**.

## Struktur

```
core/       DynamicIslandEngine, IslandView, AnimationEngine, OverlayManager, IslandController, PermissionManager
services/   IslandService (satu-satunya service permanen), IslandNotificationListener,
            IslandAccessibilityService (opsional), BootReceiver
features/   notification (di listener), music, charging, volume, connectivity, timer, customization
ui/         dashboard, preview, settings (fitur/tampilan/tentang), permissions
utils/      PreferencesManager, TimeFormat
```

## Checklist uji manual (Phase 11) — dijalankan di Redmi 12

| # | Uji | Hasil yang benar |
|---|-----|------------------|
| 1 | Nyalakan Island tanpa izin overlay | Diarahkan ke pengaturan, tidak crash |
| 2 | Nyalakan Island dengan izin | Notifikasi "Island aktif", status Beranda hijau |
| 3 | Beranda → Tampilkan contoh | Pill muncul dengan animasi; 3 aktivitas bisa digeser kiri/kanan |
| 4 | Ketuk / tekan lama / geser atas / ketuk di luar | Expand / kontrol / collapse / tutup |
| 5 | Kirim notifikasi dari app lain (Akses Notifikasi aktif) | Muncul sekali, tidak ganda; ×N bila beruntun |
| 6 | Putar musik (Spotify / YouTube Music) | Judul, artis, cover, ⏮⏯⏭, progres, equalizer |
| 7 | Colok / cabut charger | Island "Mengisi daya" / "dicabut" |
| 8 | Tekan tombol volume | Indikator volume (lihat catatan di bawah) |
| 9 | Matikan Wi-Fi / Bluetooth / mode pesawat | Island muncul sesuai perubahan |
| 10 | Timer 1 menit, jeda, lanjut, reset; stopwatch | Teks live berjalan; selesai → Island + notifikasi |
| 11 | Matikan Accessibility (Mode B saja) | Semua fitur utama tetap jalan, tidak ada error |
| 12 | Aktifkan lalu matikan Accessibility | Kartu peringatan di Beranda, Island tetap jalan |
| 13 | Tampilan → ubah slider/warna | Preview dan overlay berubah langsung |
| 14 | Restart HP dengan Island menyala | Island menyala lagi (butuh Autostart aktif) |

Catatan yang perlu dicek di perangkat nyata:
- **Volume:** memakai broadcast `VOLUME_CHANGED_ACTION` (tidak terdokumentasi). Jika HyperOS tidak mengirimnya, indikator tidak muncul.
- **Animasi 60 FPS:** ukuran Island dianimasikan lewat LayoutParams jendela overlay. Belum diukur; jika patah-patah di Helio G88, laporkan.
- **Tekan lama / geser:** gesture memakai GestureDetector standar.

## Belum ada (alasan)
Indikator screenshot, perekaman layar, clipboard, pengingat kalender: tidak ada API resmi yang layak untuk aplikasi biasa
di latar belakang (atau butuh izin sensitif tambahan). Tidak dibuat daripada memakai API fiktif.
