# Persiapan Play Console

Dokumen kerja untuk pengajuan rilis (M8). Setiap pernyataan diberi status:
**[terverifikasi]** = dicek dari kode atau build di repo ini, **[belum diverifikasi]** = harus dicek sebelum
dijawab di formulir, **[keputusan]** = butuh keputusan pemilik produk.

Kebijakan Play berubah; cocokkan ulang dengan halaman kebijakan resmi saat mengisi formulir.

## 1. Fakta yang sudah dibuktikan dari kode

| Pernyataan | Bukti |
|---|---|
| App tidak punya izin INTERNET | Manifest hasil merge build release tidak memuat `INTERNET` (ML Kit menyuntikkannya lewat `datatransport`, dibuang dengan `tools:node="remove"` di `AndroidManifest.xml`). **[terverifikasi]** |
| Satu-satunya penyimpanan lokal adalah DataStore setelan | Tidak ada penulisan file/DB lain di kode (`File`, `openFileOutput`, `cacheDir`, `MediaStore` tidak dipakai); `SharedPreferences` hanya dibaca sekali untuk migrasi setelan lama. **[terverifikasi]** |
| Teks notifikasi tidak disimpan | Setelan tidak memuat teks notifikasi; notifikasi hanya ada di state memori `CapsuleState`. **[terverifikasi]** |
| Tidak memakai `QUERY_ALL_PACKAGES` | Hanya `<queries>` untuk intent LAUNCHER. **[terverifikasi]** |
| Layanan aksesibilitas tidak melakukan gestur sintetis | `canPerformGestures="false"`. **[terverifikasi]** |
| `isAccessibilityTool` tidak diset (default false) | `accessibility_service_config.xml`. Capsool bukan alat bantu disabilitas, jadi jangan dinyatakan sebagai alat aksesibilitas. **[terverifikasi]** |

## 2. Yang perlu diputuskan atau diverifikasi sebelum mengisi formulir

1. **Auto Backup [keputusan]**. `allowBackup="true"` dan `backup_rules.xml` / `data_extraction_rules.xml` masih
   berisi template kosong, sehingga **seluruh DataStore ikut Auto Backup ke akun Google pengguna**. Isinya termasuk
   isi barcode (mis. nomor kartu member), nomor telepon pada pintasan kontak, daftar app yang disembunyikan, dan
   pengaturan lainnya. Pilihan:
   - (a) biarkan: pengaturan ikut pindah ke ponsel baru, tetapi data pribadi itu ikut tersimpan di backup Google
     pengguna, dan kebijakan privasi harus menyebutnya (sudah disebut di `PRIVACY_POLICY.md`);
   - (b) kecualikan `datastore/` dari `cloud-backup` tetapi pertahankan di `device-transfer`;
   - (c) matikan backup sepenuhnya (`allowBackup="false"`).
   Rekomendasi: (b). Belum diubah di kode.
2. **SDK Google [belum diverifikasi]**. ML Kit Barcode Scanning (model bundel), `play-services-code-scanner`
   (pemindai berjalan di proses Google Play Services) dapat mengirim telemetri/diagnostik atas nama Google. Larangan
   INTERNET hanya berlaku untuk proses app ini, tidak untuk Play Services. Baca halaman pengungkapan data ML Kit dan
   Play Services milik Google, lalu pastikan jawaban "data yang dikumpulkan/dibagikan" sesuai sebelum memilih
   "tidak mengumpulkan data". Jangan menyalin jawaban dari rencana awal tanpa pengecekan ini.
3. **Persetujuan penggunaan AccessibilityService [belum diverifikasi]**. Kebijakan Play membatasi API ini untuk
   app yang bukan alat bantu disabilitas. Penolakan adalah risiko nyata (lihat tabel risiko di rencana); siapkan
   distribusi alternatif (GitHub/F-Droid) bila ditolak.
4. **Uji perangkat [belum diverifikasi]**. Alur disclosure belum direkam di perangkat fisik; terjemahan Indonesia belum
   ditinjau penutur asli.

## 3. Formulir Data safety (draf jawaban)

Jawaban akhir bergantung pada butir 2.1 dan 2.2.

| Pertanyaan | Draf jawaban | Catatan |
|---|---|---|
| Apakah app mengumpulkan atau membagikan data pengguna? | **Tahan dulu**: "tidak" hanya jika 2.2 terbukti aman | Kode app sendiri tidak mengirim apa pun (tanpa INTERNET). |
| Data dikirim lewat koneksi terenkripsi? | Tidak berlaku | Tidak ada transmisi dari app. |
| Pengguna bisa meminta penghapusan data? | Hapus data app / copot instalasi | Semua data lokal. Bila backup dibiarkan (2.1a), data di backup Google dikelola lewat akun Google. |
| Data yang diproses di perangkat saja | Isi notifikasi (memori), nama paket app di depan, daftar app terpasang (nama dan ikon), kontak yang dipilih untuk pintasan (nomor telepon), isi barcode | Tidak keluar dari perangkat oleh kode app. |

## 4. Deklarasi layanan aksesibilitas (draf teks)

**Fungsi inti**: Capsool menampilkan kapsul interaktif di sekitar lubang kamera. Kapsul harus digambar di atas aplikasi
lain dan bereaksi terhadap lingkungan layar.

**Mengapa AccessibilityService dibutuhkan** (tiap butir dipetakan ke kode):

| Kebutuhan | Cara dipakai | Kode |
|---|---|---|
| Menggambar kapsul di atas app lain | Jendela `TYPE_ACCESSIBILITY_OVERLAY` | `OverlayWindowController` |
| Menyembunyikan kapsul saat keyboard / app layar penuh terbuka | `getWindows()` (butuh `flagRetrieveInteractiveWindows` dan `canRetrieveWindowContent`) hanya untuk melihat tipe jendela (mis. jendela input method) | `CapsoolAccessibilityService` |
| Menyembunyikan kapsul di app pilihan pengguna | Membaca **nama paket** app di depan dari event `typeWindowStateChanged`; isi layar tidak dibaca | `CapsoolAccessibilityService` |
| Aksi sistem yang ditetapkan pengguna ke gestur | `performGlobalAction`: bayangan notifikasi, setelan cepat, screenshot, kunci layar | `Effect.Global` |
| Gulir ke atas atas permintaan pengguna | Mencari node yang bisa digulir dan terlihat, lalu `ACTION_SCROLL_BACKWARD`; teks/isi node tidak dibaca | `ScrollToTop` |

**Data**: tidak ada yang dikirim keluar perangkat (app tanpa INTERNET). Isi layar dan ketikan tidak dibaca.
**Bukan alat aksesibilitas**: jawab "bukan" pada pertanyaan `isAccessibilityTool`.

Teks pengungkapan yang tampil di dalam app ada di `res/values/strings.xml` (`disclosure_*`) dan harus tetap sama dengan
yang diunggah/dijelaskan di Play Console.

## 5. Skrip video demo disclosure

Rekam layar ponsel (≥ Android 12), tanpa data pribadi di layar:

1. Buka Capsool untuk pertama kali. Tampil layar **"Sebelum mengaktifkan Capsool"** (disclosure). Jeda 5 detik agar
   seluruh daftar terbaca; gulir bila perlu.
2. Ketuk **Setuju dan lanjutkan**. Tampil Setelan Aksesibilitas sistem.
3. Buka Capsool di daftar, aktifkan, setujui dialog sistem.
4. Kembali ke Capsool: status "Running". Kapsul terlihat di atas aplikasi lain.
5. Tunjukkan satu fungsi: buka keyboard di app lain, kapsul bersembunyi; tutup keyboard, muncul lagi.
6. Tunjukkan opsi **Tolak**: ulangi dari awal, ketuk **Nanti saja**; tampil layar "Capsool nonaktif" dan Setelan
   Aksesibilitas **tidak** dibuka.

## 6. Justifikasi izin (untuk kolom "permissions" dan tinjauan)

| Izin | Alasan | Kapan diminta |
|---|---|---|
| `BIND_ACCESSIBILITY_SERVICE` (layanan) | Lihat bagian 4 | Setelah disclosure |
| `BIND_NOTIFICATION_LISTENER_SERVICE` (layanan) | Menampilkan notifikasi dan pemutar media di kapsul | Pengguna mengaktifkan akses notifikasi |
| `BLUETOOTH_CONNECT` | Event headphone tersambung/terputus | Runtime, hanya saat pengguna menyalakan event itu |
| `WRITE_SETTINGS` | Slider kecerahan dan kecerahan otomatis | Akses khusus, saat pertama memakai kontrol kecerahan |
| `ACCESS_NOTIFICATION_POLICY` | Pindah ke mode senyap saat aturan Jangan Ganggu aktif | Akses khusus |
| `ACCESS_NETWORK_STATE` | Event Wi-Fi tersambung/terputus | Normal |
| `QUERY_ADVANCED_PROTECTION_MODE` | Memperingatkan bahwa Perlindungan Lanjutan dapat mematikan layanan | Normal, hanya baca |
| `<queries>` LAUNCHER | Menampilkan nama dan ikon app (sumber notifikasi, daftar app) | Normal |

## 7. Daftar toko (draf)

- **Judul**: Capsool
- **Deskripsi singkat (EN)**: A smart capsule around your camera cutout: media, notifications, timers and shortcuts.
- **Deskripsi singkat (ID)**: Kapsul pintar di sekitar lubang kamera: media, notifikasi, timer, dan pintasan.
- Cantumkan di deskripsi panjang, di baris pertama, bahwa app memakai layanan aksesibilitas dan untuk apa
  (selaras dengan kebijakan Play), serta bahwa app tidak memakai internet.
- Bahasa: Inggris (default) dan Indonesia.

## 8. Daftar periksa sebelum kirim

- [ ] Putuskan butir 2.1 (backup) dan perbarui kode + kebijakan privasi sesuai pilihan.
- [ ] Verifikasi butir 2.2 (SDK Google) lalu finalkan jawaban Data safety.
- [ ] Unggah kebijakan privasi (`PRIVACY_POLICY.md`) ke URL publik dan isi URL-nya di Play Console.
- [ ] Rekam video bagian 5 di perangkat fisik dan unggah.
- [ ] Tinjauan terjemahan Indonesia oleh penutur asli.
- [ ] Smoke test build release (R8) di perangkat; tandatangani dengan keystore rilis.
- [ ] Isi bagian "Akses aplikasi" dengan akun uji bila Play memintanya (app ini tidak memakai login).
