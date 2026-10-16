# Capsool — PRD, Technical Specification & Execution Plan (v3.0)

| | |
|---|---|
| Versi | 3.0 (menggantikan `CAPSOOL_PRD_SPEC_PLAN_V2 (1).md`) |
| Tanggal | 2026-10-15 |
| Owner | Iqbal Fauzi |
| Package | `com.iqbalwork.capsule` |
| Repo | `/Users/iqbalfauzi/Personal/App/capsule` |

### Perubahan dari v2.1
- **Framing**: Capsool diposisikan sebagai produk sendiri, bukan re-implementasi atau "unlock Pro" aplikasi lain. Semua referensi ke versi, packer, dan fitur berbayar aplikasi pihak ketiga dihapus.
- **Scope**: setiap fitur punya ID (`F-xx`) dan prioritas (P0/P1/P2). Fitur yang sebelumnya terlewat ditambahkan: dynamic color dari wallpaper, scroll-to-top, aturan visibilitas (fullscreen, keyboard, landscape, lock screen), blacklist per app, onboarding izin, shape/animasi.
- **Spec**: setiap fitur dijelaskan sampai level API Android, izin, versi minimum, edge case, dan fallback. Ditambah arsitektur side effect (reducer tetap pure, efek dijalankan oleh `EffectHandler`).
- **Plan**: milestone diubah jadi *vertical slice* yang bisa dipakai di HP asli, masing-masing dengan acceptance criteria di device. "Unit test lulus" bukan lagi kriteria selesai.
- **Baseline**: dokumen ini mencatat jujur apa yang sudah ada di kode (lihat §3.1).
- **v3.1 (riset pasar)**: daftar fitur dilengkapi dari inspeksi level-fitur aplikasi sejenis (lihat Lampiran A): model kartu dinamis vs kartu statis, gesture dua konteks, intent builder, profil layar per orientasi, mode kapsul, kontrol animasi, palet siang/malam, peringatan Advanced Protection Android 17. `minSdk` naik ke 31.

---

# BAGIAN 1 — PRODUCT REQUIREMENT DOCUMENT

## 1.1 Visi

**Capsool** mengubah area kamera depan (punch-hole) di Android menjadi kapsul interaktif yang menampilkan aktivitas yang sedang berjalan — musik, notifikasi, charging, timer, download — dan memberi akses cepat ke kontrol yang sering dipakai, tanpa harus membuka notification shade atau aplikasinya.

Dibangun dengan **Jetpack Compose + Material 3 Expressive**, terasa seperti bagian dari sistem Android (warna ikut wallpaper, motion berbasis spring), bukan tiruan iOS.

### Prinsip produk
1. **Native-feeling** — mengikuti Material You; tidak memaksakan estetika iOS.
2. **Jujur soal izin** — setiap izin diminta saat fiturnya diaktifkan, dengan alasan yang jelas. Tidak ada izin yang tidak dipakai.
3. **Offline & privat** — tidak ada akses internet, analytics, atau iklan. Data notifikasi tidak pernah disimpan ke disk.
4. **Hemat** — saat tidak ada aktivitas, kapsul tidak boleh menggambar ulang setiap frame.
5. **Bisa dimatikan per fitur** — setiap kartu dan event bisa dinonaktifkan sendiri-sendiri.

### Posisi pasar
Kategori "island/capsule untuk Android" sudah ada di Play Store dan ramai peminat. Capsool tidak meniru aset, nama, atau teks aplikasi mana pun. Diferensiasi Capsool:
- Gratis, tanpa iklan, tanpa koneksi internet.
- Kustomisasi terbuka (semua opsi tersedia, tidak ada paywall).
- Integrasi otomasi (Tasker/MacroDroid) dua arah sebagai fitur kelas satu.

## 1.2 Target pengguna

| Persona | Kebutuhan utama |
|---|---|
| **Penikmat kustomisasi** | Tampilan rapi yang menyatu dengan punch-hole, banyak opsi bentuk/warna/animasi |
| **Pengguna musik/podcast** | Kontrol media tanpa membuka app, terlihat cepat lagu apa yang diputar |
| **Pengguna chat berat** | Lihat & balas pesan singkat tanpa pindah aplikasi |
| **Power user / otomasi** | Gesture yang bisa dipetakan ke aksi apa pun, broadcast ke Tasker |

## 1.3 Metrik keberhasilan

| Metrik | Target |
|---|---|
| RAM (PSS) proses saat idle, overlay aktif | ≤ 80 MB |
| Konsumsi baterai yang diatribusikan ke Capsool, layar nyala, idle 1 jam | ≤ 1% |
| Frame idle (tidak ada event) | 0 recomposition/frame — tidak ada animasi infinite saat idle |
| Waktu dari event sistem ke kapsul bereaksi | ≤ 300 ms |
| Jank saat expand/collapse | < 1% frame > 16 ms di device kelas menengah |
| Service tetap hidup | ≥ 24 jam tanpa mati di Pixel, Samsung, Xiaomi (dengan panduan keepalive diikuti) |
| Crash-free sessions | ≥ 99,5% |

## 1.4 Daftar fitur & prioritas

**P0** = wajib untuk rilis pertama (MVP). **P1** = rilis berikutnya. **P2** = eksperimental / nanti.

### A. Kapsul inti
| ID | Fitur | Prioritas |
|---|---|---|
| F-01 | Kapsul overlay di atas punch-hole, posisinya otomatis terdeteksi dari `DisplayCutout` | P0 |
| F-02 | Kalibrasi manual: posisi (tengah/kiri/kanan/custom X-Y), lebar, tinggi, corner radius, dengan target kalibrasi merah di atas kamera | P0 |
| F-03 | State kapsul: **Hidden** → **Idle pill** → **Mini event** → **Compact card** → **Expanded card** | P0 |
| F-04 | Opsi "selalu tampilkan pill" walaupun tidak ada aktivitas, plus opacity idle (0–100%) | P0 |
| F-05 | Carousel kartu: kalau ada beberapa aktivitas, swipe kiri/kanan untuk berpindah | P0 |
| F-06 | Prioritas kartu otomatis (mis. notifikasi baru > timer > media > charging) yang bisa diatur ulang | P1 |
| F-07 | Screen corner radius (menyamarkan lengkung sudut layar) | P2 |
| F-08 | **Profil layar**: geometri kapsul disimpan per resolusi × orientasi (portrait/landscape), dibuat otomatis saat resolusi/orientasi baru terdeteksi | P1 |
| F-09 | **Mode kapsul**: "menyatu dengan kamera" (menutupi cutout) vs "mandiri" (di bawah status bar, semua gesture normal); lebar kartu "ikut konten" vs "penuh"; radius kartu kustom | P1 |
| F-04b | Mini capsule: skala 100–200% dengan anchor (atas/tengah/bawah), alignment kiri/tengah/kanan, animasi fade untuk device tanpa cutout atau di landscape | P1 |

**Dua jenis kartu.** *Kartu dinamis* muncul otomatis saat ada aktivitas (media, volume saat ada audio, senter saat menyala, progress, charging, timer, notifikasi). *Kartu statis* dibuat user lewat tombol tambah (pintasan app, pintasan kontak, slider, barcode/QR, widget, intent kustom), diurutkan manual (naik/turun), bisa ditandai sebagai kartu awal, dan ditampilkan sebagai carousel atau list saat kapsul dibuka tanpa aktivitas.

### B. Kartu aktivitas
| ID | Fitur | Prioritas |
|---|---|---|
| F-10 | **Media**: judul, artis, cover, play/pause, next/prev, seek bar, warna kartu dari cover | P0 |
| F-11 | Media: mini indicator (cover berputar / visualizer sintetis) di pill | P1 |
| F-12 | Media: aturan auto-hide (timeout 5s–2m, saat pause, saat app musik di foreground) | P1 |
| F-13 | Media: pilih app yang boleh tampil (whitelist) | P1 |
| F-14 | Media: dua ukuran pemutar — **compact** (bagian dari carousel, seek bar ditampilkan/disembunyikan dengan long press judul/cover) dan **full-size** (seek bar selalu tampil, kontrol tambahan, carousel nonaktif) | P1 |
| F-15 | Media: tampilan — label/ikon app pemutar/tidak ada, gaya cover (border/tanpa border), tipe tombol, warna default vs warna cover, getaran saat tombol ditekan; collapse otomatis saat rotasi, layar terkunci, atau sesi media hilang | P1 |
| F-20 | **Notifikasi**: tampil di kapsul (mode ringkas & expanded), avatar pengirim, badge app, glow sesuai warna app | P0 |
| F-21 | Notifikasi: balas langsung (inline reply), mark as read, dismiss | P0 |
| F-22 | Notifikasi: blacklist/whitelist per app, sembunyikan isi saat terkunci | P0 |
| F-23 | Notifikasi: swipe ke atas untuk collapse, swipe samping untuk dismiss | P1 |
| F-24 | Notifikasi: mode tampil **ringkas** (expand saat di-tap) vs **langsung expanded**; delay auto-hide 1/3/5/10/20/30 detik atau "sampai dibuka" | P0 |
| F-25 | Notifikasi: hapus otomatis dari notification shade setelah dilihat di kapsul (opsional) | P1 |
| F-26 | Panduan per app untuk mematikan pop-up (heads-up) sistem agar tidak dobel dengan kapsul — tap app → buka pengaturan notifikasi app itu | P0 |
| F-30 | **Charging**: mini event saat colok/cabut (AC/USB/wireless), persen, estimasi penuh, suhu | P0 |
| F-31 | Peringatan baterai lemah (ambang bisa diatur, default 15%) dan baterai penuh | P0 |
| F-32 | Satuan suhu °C/°F | P1 |
| F-40 | **Progress**: download/upload dari Play Store, browser, Telegram, DownloadManager, dll. (dibaca dari notifikasi berprogress) | P1 |
| F-41 | **Timer & stopwatch** dari aplikasi jam (Google Clock dulu, lalu app lain berbasis chronometer) dengan aksi +1 menit / pause / dismiss; warna waktu & tombol bisa diatur | P1 |
| F-43 | Progress: pilih app yang dilacak, gaya ikon (asli/bentuk), warna progress & track | P1 |
| F-42 | Notifikasi "live" generik: apa pun yang memakai `ProgressStyle`/chronometer (navigasi, ojol, upload) | P2 |

### C. Kontrol & pintasan
| ID | Fitur | Prioritas |
|---|---|---|
| F-50 | **Slider volume** media + toggle mode dering (Normal ↔ Getar ↔ Senyap); muncul juga sebagai kartu dinamis saat ada audio diputar | P0 |
| F-51 | Gaya slider volume: 3 varian visual (pill tunggal, segmented, rounded) | P1 |
| F-52 | **Slider brightness** + toggle adaptive, dengan kalibrasi nilai maksimum | P1 |
| F-53 | **Senter** on/off + slider kekuatan (di device yang mendukung); kartu dinamis saat senter menyala | P0 (on/off), P1 (strength) |
| F-59 | Kartu slider: tombol toggle di samping slider (auto-brightness / mode dering / tidak ada) + toggle cepat tambahan (panel internet, lock, screenshot, panel Wi-Fi, panel volume); bentuk & warna toggle/slider; sembunyikan saat app media di layar | P1 |
| F-54 | Pintasan aplikasi (ikon asli atau themed/monochrome), bentuk & ukuran ikon, buka dengan tap / double tap / long press, collapse setelah dibuka | P1 |
| F-55 | Pintasan kontak: telepon & chat WhatsApp; label teks/emoji atau inisial; nomor diketik manual atau diimpor dari kontak | P1 |
| F-56 | Pintasan sistem: Wi-Fi, Bluetooth, Display, Location, Battery settings | P1 |
| F-57 | Kartu barcode/QR: buat dari isi yang diketik atau hasil scan kamera/gambar; format QR, EAN-13/8, UPC-A/E, Code 128/39/93, Codabar, ITF; label + warna; opsi flip | P1 |
| F-58 | Widget Android pihak ketiga di dalam kapsul (pilih tema jika provider menyediakan) | P2 |

### D. Mini event (2–4 detik)
| ID | Fitur | Prioritas |
|---|---|---|
| F-60 | Charger colok/cabut | P0 |
| F-61 | Mode dering berubah | P0 |
| F-62 | Perangkat Bluetooth audio tersambung/terputus + baterai (jika tersedia) | P1 |
| F-63 | Wi-Fi tersambung/terputus | P1 |
| F-64 | Mode pesawat on/off | P1 |
| F-65 | Device unlock (animasi sapaan) | P1 |
| F-66 | USB/OTG storage terpasang/dilepas | P2 |
| F-67 | Baterai AirPods (via BLE) | P2 |
| F-68 | Durasi tampil setiap event bisa diatur; tampilan per event (ikon, warna, bentuk sisi kiri & kanan) | P1 |
| F-69 | Event: mode brightness (adaptive on/off) berubah | P1 |

### E. Gesture & otomasi
| ID | Fitur | Prioritas |
|---|---|---|
| F-70 | 5 gesture: tap, double tap, long press, swipe kiri, swipe kanan — semua bisa dipetakan | P0 |
| F-71 | Aksi yang tersedia: expand/collapse, kartu berikut/sebelumnya, play/pause, senter, buka notification shade, buka quick settings, screenshot, lock screen, buka app tertentu, kirim broadcast, tidak ada | P0 (subset), P1 (lengkap) |
| F-72 | Scroll-to-top: tap kapsul menggulir app aktif ke paling atas | P1 |
| F-73 | Broadcast keluar saat gesture/event (untuk Tasker, MacroDroid, Automate) | P1 |
| F-74 | Intent masuk: app otomasi (atau app pihak ketiga seperti pembaca baterai AirPods) bisa menampilkan mini event kustom (teks + ikon + persen) | P2 |
| F-75 | **Gesture dua konteks**: pemetaan terpisah untuk "saat ada kartu dinamis aktif" dan "saat tidak ada" | P1 |
| F-76 | **Intent builder**: aksi kustom berupa Activity / Service / Broadcast dengan action, component, extras (key-value), ikon, dan tombol tes | P1 |
| F-77 | Aksi tambahan: buka layar pengaturan sistem tertentu (Wi-Fi, Bluetooth, jaringan, display, lokasi, baterai, keamanan, suara, info perangkat), tampilkan QR cepat, buka asisten/app tertentu, pencarian app | P1 (P2 untuk pencarian app) |

### F. Personalisasi & visibilitas
| ID | Fitur | Prioritas |
|---|---|---|
| F-80 | Dynamic color dari wallpaper (Android 12+), fallback palet bawaan | P0 |
| F-81 | Warna kustom (background, border 0–3 dp, shadow), shape kapsul (pill, rounded rect, squircle) | P1 |
| F-82 | Animasi: kecepatan (0.5×–2×), intensitas bounce (tanpa–tertinggi), gaya expand (organik/kaku), "reduce animations" | P1 |
| F-86 | Palet warna kustom dengan varian siang & malam (background/foreground), outline saat mode gelap, shadow saat mode terang, gradient di sekitar cutout, transparansi | P1 |
| F-87 | Haptic kapsul (buka, tutup, ganti kartu) bisa dimatikan | P0 |
| F-83 | Sembunyikan saat fullscreen (video/game), saat keyboard terbuka, saat landscape | P0 |
| F-84 | Sembunyikan di app tertentu (per-app hide list) | P1 |
| F-85 | Perilaku di lock screen: tampil / tampil tanpa isi / sembunyi | P1 |

### G. Onboarding, keandalan & app pendamping
| ID | Fitur | Prioritas |
|---|---|---|
| F-90 | Onboarding: disclosure aksesibilitas yang jelas + persetujuan eksplisit, lalu izin lain per fitur | P0 |
| F-91 | Halaman status izin: apa yang aktif, apa yang kurang, tombol untuk memperbaiki | P0 |
| F-92 | Panduan keepalive per OEM (Xiaomi/HyperOS, Samsung, Oppo/ColorOS, Vivo, Realme) | P1 |
| F-93 | Preview kapsul live di app pendamping | P0 |
| F-94 | Panel diagnostik (simulasi event) — **hanya di build debug** | P0 (debug) |
| F-95 | Backup/restore pengaturan (file JSON) | P2 |
| F-96 | Preset geometri per model device (database bawaan + kontribusi user via export JSON) | P2 |
| F-97 | Deteksi **Android Advanced Protection** (Android 17+) dan beri tahu user bahwa layanan aksesibilitas bisa dimatikan otomatis oleh sistem | P0 |
| F-98 | Tema app pendamping (ikut sistem/terang/gelap) dan bahasa app (EN, ID dulu) | P1 |

## 1.5 Di luar scope (v1)
- Mengganti status bar sistem sepenuhnya.
- Always-On Display (AOD) — butuh akses yang tidak tersedia untuk app pihak ketiga.
- Menyembunyikan heads-up notification sistem (lihat §2.5.2).
- Sinkronisasi cloud, akun, atau fitur online apa pun.
- Tablet & foldable layar dalam (boleh berjalan, tidak dioptimalkan).

## 1.6 Privasi, izin & kepatuhan Play Store

### Daftar izin
| Izin / akses | Dipakai oleh | Kapan diminta | Wajib? |
|---|---|---|---|
| Accessibility Service | Overlay kapsul, gesture global, deteksi app foreground, scroll-to-top | Onboarding (setelah disclosure) | Ya |
| Notification Listener | Media, notifikasi, progress, timer | Onboarding | Ya untuk F-10..F-42 |
| `POST_NOTIFICATIONS` | Tidak dipakai di v1 | — | — |
| `WRITE_SETTINGS` (special) | Brightness | Saat F-52 diaktifkan | Tidak |
| Notification Policy Access | Ganti ke mode senyap/DND | Saat pertama kali pilih Senyap | Tidak |
| `BLUETOOTH_CONNECT` (API 31+) | Event Bluetooth, nama perangkat | Saat F-62 diaktifkan | Tidak |
| `BLUETOOTH_SCAN` | AirPods via BLE | Saat F-67 diaktifkan | Tidak |
| `CALL_PHONE` | Telepon langsung dari pintasan kontak | Opsional; tanpa ini pakai `ACTION_DIAL` | Tidak |
| `CAMERA` | Tidak diminta — senter pakai `CameraManager.setTorchMode` (tidak butuh izin) | — | — |
| `QUERY_ADVANCED_PROTECTION_MODE` | Cek status Advanced Protection (F-97), read-only | Manifest (normal) | Ya |
| `QUERY_ALL_PACKAGES` | **Tidak dipakai.** Daftar app via `LauncherApps` | — | — |
| `INTERNET` | **Tidak dipakai** | — | — |

### Kepatuhan AccessibilityService (Google Play)
Capsool **bukan** accessibility tool, jadi:
- `android:isAccessibilityTool` **tidak** diset `true`.
- Sebelum mengarahkan ke Settings aksesibilitas, tampilkan layar **prominent disclosure** di dalam app: data apa yang diakses (jendela aktif, nama package app foreground, node scrollable untuk scroll-to-top), untuk apa, dan bahwa data tidak dikirim ke mana pun. User harus menekan tombol persetujuan eksplisit ("Setuju & lanjutkan"); tombol tolak tersedia dan app tetap bisa dibuka.
- Siapkan deklarasi di Play Console + video demo alur disclosure.
- Konfigurasi service seminimal mungkin (lihat §2.4.1).

### Data
- Isi notifikasi hanya disimpan di memori, tidak pernah ditulis ke disk atau log.
- Log debug tidak boleh mencetak isi notifikasi, nama kontak, atau nomor telepon.
- `allowBackup` hanya mencakup file pengaturan.

---

# BAGIAN 2 — TECHNICAL SPECIFICATION

## 2.1 Platform

| Item | Nilai | Alasan |
|---|---|---|
| `minSdk` | **31** (Android 12) | Dynamic color, model izin Bluetooth baru, dan `WindowMetrics` selalu tersedia; pangsa Android < 12 di segmen pengguna kustomisasi sudah kecil (aplikasi sejenis memakai 32) |
| `targetSdk` / `compileSdk` | 37 | Mengikuti toolchain saat ini |
| Bahasa / UI | Kotlin, Jetpack Compose, Material 3 (Expressive) | — |
| Persistence | Jetpack DataStore (Preferences) | Async, aman dipakai dari service & activity dalam satu proses |
| Async | Coroutines + Flow | — |
| DI | Manual (object `AppGraph` di `Application`) | Proyek kecil; Hilt bisa ditambah nanti |
| Proses | Satu proses (default) | Service aksesibilitas, notification listener, dan activity berbagi singleton engine |

### API yang di-gate per versi
| API | Min | Fallback |
|---|---|---|
| `CameraManager.turnOnTorchWithStrengthLevel` | 33 | Senter on/off saja, slider disembunyikan |
| Monochrome icon (`AdaptiveIconDrawable.monochrome`) | 33 | Ikon asli yang di-tint |
| `Notification.ProgressStyle` | 36 | Baca `EXTRA_PROGRESS`/`EXTRA_PROGRESS_MAX` |
| `AdvancedProtectionManager` (butuh `QUERY_ADVANCED_PROTECTION_MODE`) | 36/37 | Lewati pengecekan |

## 2.2 Arsitektur

```
 ┌──────────────────────── Sistem Android ─────────────────────────┐
 │ NotificationListener  BroadcastReceivers  MediaSession  Camera  │
 │ AudioManager  Settings.System  Connectivity  AccessibilityEvents│
 └──────┬──────────────────────┬────────────────────────▲──────────┘
        │ (1) observe          │                        │ (4) execute
 ┌──────▼──────────────────────▼──────┐      ┌──────────┴──────────┐
 │         Sources (per domain)        │      │   EffectHandler     │
 │ MediaSource, NotificationSource,    │      │ MediaController,    │
 │ BatterySource, SystemEventSource,   │      │ AudioController,    │
 │ ProgressSource, TimerSource ...     │      │ TorchController,    │
 └──────┬──────────────────────────────┘      │ GlobalActions ...   │
        │ (2) SystemEvent                     └──────────▲──────────┘
 ┌──────▼──────────────────────────────────────────────┐ │ (3) Effect
 │ CapsuleStore: state = reduce(state, action)          ├─┘
 │   reducer pure → (newState, List<Effect>)            │
 └──────┬──────────────────────────────────▲───────────┘
        │ StateFlow<CapsuleState>          │ UserIntent
 ┌──────▼──────────────────────────────────┴───────────┐
 │ Overlay UI (Compose di TYPE_ACCESSIBILITY_OVERLAY)   │
 │ + Companion app (MainActivity)                        │
 └──────────────────────────────────────────────────────┘
```

Aturan:
1. **Reducer tetap pure** dan bisa di-unit-test. Reducer mengembalikan `Reduction(state, effects)`; tidak pernah memanggil API Android.
2. **State dari sistem adalah sumber kebenaran.** Contoh: tekan play → reducer mengeluarkan `Effect.Media.PlayPause` → `EffectHandler` memanggil `transportControls.play()` → `MediaSource` menerima `onPlaybackStateChanged` → `SystemEvent.MediaUpdated` → state berubah. Boleh ada *optimistic update* untuk UI, tapi wajib dikoreksi oleh event sistem.
3. **Source hidup sesuai fitur.** Source hanya register listener/receiver kalau fiturnya aktif di Settings, dan unregister saat dimatikan.
4. **Tidak ada data simulasi di build release.** Semua simulator dipindah ke source set `debug`.

### Struktur package
```
com.iqbalwork.capsule
├── CapsoolApp.kt                 // Application, membuat AppGraph
├── AppGraph.kt                   // wiring singleton: store, settings, sources, effect handler
├── core/
│   ├── store/  CapsuleStore, CapsuleState, Action (UserIntent | SystemEvent), Effect, Reducer
│   ├── settings/  SettingsRepository (DataStore), model Settings
│   └── stack/  CardStackManager (dipakai reducer)
├── platform/
│   ├── accessibility/  CapsoolAccessibilityService, OverlayWindowController, ForegroundAppTracker, ScrollToTop
│   ├── notification/   CapsoolNotificationListener, NotificationParser, MessagingParser
│   ├── media/          MediaSource, MediaEffects
│   ├── battery/        BatterySource
│   ├── system/         SystemEventSource (ringer, wifi, bt, airplane, unlock, usb)
│   ├── sliders/        AudioController, BrightnessController, TorchController
│   ├── progress/       ProgressParser, TimerParser
│   ├── shortcuts/      AppShortcutRepository, ContactShortcutRepository
│   └── widgets/        CapsoolWidgetHost
├── ui/
│   ├── overlay/    CapsuleRootView, cards/*, mini/*, gestures/*
│   ├── companion/  onboarding/*, settings/*, preview/*
│   └── theme/      CapsuleTokens, Theme (dynamic color)
└── debug/ (source set debug)  DiagnosticsPanel, FakeEvents
```

### Kontrak Action & Effect (ringkas)
```kotlin
sealed interface Action
sealed interface UserIntent : Action {
    data class Gesture(val type: GestureType) : UserIntent        // TAP, DOUBLE_TAP, LONG_PRESS, SWIPE_LEFT, SWIPE_RIGHT, SWIPE_UP
    data object Collapse : UserIntent
    data class MediaCommand(val cmd: MediaCmd) : UserIntent       // PLAY_PAUSE, NEXT, PREV, SeekTo(ms)
    data class SetVolume(val fraction: Float) : UserIntent
    data object CycleRinger : UserIntent
    data class SetBrightness(val fraction: Float) : UserIntent
    data class SetTorch(val on: Boolean, val level: Float? = null) : UserIntent
    data class NotificationReply(val key: String, val text: String) : UserIntent
    data class NotificationAction(val key: String, val action: NotifActionKind) : UserIntent // MARK_READ, DISMISS
    data class TimerAction(val key: String, val actionIndex: Int) : UserIntent
    data class LaunchShortcut(val id: String) : UserIntent
}
sealed interface SystemEvent : Action {
    data class MediaUpdated(val media: MediaInfo?) : SystemEvent
    data class NotificationPosted(val n: CapsuleNotification) : SystemEvent
    data class NotificationRemoved(val key: String) : SystemEvent
    data class BatteryUpdated(val battery: BatteryInfo) : SystemEvent
    data class SlidersUpdated(val sliders: SliderInfo) : SystemEvent
    data class ProgressUpdated(val items: List<ProgressItem>) : SystemEvent
    data class TimerUpdated(val timer: TimerInfo?) : SystemEvent
    data class MiniEvent(val event: MiniEventInfo) : SystemEvent
    data class ForegroundAppChanged(val pkg: String) : SystemEvent
    data class VisibilityContextChanged(val ctx: VisibilityContext) : SystemEvent // fullscreen, ime, landscape, locked
    data class SettingsChanged(val settings: Settings) : SystemEvent
}
sealed interface Effect {
    sealed interface Media : Effect      // PlayPause, Next, Prev, SeekTo
    sealed interface Notif : Effect      // Reply, MarkRead, Dismiss, FireAction
    data class Volume(val fraction: Float) : Effect
    data class Ringer(val mode: RingerMode) : Effect
    data class Brightness(val fraction: Float) : Effect
    data class Torch(val on: Boolean, val level: Float?) : Effect
    data class Global(val action: GlobalAction) : Effect // NOTIFICATIONS, QUICK_SETTINGS, LOCK, SCREENSHOT, SCROLL_TO_TOP
    data class Launch(val target: LaunchTarget) : Effect
    data class Broadcast(val name: String, val extras: Map<String, String>) : Effect
    data object Haptic : Effect
}
```

## 2.3 Model state kapsul

```kotlin
data class CapsuleState(
    val mode: CapsuleMode,              // HIDDEN, IDLE, MINI_EVENT, COMPACT, EXPANDED
    val cards: List<ActiveCard>,        // hanya kartu yang punya aktivitas nyata, urut prioritas
    val focusedCardId: String?,
    val miniEvent: MiniEventInfo?,
    val media: MediaInfo?,
    val notifications: List<CapsuleNotification>, // maks 5 terbaru
    val battery: BatteryInfo,
    val sliders: SliderInfo,
    val progress: List<ProgressItem>,
    val timer: TimerInfo?,
    val visibility: VisibilityContext,
    val settings: Settings,
)
```

**Perubahan penting dari kode sekarang:** carousel tidak lagi berisi semua 6 jenis kartu secara tetap. `cards` diturunkan dari aktivitas nyata (ada media aktif → ada kartu media; ada timer berjalan → ada kartu timer). Kartu pintasan/slider adalah kartu "on demand" yang muncul lewat gesture (mis. long press), bukan selalu di carousel.

### Transisi mode
| Dari | Pemicu | Ke |
|---|---|---|
| HIDDEN | Ada aktivitas, atau "selalu tampil" aktif, dan visibilitas mengizinkan | IDLE / COMPACT |
| IDLE | Mini event masuk | MINI_EVENT (durasi `settings.miniEventDurationMs`) → kembali |
| IDLE/COMPACT | Tap (aksi default) | EXPANDED |
| EXPANDED | Tap di luar kapsul, swipe up, timeout 8 s tanpa interaksi | COMPACT / IDLE |
| apa pun | Fullscreen / keyboard / landscape / app di hide list (sesuai setting) | HIDDEN |

Prioritas jika beberapa hal terjadi bersamaan: notifikasi baru > mini event > timer selesai > media > progress > charging.

## 2.4 Overlay window

### 2.4.1 Konfigurasi AccessibilityService
```xml
<accessibility-service
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowsChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:canPerformGestures="false"
    android:notificationTimeout="100"
    android:description="@string/accessibility_service_description" />
```
- Hapus `typeAllMask` dan `FLAG_REQUEST_FILTER_KEY_EVENTS` yang sekarang dipasang di kode — tidak dibutuhkan dan memperbesar beban + risiko review.
- `canRetrieveWindowContent` **wajib** untuk deteksi keyboard: tanpa capability ini `flagRetrieveInteractiveWindows` diabaikan dan `getWindows()` selalu kosong (terverifikasi di emulator API 37). Juga dipakai scroll-to-top (F-72). Jelaskan di disclosure.
- Jangan override `serviceInfo` di `onServiceConnected` dengan nilai berbeda dari XML.
- Overlay dipasang langsung di `onServiceConnected` tanpa cek `canDrawOverlays` (benar seperti sekarang).

### 2.4.2 Ukuran window & sentuhan
Masalah: window `WRAP_CONTENT` yang ikut membesar saat expand akan menghalangi sentuhan ke app di bawahnya, dan relayout window setiap frame animasi itu mahal.

Solusi (diperbarui setelah tes animasi): **Android 14+** — window tetap seukuran kartu dan hanya *touchable region*-nya yang berubah (`AttachedSurfaceControl.setTouchableRegion`: pill saat collapsed, kartu saat expanded, kosong saat hidden). Window tidak pernah di-resize, karena resize menampilkan satu frame konten lama di frame baru sehingga pill "loncat" ke samping saat collapse (terlihat di rekaman layar emulator). Konten yang keluar tetap dirender dengan data terakhirnya dan fade-out selama kapsul menyusut; collapse memakai spring lebih lembut (damping 0.8, stiffness 260). **Android 12–13** (fallback) — `OverlayWindowController` mengelola **dua ukuran window**:
1. **Idle/Compact:** window seukuran bounding box kapsul + margin 8 dp. Sentuhan di luar box langsung tembus ke app.
2. **Expanded:** sebelum animasi expand dimulai, window dibesarkan sekali ke ukuran maksimal kartu. Setelah animasi collapse selesai (750 ms × skala animasi sistem), window dikecilkan lagi. Window di-anchor ke tengah horizontal. Tidak ada `updateViewLayout` per frame.

Flag: `FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_NO_LIMITS | FLAG_NOT_TOUCH_MODAL | FLAG_WATCH_OUTSIDE_TOUCH`. `MotionEvent.ACTION_OUTSIDE` → `UserIntent.Collapse`.

**Mode HIDDEN:** set root view `INVISIBLE` + `FLAG_NOT_TOUCHABLE`. Komposisi kosong saja tidak cukup — window tetap menampilkan frame terakhir karena tidak ada redraw (terverifikasi di emulator).

Inline reply butuh keyboard: saat field balasan difokuskan, hapus `FLAG_NOT_FOCUSABLE` sementara dan set `softInputMode = ADJUST_NOTHING`; kembalikan saat reply dikirim/dibatalkan.

`layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` agar window bisa menutupi area cutout.

### 2.4.3 Posisi & cutout
1. Ambil `DisplayCutout` dari `windowManager.currentWindowMetrics.windowInsets.displayCutout`. Pakai batas `cutoutPath` (`Path.computeBounds`) bila ada, fallback ke `boundingRectTop`. Alasannya: bounding rect menempel ke tepi layar (di emulator Pixel: y 0–156 px padahal lubang di y 53–151), jadi pusat vertikalnya meleset.
2. Default: kapsul berpusat horizontal di tengah rect itu; tinggi = max(tinggi rect + 8 dp, 32 dp); y = rect.top − 4 dp.
3. Tanpa cutout (atau cutout di samping): default tengah atas layar, di dalam status bar.
4. Kalibrasi user (F-02) disimpan sebagai **offset relatif** terhadap default, dalam **profil layar** dengan kunci `(lebar px, tinggi px, orientasi)` (F-08). Profil baru dibuat otomatis dari default saat kombinasi baru terdeteksi.
5. Rotasi: dengarkan `onConfigurationChanged` di service; di landscape default HIDDEN (F-83), opsi tampil di sisi cutout.
6. **Zona mati di tepi atas:** sistem memprioritaskan swipe-turun dari tepi atas layar untuk membuka notification shade, jadi gesture di beberapa piksel teratas kartu tidak andal. Mode "menyatu dengan kamera" (F-09) menerima keterbatasan ini (konten interaktif kartu diberi padding atas ≥ tinggi status bar); mode "mandiri" menempatkan kapsul di bawah status bar sehingga semua gesture normal.

### 2.4.4 Konteks visibilitas
| Kondisi | Cara deteksi |
|---|---|
| App foreground | `TYPE_WINDOW_STATE_CHANGED` → `event.packageName` (abaikan package sistem UI & launcher overlay sesaat) |
| Keyboard terbuka | `windows` berisi `AccessibilityWindowInfo.TYPE_INPUT_METHOD` |
| Fullscreen | Root overlay menerima `WindowInsets`; jika `statusBars()` tidak visible → fullscreen. **Validasi di device** (perilaku insets untuk overlay aksesibilitas bisa beda per OEM); fallback: cek window aplikasi teratas menutupi seluruh layar via `AccessibilityWindowInfo.getBoundsInScreen` |
| Lock screen | `KeyguardManager.isKeyguardLocked` + receiver `ACTION_SCREEN_OFF` / `ACTION_USER_PRESENT` |
| Layar mati | `ACTION_SCREEN_OFF` → hentikan semua animasi & pause Source yang polling |

## 2.5 Spesifikasi per fitur

### 2.5.1 Media (F-10..F-13)
- **Sumber:** `MediaSessionManager.getActiveSessions(ComponentName(ctx, CapsoolNotificationListener::class.java))` + `addOnActiveSessionsChangedListener`. Butuh notification listener aktif.
- **Pilih sesi:** sesi pertama dengan `PlaybackState.STATE_PLAYING`; jika tidak ada, sesi terakhir yang playing dalam 30 detik terakhir (untuk kondisi pause). Filter whitelist F-13.
- **Metadata:** `METADATA_KEY_TITLE`, `ARTIST` (fallback `DISPLAY_SUBTITLE`), `DURATION`. Cover: `ART` → `ALBUM_ART` → `DISPLAY_ICON` → `ART_URI`/`ALBUM_ART_URI` (load via `ContentResolver`) → large icon notifikasi media dari package yang sama → ikon app.
- **Posisi:** `position + (SystemClock.elapsedRealtime() - lastPositionUpdateTime) * playbackSpeed`. Ticker 1 Hz hanya saat kartu terlihat dan sedang playing. Seek bar hanya aktif jika `actions` mengandung `ACTION_SEEK_TO`; jika `DURATION` ≤ 0 (umum pada beberapa player/streaming), sembunyikan seek bar dan tampilkan judul saja.
- **Kontrol:** `MediaController.transportControls.play/pause/skipToNext/skipToPrevious/seekTo`.
- **Warna:** `androidx.palette` dari cover (ukuran diperkecil ke 64×64 di background thread), ambil `vibrant`/`dominant` swatch, lalu harmonisasi ke skema M3 (`MaterialKolor` atau hitung tone sendiri) agar kontras teks ≥ 4.5:1.
- **Auto-hide (F-12):** timeout setelah pause (pilihan: instan, 5s, 15s, 30s, 60s, 2m, tidak pernah); sembunyikan saat `ForegroundAppTracker` = package sesi media.
- **Mini indicator (F-11):** cover berputar (rotasi 8 s/putaran) dan visualizer sintetis (bukan dari audio — `Visualizer` API butuh `RECORD_AUDIO`, tidak dipakai). Animasi berhenti saat pause/hidden/layar mati.
- **Edge case:** banyak sesi bersamaan, sesi tanpa metadata, cover bitmap besar (downscale + recycle), ganti lagu cepat (debounce 150 ms).

### 2.5.2 Notifikasi (F-20..F-23)
- **Sumber:** `CapsoolNotificationListener.onNotificationPosted/Removed`. Rebind otomatis via `requestRebind` saat `onListenerDisconnected`.
- **Filter (urut):** abaikan jika package = Capsool; `FLAG_GROUP_SUMMARY`; `isOngoing` (kecuali diproses oleh Progress/Timer parser); importance < `DEFAULT` (via `RankingMap`); kategori `CATEGORY_TRANSPORT`/`SERVICE`/`SYSTEM`; package di blacklist (default kosong; whitelist mode opsional); update notifikasi yang sama tanpa teks baru (`FLAG_ONLY_ALERT_ONCE`).
- **Parsing:** `MessagingStyle` (`Notification.MessagingStyle.extractMessagingStyleFromNotification` via `NotificationCompat`) → pesan terakhir + `Person` (nama, ikon). Fallback `EXTRA_TITLE`, `EXTRA_TEXT`/`EXTRA_BIG_TEXT`, `largeIcon`.
- **Glow & badge:** warna dari `notification.color` jika bukan default; jika tidak, Palette dari ikon app (cache per package). Badge = ikon app kecil di pojok avatar. Tidak ada warna hardcode per brand.
- **Reply:** cari `Action` dengan `RemoteInput` yang `allowFreeFormInput`. Kirim: `RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)` lalu `action.actionIntent.send(ctx, 0, intent)`. Jika tidak ada RemoteInput → tombol Reply disembunyikan.
- **Mark as read:** action dengan `semanticAction == SEMANTIC_ACTION_MARK_AS_READ`; jika tidak ada, tombol disembunyikan.
- **Dismiss:** `cancelNotification(sbn.key)` (hanya jika notifikasi clearable).
- **Heads-up sistem:** tidak disembunyikan secara programatik. Solusinya F-26: halaman "Atur app" berisi daftar app yang notifikasinya diteruskan ke kapsul; tap satu app membuka `Settings.ACTION_APP_NOTIFICATION_SETTINGS` (`EXTRA_APP_PACKAGE`) agar user mematikan pop-up untuk app itu.
- **Hapus setelah dilihat (F-25):** setelah notifikasi ditampilkan expanded ≥ 1 s atau dibalas, panggil `cancelNotification(key)` (atau seluruh grup via `groupKey`) jika opsi aktif.
- **Manifest listener:** meta-data `android.service.notification.default_filter_types = conversations|alerting|ongoing|silent` agar semua tipe yang dibutuhkan (termasuk ongoing untuk progress/timer) diteruskan.
- **Lock screen privacy (F-22):** jika terkunci dan `visibility != VISIBILITY_PUBLIC` atau setting user "sembunyikan isi" → tampilkan nama app saja.
- **Mode & durasi (F-24):** ringkas (expand saat tap) atau langsung expanded; auto-hide 1/3/5/10/20/30 s atau "sampai dibuka", lalu kembali ke kartu sebelumnya; notifikasi tetap ada di antrian maks 5 sampai di-dismiss atau dihapus dari shade.

### 2.5.3 Baterai & charging (F-30, F-31, F-60)
- **Sumber:** receiver dinamis (didaftarkan di service) untuk `ACTION_BATTERY_CHANGED` (sticky), `ACTION_POWER_CONNECTED`, `ACTION_POWER_DISCONNECTED`.
- **Data:** level/scale → persen; `EXTRA_PLUGGED` → AC/USB/WIRELESS/DOCK; `EXTRA_STATUS == FULL`; `EXTRA_TEMPERATURE / 10f` °C; estimasi: `BatteryManager.computeChargeTimeRemaining()` (−1 → sembunyikan).
- **Mini event:** saat colok — ikon tipe charger + persen + animasi fill; saat cabut — persen saja.
- **Low battery:** sekali per siklus discharge saat melewati ambang (default 15%) dan lagi di 5%. **Full:** sekali saat status FULL sambil tercolok.
- **Edge case:** `ACTION_BATTERY_CHANGED` sering datang (tiap perubahan suhu/voltase) → hanya emit jika field yang dipakai berubah.

### 2.5.4 Event sistem (F-61..F-67)
| Event | Sumber | Catatan |
|---|---|---|
| Ringer | `AudioManager.RINGER_MODE_CHANGED_ACTION` | Abaikan jika perubahan berasal dari Capsool sendiri < 500 ms lalu (cegah double event) |
| Bluetooth audio | `BluetoothDevice.ACTION_ACL_CONNECTED/DISCONNECTED` + filter `BluetoothClass.Device.Major.AUDIO_VIDEO` | Butuh `BLUETOOTH_CONNECT` (31+). Baterai: broadcast tersembunyi `android.bluetooth.device.action.BATTERY_LEVEL_CHANGED` (extra `android.bluetooth.device.extra.BATTERY_LEVEL`) — **best effort**, tampilkan tanpa persen jika tidak ada |
| Wi-Fi | `ConnectivityManager.registerNetworkCallback` dengan `TRANSPORT_WIFI` | SSID tidak ditampilkan (butuh izin lokasi) — cukup "Wi-Fi tersambung" |
| Pesawat | `Intent.ACTION_AIRPLANE_MODE_CHANGED` | — |
| Unlock | `Intent.ACTION_USER_PRESENT` | Animasi sapaan 1,5 s, bisa dimatikan |
| USB/OTG storage | `StorageManager.registerStorageVolumeCallback` (30+), cek `isRemovable` | P2; validasi di device |
| AirPods | `BluetoothLeScanner` filter manufacturer ID `0x004C`, decode paket proximity | P2, eksperimental; scan hanya beberapa detik setelah ACL connected agar hemat baterai |

### 2.5.5 Slider (F-50..F-53)
- **Volume:** `AudioManager.setStreamVolume(STREAM_MUSIC, idx, 0)`; tampilan nilai dari `getStreamVolume/getStreamMaxVolume`. Observasi perubahan eksternal: `ContentObserver` pada `Settings.System.CONTENT_URI` (beberapa OEM tidak memicu; fallback re-read saat kartu dibuka).
- **Ringer:** `AudioManager.setRingerMode`. Pindah ke/dari SILENT saat DND butuh `NotificationManager.isNotificationPolicyAccessGranted`; jika belum, buka `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` dan lewati Senyap dalam siklus.
- **Gaya volume (F-51):** 3 komposabel visual, logika sama.
- **Brightness:** tulis `Settings.System.SCREEN_BRIGHTNESS` (butuh `Settings.System.canWrite`; jika belum → `ACTION_MANAGE_WRITE_SETTINGS`). Android memakai kurva gamma untuk slider, jadi posisi slider dipetakan ke nilai linear lewat fungsi gamma (port dari `BrightnessUtils` AOSP). **Kalibrasi maksimum:** banyak OEM memakai max ≠ 255 (mis. 1023/2047/4095). Wizard meminta user menggeser brightness sistem ke penuh, lalu membaca nilai `SCREEN_BRIGHTNESS` sebagai max; disimpan di settings. Adaptive: `SCREEN_BRIGHTNESS_MODE`.
- **Senter:** `CameraManager.setTorchMode(cameraId, on)`; pilih kamera dengan `FLASH_INFO_AVAILABLE` dan facing back. Status dari `registerTorchCallback`. Strength (API 33+): jika `FLASH_INFO_STRENGTH_MAXIMUM_LEVEL > 1` → `turnOnTorchWithStrengthLevel(id, level)`; jika tidak, slider disembunyikan. Tangani `CameraAccessException` saat kamera dipakai app lain.

### 2.5.6 Progress & timer (F-40..F-42)
- **Sumber:** notifikasi ongoing dari `CapsoolNotificationListener` yang diteruskan ke parser.
- **Progress:** notifikasi dengan `EXTRA_PROGRESS_MAX > 0` atau `EXTRA_PROGRESS_INDETERMINATE`, atau `ProgressStyle` (API 36). Data: judul, persen, app. Tidak ada daftar package hardcode — semua app yang memakai progress standar ikut; whitelist/blacklist mengikuti F-22. Selesai: notifikasi dihapus atau progress = max → mini event "Selesai" 2 s.
- **Timer:** notifikasi dengan `EXTRA_SHOW_CHRONOMETER == true`; countdown jika `EXTRA_CHRONOMETER_COUNT_DOWN == true`. Waktu target = `notification.when`. Render sendiri tiap detik (hanya saat terlihat). Tombol = action notifikasi asli (label & `PendingIntent` diteruskan apa adanya), jadi +1 menit/pause/stop bekerja sesuai app jamnya. Google Clock (`com.google.android.deskclock`) dites pertama; parser harus toleran (catatan: format notifikasi app jam berubah antar versi Android — tambahkan test fixture per versi).
- **Edge case:** timer di-pause → `when` tidak bergerak / chronometer off → tampilkan sisa waktu statis dari teks notifikasi.

### 2.5.7 Pintasan (F-54..F-57)
- **App:** daftar dari `LauncherApps.getActivityList(null, user)` (tidak butuh `QUERY_ALL_PACKAGES`). Ikon themed: layer monochrome (33+) di-tint `primary`; fallback ikon asli. Launch: `LauncherApps.startMainActivity`.
- **Kontak:** pilih via `Intent.ACTION_PICK` (`ContactsContract.CommonDataKinds.Phone.CONTENT_URI`) — tidak butuh `READ_CONTACTS`; simpan nama + nomor + foto (dikecilkan) saat dipilih. Telepon: `ACTION_DIAL` (default) atau `ACTION_CALL` jika `CALL_PHONE` diberikan. WhatsApp: `https://wa.me/<nomor E.164>`.
- **Sistem:** intent `Settings.ACTION_WIFI_SETTINGS`, `ACTION_BLUETOOTH_SETTINGS`, `ACTION_DISPLAY_SETTINGS`, `ACTION_LOCATION_SOURCE_SETTINGS`, `Intent.ACTION_POWER_USAGE_SUMMARY`; panel cepat (29+): `Settings.Panel.ACTION_WIFI`, `ACTION_INTERNET_CONNECTIVITY`.
- **Barcode (F-57):** isi bisa **diketik** langsung atau **di-scan** dengan ML Kit Code Scanner (`play-services-code-scanner`, UI scanner dari Play services, tidak butuh izin kamera) / ML Kit Barcode Scanning dari gambar galeri. **Render** dengan ZXing core (`com.google.zxing:core`) — ML Kit tidak bisa membuat barcode. Format: QR, EAN-13, EAN-8, UPC-A, UPC-E, Code 128, Code 39, Code 93, Codabar, ITF. Validasi isi per format (checksum EAN/UPC, panjang ITF genap) dengan pesan error yang jelas. Saat kartu barcode tampil: naikkan brightness window overlay (`screenBrightness = 1f` pada LayoutParams) sementara.
- **Launch dari overlay:** semua `startActivity` dari service memakai `FLAG_ACTIVITY_NEW_TASK`, lalu collapse kapsul. **Validasi** background activity launch di Android 15+ (accessibility service yang aktif biasanya dikecualikan, tapi wajib dites).

### 2.5.8 Widget (F-58, P2)
- `AppWidgetHost(ctx, HOST_ID)` dimiliki service, `startListening` saat service connected, `stopListening` saat destroy.
- Tambah widget dari app pendamping: `allocateAppWidgetId` → `bindAppWidgetIdIfAllowed`; jika false → activity `ACTION_APPWIDGET_BIND`; lalu configure activity jika ada.
- Render di Compose via `AndroidView { host.createView(ctx, id, info) }` dengan ukuran tetap (lebar kartu × maks 180 dp), panggil `updateAppWidgetSize`.
- Risiko: klik `PendingIntent` widget dari window overlay — validasi di device.

### 2.5.9 Gesture & aksi (F-70..F-74)
- **Deteksi** (sudah ada di `capsule5WayGestures`, perlu diperbaiki): tap, double tap (300 ms), long press (450 ms), swipe horizontal (> 48 dp, |dx| > 1.2 |dy|), swipe up (collapse). Bug saat ini: drag yang melebihi slop long-press tapi kurang dari ambang swipe akan dianggap tap saat dilepas — harus dibatalkan.
- **Ketika ada double tap terpasang**, tap tunggal tertunda 300 ms. Jika double tap dipetakan ke "tidak ada", tap dieksekusi seketika.
- **Dua konteks (F-75):** setiap gesture punya dua pemetaan — `whenActive` (ada kartu dinamis) dan `whenIdle` (tidak ada). Pemetaan `whenIdle` default-nya membuka kartu statis.
- **Intent builder (F-76):** model `CustomIntent(type: ACTIVITY|SERVICE|BROADCAST, action, component?, extras: Map<String,String>, icon)`. Activity dijalankan dengan `FLAG_ACTIVITY_NEW_TASK`; Service dengan `startForegroundService` hanya jika target mengizinkan, tangani `SecurityException`/`IllegalStateException` dengan pesan ke user. Tombol "Kirim tes" di app pendamping.
- **Pemetaan default:** Tap → expand/collapse; Double tap → play/pause; Long press → kartu pintasan; Swipe kiri/kanan → kartu berikut/sebelumnya; Swipe up → collapse.
- **Aksi global:** `performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS | QUICK_SETTINGS | LOCK_SCREEN | TAKE_SCREENSHOT)`. Untuk screenshot, sembunyikan kapsul dulu 150 ms agar tidak ikut terfoto.
- **Scroll-to-top (F-72):** dari `rootInActiveWindow`, cari node pertama yang `isScrollable` dan terlihat; ulangi `ACTION_SCROLL_BACKWARD` sampai return false (maks 30 kali) atau gunakan `ACTION_SCROLL_TO_POSITION` dengan row 0 jika `collectionInfo` tersedia. `recycle` node tidak perlu (API 33+ deprecated) tapi hindari menahan referensi.
- **Broadcast keluar (F-73):** action `com.iqbalwork.capsule.action.EVENT`, extras: `type` (`gesture`/`card_shown`/`mini_event`), `name` (mis. `double_tap`), `card` (`media`/`notification`/...). Bisa dimatikan; default mati.
- **Intent masuk (F-74, P2):** `com.iqbalwork.capsule.action.SHOW_EVENT` dengan `title`, `text`, `icon` (nama material symbol), `durationMs`; dilindungi opsi "izinkan app otomasi" di settings.

## 2.6 Pengaturan (DataStore)

Satu `SettingsRepository` yang mengekspos `Flow<Settings>`; service dan activity membaca dari flow yang sama, jadi perubahan di app pendamping langsung terlihat di overlay. Kunci utama:

```
geometry.{portrait,landscape}.{offsetX,offsetY,width,height,corner}
geometry.screenCornerRadius
appearance.{colorSource: DYNAMIC|CUSTOM, customBg, border, shadow, shape, animationStyle, reduceMotion, idleOpacity, alwaysShowPill}
cards.{media,notification,charging,progress,timer}.enabled
media.{autoHide, hideWhenAppForeground, appWhitelist}
notification.{mode: BLACKLIST|WHITELIST, packages, hideContentWhenLocked, displaySeconds}
events.{charging,ringer,bluetooth,wifi,airplane,unlock,usb,airpods}.enabled, events.durationMs
battery.lowThreshold
sliders.{volumeStyle, brightnessMax}
gestures.{tap,doubleTap,longPress,swipeLeft,swipeRight}.{whenActive,whenIdle}
staticCards: List<StaticCard>   // urutan, tipe, konfigurasi per kartu, startCardId, viewMode: CAROUSEL|LIST
profiles: Map<ProfileKey(widthPx,heightPx,orientation), Geometry>
capsule.{mode: CAMERA|INDEPENDENT, widthMode: CONTENT|FULL, cardRadius?, miniScale, miniScaleAnchor, miniAlignment, fadeWhenNoCutout}
animation.{speed, bounce, expandStyle, reduce}
palettes: List<Palette(day, night)>, appearance.{outlineInDark, shadowInLight, cutoutGradient}
haptics.enabled
visibility.{hideFullscreen, hideIme, hideLandscape, lockScreen: SHOW|SHOW_PRIVATE|HIDE, hiddenApps}
automation.{broadcastOut, acceptIncoming}
onboarding.{disclosureAccepted, completedAt}
```
Migrasi dari `CutoutPreferencesRepository` (SharedPreferences `capsool_cutout_prefs`) ke DataStore lewat `SharedPreferencesMigration`.

## 2.7 Desain & token

- **Skema warna:** Android 12+ → `dynamicDarkColorScheme(ctx)` (kapsul selalu gelap agar menyatu dengan kamera). Di bawah 12 atau jika user pilih CUSTOM → palet bawaan di bawah. Warna kartu media/notifikasi diturunkan dari cover/ikon (§2.5.1, §2.5.2).
- **Palet bawaan (fallback):** pertahankan token warm brown dari v2 (`Surface 0xFF1A1615`, `Primary 0xFFFFB690`, dst.). Hapus `WhatsAppBadge`/`TelegramBadge` — badge pakai ikon & warna app asli.
- **Kalibrasi:** `CalibratorRed = 0xFFFF3B30` tetap.
- **Motion:** `FruitSpring` (damping 0.72, stiffness 380) untuk perubahan ukuran kapsul; `M3ElasticMotion` (0.62, 520) untuk konten; gaya "minimal" = `tween(200)`; `reduceMotion` atau setting sistem `ANIMATOR_DURATION_SCALE == 0` → tanpa animasi bounce.
- **Tipografi:** Google Sans Flex jika tersedia via font sistem, fallback `FontFamily.Default`. Angka (persen, timer) pakai `fontFeatureSettings = "tnum"`.

## 2.8 Performa & stabilitas
- Tidak ada `infiniteTransition` aktif kecuali (media playing **dan** kapsul terlihat **dan** layar nyala).
- Bitmap cover/avatar: downscale ke ukuran tampil × density, cache LRU 8 MB.
- Parser notifikasi berjalan di `Dispatchers.Default`, bukan di thread binder listener.
- Semua `BroadcastReceiver` dinamis didaftarkan dengan `RECEIVER_NOT_EXPORTED` kecuali broadcast sistem yang membutuhkan `RECEIVER_EXPORTED`.
- `CapsoolAccessibilityService.onDestroy` wajib melepas window, receiver, callback kamera, listener sesi media.
- StrictMode aktif di build debug.

## 2.9 Strategi test
| Lapisan | Alat | Cakupan |
|---|---|---|
| Reducer & state machine | JUnit | Semua transisi mode §2.3, prioritas kartu, efek yang dikeluarkan |
| Parser notifikasi/progress/timer | Robolectric + fixture `Notification` yang dibangun dengan `NotificationCompat.Builder` | Messaging, reply, progress, chronometer, edge case |
| Brightness gamma mapping | JUnit | Nilai ujung & monoton |
| UI kartu | Compose screenshot test (Roborazzi) | Setiap kartu terang/gelap, teks panjang, RTL |
| Integrasi | Checklist manual di device (Bagian 3) | Semua acceptance criteria |

### Matriks device minimum
| Device | Android | Kenapa |
|---|---|---|
| Pixel (fisik atau emulator dengan cutout) | 16 & 17 | Referensi AOSP, Google Clock |
| Samsung Galaxy (One UI 7/8) | 15/16 | OEM terbesar, perilaku volume & keepalive berbeda |
| Xiaomi / POCO (HyperOS) | 14/15 | Killer agresif |
| Emulator API 31 | 12 | Batas minSdk |

---

# BAGIAN 3 — EXECUTION PLAN

## 3.1 Baseline (kondisi kode per commit `24b798d`, sebelum M0)

| Area | Status | Catatan |
|---|---|---|
| Overlay `TYPE_ACCESSIBILITY_OVERLAY` + lifecycle owner Compose | ✅ Ada | Perlu `OverlayWindowController` (§2.4.2) dan config minimal (§2.4.1) |
| Reducer + `CardStackManager` + unit test | ✅ Ada | Belum mengeluarkan effect; carousel berisi 6 kartu tetap |
| Gesture 5 arah | ✅ Ada | Bug drag→tap (§2.5.9) |
| Tampilan semua kartu & mini capsule | ✅ Ada (UI) | Semua data dari state default/simulasi |
| App pendamping 3 tab + panel simulasi | ✅ Ada | Simulasi harus pindah ke `debug` |
| `CutoutPreferencesRepository` | ⚠️ Ada, tidak dipakai | Diganti `SettingsRepository` + migrasi |
| Notification listener, MediaSession, receiver, kontrol audio/brightness/senter, ML Kit, widget | ❌ Belum ada | — |
| Permission di manifest, onboarding, disclosure | ❌ Belum ada | — |
| `com/iqbalf/capsule/ui/CapsuleRootView.kt` | 🗑 Hapus | Facade hasil salah path di prompt v2 |

## 3.2 Definition of Done (berlaku untuk setiap milestone)
1. Semua acceptance criteria milestone dicentang **setelah dicoba di HP fisik** (minimal 1 device dari matriks §2.9), dengan catatan device + versi Android di PR/commit.
2. Tidak ada data hardcode/simulasi di source set `main` untuk fitur milestone itu.
3. Unit test reducer & parser untuk fitur baru ada dan lulus; `./gradlew testDebugUnitTest assembleDebug lintDebug` lulus.
4. Fitur bisa dimatikan dari Settings dan saat mati tidak ada receiver/listener yang terdaftar.
5. Checklist di dokumen ini diperbarui jujur (✅ / ⚠️ sebagian + alasan / ❌).
6. Pesan commit menjelaskan apa yang benar-benar dikerjakan; tidak mengklaim melebihi itu.

## 3.3 Milestone

**MVP (rilis pertama) = M0–M4**, yang mencakup semua fitur P0. M5–M8 menyelesaikan P1 dan persiapan rilis publik.

### M0 — Fondasi yang bisa dipasang (F-01..F-05 dasar, F-80, F-83, F-87, F-90, F-91, F-93, F-94, F-97)
**Tujuan:** app bisa di-install orang lain, onboarding jelas, kapsul kosong nempel pas di kamera dan tidak mengganggu.

Pekerjaan:
- [x] `CapsoolApp` + `AppGraph`; hapus facade `com.iqbalf`; pindah simulator ke `src/debug` (varian `src/release` berisi stub kosong).
- [x] Refactor store: `Action = UserIntent | SystemEvent`, reducer → `Reduction(state, effects)`, `PlatformEffectHandler` tersambung (aksi global shade/quick settings sudah jalan; efek lain di-log by type sampai milestone-nya).
- [x] `SettingsRepository` (DataStore) + migrasi dari `capsool_cutout_prefs`.
- [x] `OverlayWindowController`: dua ukuran window, outside touch, posisi dari `DisplayCutout` (cutout path), per orientasi.
- [x] Config aksesibilitas minimal (§2.4.1) + pelacak app foreground + deteksi keyboard/fullscreen/landscape/layar mati.
- [x] Onboarding: layar disclosure + persetujuan → status izin (tombol ke Accessibility settings, petunjuk "restricted settings" untuk sideload). ⚠️ Langkah notification listener dipindah ke M1, karena listener baru dipakai di M1 (prinsip: tidak minta izin yang belum dipakai).
- [x] Kalibrasi (F-02) tersambung ke settings, target merah tampil di overlay asli; geometri terpisah portrait/landscape.
- [x] Dynamic color + fallback palet. ⚠️ Pill sengaja hitam pekat (menyatu dengan kamera); dynamic color berlaku untuk aksen overlay & app pendamping. Kartu lama masih memakai token statis — dipindah ke `colorScheme` saat masing-masing kartu di-wire (M1–M4).
- [x] Cek Advanced Protection (F-97) dan tampilkan peringatan di halaman status; toggle haptic (F-87).
- [x] Perbaiki bug gesture drag→tap; pemetaan gesture default (F-70, subset aksi F-71: expand/collapse, ganti kartu, play/pause, buka shade, quick settings) disimpan di settings (UI pemetaan di M6).

Acceptance di device (✅ = dicek di emulator Pixel 10 Pro, Android 17 / API 37, 2026-10-15; HP fisik belum):
- [~] Install bersih → onboarding → setelah izin aksesibilitas aktif, pill muncul ≤ 1 s menutupi punch-hole. ✅ emulator (posisi dari cutout path). ❌ belum dicek di HP fisik / Samsung / Xiaomi.
- [x] Tap di luar pill tetap sampai ke app di bawahnya. ✅ emulator: tap di samping pill mengganti tab; tap di luar kartu expanded menutup kartu **dan** tetap memicu tombol di bawahnya.
- [~] Fullscreen → pill hilang. ❌ belum terverifikasi (tidak ada app fullscreen di emulator; deteksi via insets masih risiko, lihat §3.4). Keyboard → pill hilang ✅ emulator. Landscape → hilang ✅ emulator.
- [ ] Ganti wallpaper → warna aksen berubah (Android 12+). Belum dicek.
- [~] Ubah lebar/offset di app pendamping → overlay berubah tanpa restart ✅ emulator (toggle target kalibrasi live). Reboot HP → pengaturan tetap: belum dicek.
- [x] Matikan izin aksesibilitas → halaman status menunjukkan merah dengan tombol perbaiki. ✅ (status ikut `serviceRunning`).
- [x] Build release tidak berisi kelas simulator. ✅ string simulator ada di APK debug, 0 di APK release.
- [ ] Di device dengan Advanced Protection aktif (Android 17), halaman status menampilkan peringatan. Belum dicek.

### M1 — Media (F-10..F-13)
Pekerjaan:
- [x] `CapsoolNotificationListener` (+ `requestRebind`, `default_filter_types`) dan `MediaSource` (`MediaSessionManager` + `MediaController.Callback`, debounce 120 ms, artwork di-downscale ke 256 px).
- [x] Pemilih sesi pure (`MediaSessionSelector`): sesi playing menang, lalu sesi yang sedang diikuti, lalu yang terbaru.
- [x] Effect play/pause/next/prev/seek/buka player → `transportControls` / `sessionActivity`.
- [x] Warna kartu dari cover (Palette, vibrant → dominant), kartu media di-restyle: cover asli, ikon vektor (bukan emoji), tombol play/pause berubah bentuk, seek bar hanya jika `ACTION_SEEK_TO` + durasi > 0, posisi diekstrapolasi dari `lastPositionUpdateTime`.
- [x] Mini indicator (F-11): cover berputar + equalizer berwarna aksen cover; ikut aturan visibilitas media yang sama dengan kartu.
- [x] Aturan auto-hide (F-12): delay setelah pause (instan / 5 s / 15 s / 30 s / 1 m / 2 m / selalu, default 30 s); sesi yang pertama kali terlihat dalam keadaan pause langsung disembunyikan; sembunyi saat app player di depan (deteksi app depan hanya dari window `TYPE_APPLICATION`).
- [x] Langkah izin notification access di halaman Status (+ fallback ke daftar listener).
- [x] Gesture saat kartu expanded: hanya swipe yang ditangani kapsul; tap/long press diserahkan ke tombol kartu, dan drag slider tidak lagi dianggap swipe ganti kartu.
- [x] Debug-only: `MediaSession` asli tanpa audio untuk tes end-to-end di device tanpa app musik.
- [x] F-13 whitelist app media (P1): `mediaOnlyAllowedApps` + `mediaAllowedApps` (kunci `media.onlyAllowedApps`, `media.appWhitelist`). Disaring **di pemilihan sesi** (`MediaSource.isAllowed`), bukan di tampilan, sehingga pemutar yang tidak diizinkan tidak bisa menutupi pemutar lain yang diizinkan; mengubah daftar langsung memilih ulang sesi. Daftar kosong + saklar aktif = tidak ada pemutar. Belum dicoba dengan dua pemutar nyata.

Acceptance di device (✅ = emulator Pixel 10 Pro / API 37 dengan `MediaSession` debug asli, 2026-10-15; user melaporkan M0+M1 berjalan di HP-nya — Pixel 8 Pro terhubung, detail per item belum dicatat):
- [~] Spotify, YouTube Music, player lokal: judul, artis, cover sesuai. ✅ untuk sesi debug (lewat `MediaSessionManager` sungguhan). ❌ belum dites dengan Spotify/YouTube Music/player lokal (butuh login atau app tidak ada di emulator).
- [x] Play/pause/next/prev dari kapsul mengubah pemutaran asli. ✅ next → "Debug Track Two", pause → `PAUSED` di `dumpsys media_session`. Perubahan dari luar ter-update ≤ 300 ms: ✅ pill berubah dalam satu screenshot (< 1,5 s; latensi persis belum diukur).
- [x] Seek bar bergerak dan seek mengubah posisi ✅ (drag → posisi sesi 2:01). Player tanpa durasi → seek bar tersembunyi: ✅ unit test, belum dicek visual.
- [x] Pause 30 s (default) → kartu media hilang ✅. Buka app player → kartu & indikator pill hilang ✅ (dicek dengan app pemilik sesi).
- [ ] Tap cover → membuka app player. Belum terverifikasi (sesi debug tidak punya `sessionActivity`).
- [ ] Layar mati 10 menit dengan musik berjalan → tidak ada lonjakan baterai. Belum dicek.

### M2 — Notifikasi (F-20..F-26)
Pekerjaan:
- [x] `NotificationSource`: parsing `StatusBarNotification` (MessagingStyle → pengirim & pesan terakhir, avatar `Person`/large icon, small icon app, nama app, warna glow dari `notification.color` atau Palette ikon launcher).
- [x] `NotificationFilter` pure: abaikan package sendiri (kecuali build debug), group summary, ongoing, MediaStyle, kategori transport/service/system/progress/navigation/call/status, importance < DEFAULT, tanpa isi, app di blacklist; update `FLAG_ONLY_ALERT_ONCE` → refresh di tempat tanpa pop-up ulang.
- [x] Antrian maks 5 (terbaru di depan); notifikasi dihapus dari shade → hilang dari kapsul.
- [x] Mode tampil (F-24): kecil lalu tap untuk buka / langsung buka kartu; delay 1–30 s atau "sampai dibuka"; menutup kartu yang sedang dibaca = sudah dilihat (keluar dari kapsul).
- [x] Aksi (F-21): balas inline via `RemoteInput`, Read (`SEMANTIC_ACTION_MARK_AS_READ`), Dismiss (`cancelNotification`), tap isi → buka app (`contentIntent`, opt-in background activity start di Android 14+). Tombol hanya muncul bila app mendukung.
- [x] Window overlay jadi focusable hanya selama mengetik balasan; keyboard tidak menyembunyikan kapsul saat membalas; timeout expanded dijeda selama mengetik.
- [x] Privasi lock screen (F-22): teks disembunyikan saat terkunci kecuali notifikasi `VISIBILITY_PUBLIC` atau user mematikan opsi; balas dinonaktifkan saat terkunci.
- [x] Halaman "Apps in the capsule" (F-22 + F-26): blokir per app + tombol "Pop-up" ke pengaturan notifikasi app (`ACTION_APP_NOTIFICATION_SETTINGS`). Daftar app lewat `LauncherApps`; `<queries>` launcher untuk nama/ikon (bukan `QUERY_ALL_PACKAGES`).
- [x] Hapus dari shade setelah dilihat (F-25, opsional, default mati) dan glow berdenyut di pill (F-20).
- [x] Debug-only: notifikasi chat asli (MessagingStyle + RemoteInput + mark-as-read) untuk tes end-to-end.
- [x] F-23 swipe samping untuk dismiss (P1). Desain: **opt-in** (`notificationSwipeDismiss`, default mati) karena swipe kiri/kanan di kartu notifikasi sebelumnya berarti ganti kartu. Saat aktif, menyeret kartu notifikasi (yang boleh dihapus, bukan saat membalas) melewati 96 dp lalu melepas akan `NotificationDismiss`; kurang dari itu kartu kembali. Drag horizontal dikonsumsi kartu sehingga gestur kapsul tidak ikut ganti kartu; swipe ke atas tetap menutup. Untuk ganti kartu, geser di baris titik halaman (kini selebar kartu). Swipe-up-untuk-collapse sudah ada sebelumnya. Belum dicoba di perangkat.

Acceptance di device (✅ = emulator Pixel 10 Pro / API 37 dengan notifikasi debug asli, 2026-10-15):
- [~] Pesan WhatsApp & Telegram masuk → avatar, nama, isi, badge app, glow. ✅ untuk notifikasi MessagingStyle debug (pill: avatar + ikon app + glow hijau; kartu: pengirim, app, pesan). ❌ belum dites dengan WhatsApp/Telegram asli.
- [x] Balas dari kapsul → pesan benar-benar terkirim. ✅ `RemoteInput` sampai ke app pengirim ("Reply received: On my way"); keyboard muncul dan kapsul tetap tampil. ❌ belum dengan WhatsApp/Telegram.
- [x] Mark as read hilang dari shade ✅ (aksi sampai ke app debug, notifikasi dibatalkan); tombol tidak tampil bila app tidak mendukung: ✅ unit test.
- [x] Dismiss menghapus notifikasi dari shade ✅ (`dumpsys notification` 1 → 0).
- [ ] App di blacklist tidak pernah tampil: ✅ unit test filter, belum dicek di device. Notifikasi ongoing tidak tampil: ✅ unit test.
- [ ] Layar terkunci + "sembunyikan isi" → hanya nama app: ✅ unit test, belum dicek di device.
- [ ] Mode ringkas vs expanded dan delay auto-hide: ✅ unit test, baru mode ringkas yang dicek di emulator.
- [ ] Halaman "Apps in the capsule" → Pop-up membuka pengaturan notifikasi app: belum dicek.
- [ ] 10 notifikasi beruntun dalam 5 s → tidak crash, antrian maks 5: ✅ unit test antrian, belum stress test di device.

### M3 — Baterai & event sistem (F-30..F-32, F-60..F-65, F-68, F-69)
Pekerjaan:
- [x] `BatterySource`: sticky `ACTION_BATTERY_CHANGED` → persen, tipe charger (AC/USB/wireless/dock), status penuh, suhu, estimasi penuh (`computeChargeTimeRemaining`); hanya perubahan yang terlihat yang di-dispatch (jitter suhu < 1 °C diabaikan).
- [x] `BatteryEventDetector` pure: colok/cabut, penuh sekali selama tercolok, baterai lemah saat melewati ambang user dan lagi di 5 %; bacaan pertama hanya baseline.
- [x] `SystemEventSource`: ringer (abaikan sticky awal), mode pesawat, unlock, Wi-Fi (`NetworkCallback`, `WifiTracker` pure dengan jendela baseline 1,5 s), headphone Bluetooth (ACL + broadcast baterai tersembunyi, best effort; receiver mewajibkan pengirim punya `BLUETOOTH_CONNECT`), auto-brightness (`ContentObserver`).
- [x] Sumber hidup hanya selama layanan aksesibilitas berjalan.
- [x] Mini event lebar (pill + 110 dp) dengan ikon Material, judul, nilai dan cincin level; crossfade saat muncul/hilang; lebar ikut dihitung di window/touchable region.
- [x] Kartu charging expanded di-restyle (cincin level, tipe charger, estimasi penuh, suhu °C/°F); pill compact saat charging (petir + persen), bisa dimatikan.
- [x] Pengaturan "Events": on/off per kategori (unlock default mati), durasi 2/3/4/6 s, ambang baterai lemah 5–30 %, °F; tombol izin Bluetooth saat event headphone aktif.
- [x] Notifikasi dari `android`/`com.android.systemui` (baterai, USB debugging, screenshot) tidak lagi masuk kapsul.
- [x] Fix crash API: `getParcelableExtra(…, Class)` (API 33) diganti `IntentCompat`.
- [x] F-68 tampilan per event: `EventStyle` per kategori (warna, ikon dari daftar ikon bersama, bentuk kiri lingkaran/persegi bulat/tidak ada, sisi kanan nilai+cincin/nilai saja/tidak ada), kunci `events.styles` satu baris per kategori (yang sama dengan bawaan tidak ditulis, baris rusak dilewati). Editor "Pop-up appearance" di tab Capsule. Belum dicoba di perangkat.

Acceptance di device (✅ = emulator Pixel 10 Pro / API 37, event dipicu via `dumpsys battery`, `cmd audio`, `svc wifi`, `cmd connectivity`, `settings`; 2026-10-15):
- [~] Colok charger → mini event dengan ikon & persen ✅ ("Charging 70 %" + cincin). Di emulator event baru tergambar ~1–2 s setelah colok karena GPU emulasi tersendat oleh animasi charging SystemUI (`gfxinfo`: frame lambat di "issue draw commands"); store sudah benar dalam < 10 ms. Perlu dicek di HP fisik. Wireless charger: belum.
- [x] **Teks event mini tertutup lubang kamera** (laporan di HP, 2026-10-16): pil event terpusat di lubang, dan judul berada di tengah baris, tepat di belakang lubang. Kini pil **dibelah dua di sekitar lubang** (`WindowPlacement.cameraGapDp` dari lebar lubang nyata + 4 dp tiap sisi): ikon dan judul di kiri, nilai dan cincin di kanan, celah kosong di tengah; pil dilebarkan `SPLIT_EXTRA_WIDTH_DP` (40) agar tiap sisi muat judul. Tanpa lubang (tidak ada cutout, atau mode mandiri) tata letak lama dipakai. Kartu ringkas lain (timer, media, notifikasi, progres) menaruh isi di tepi kiri/kanan sehingga tengahnya sudah kosong. **Belum dilihat langsung**: diverifikasi hanya lewat tes penempatan; judul panjang masih bisa terpotong dengan elipsis.
- [x] Cabut → mini event ✅. Baterai melewati 15 % → peringatan sekali ✅ (unit test: tidak dobel, ulang di 5 %).
- [x] Ganti mode dering → mini event ✅ (Vibrate, Ring). Via kapsul tidak dobel: belum relevan sampai kontrol ringer M4.
- [ ] Earbuds Bluetooth → nama + baterai: belum (emulator tanpa Bluetooth); alur izin belum dicek.
- [x] Wi-Fi on/off ✅, mode pesawat ✅, auto-brightness ✅, unlock: belum dicek (default mati); event bisa dimatikan per kategori ✅ unit test.

### M4 — Slider & senter (F-50..F-53, F-51)
Pekerjaan:
- [x] Kartu Controls (`SLIDERS`): dibuka dengan tap pill saat tidak ada aktivitas atau long press (gesture default baru `OPEN_CONTROLS`); muncul sendiri sebagai kartu dinamis saat senter menyala; tertutup bersama kartu.
- [x] `ControlsSource`: volume media (`STREAM_MUSIC`), mode dering, brightness (Settings.System + kurva gamma `BrightnessCurve`), auto-brightness, senter (`TorchCallback`, strength API 33+); satu `ContentObserver` Settings.System untuk perubahan dari luar.
- [x] Ringer: ring → vibrate → silent; tanpa akses DND, silent dilewati dan user diarahkan ke pengaturan akses DND. Perubahan ringer dari kapsul tidak memicu mini event dobel (`isOwnRingerChange`, 1 s).
- [x] Brightness/auto tanpa izin → buka `ACTION_MANAGE_WRITE_SETTINGS`; izin dibaca ulang setiap kali Controls dibuka (pemberian izin di Settings tidak mengirim broadcast).
- [x] Senter: tanpa flash → "No flashlight on this device"; kamera memakai flash → "Camera in use"; strength < 5 % = mati.
- [x] 3 gaya slider (F-51): Pill, Split (M3 Slider), Rounded; slider kustom meng-consume event sehingga drag tidak dibaca sebagai swipe ganti kartu.
- [x] Pengaturan "Controls": gaya slider, status izin brightness & silent, kalibrasi brightness maksimum (baca nilai sistem saat user set ke maksimal) + reset.
- [x] Manifest: `WRITE_SETTINGS` dan `ACCESS_NOTIFICATION_POLICY` dideklarasikan (tanpa ini app tidak muncul di daftar izin Settings — ketemu saat tes).

Acceptance di device (✅ = emulator Pixel 10 Pro / API 37; izin diberi lewat `appops`/`cmd notification` lalu dikembalikan; 2026-10-15):
- [x] Geser volume → volume media berubah ✅ (10/15); tombol volume fisik → slider ikut ✅.
- [~] Siklus ringer ✅ ring → vibrate → ring; mode silent dikembalikan sistem emulator ke vibrate (card menampilkan status asli). Perlu dicek di HP fisik. Tanpa izin DND: ✅ unit test (lewati silent + buka pengaturan).
- [~] Brightness: ✅ geser mengubah `screen_brightness` mengikuti kurva gamma (tengah slider ≈ 14/255, seperti slider sistem); posisi awal benar (102 → ~83 %). Kalibrasi di Samsung/Xiaomi: belum.
- [ ] Senter on/off & strength: belum (emulator tanpa flash; jalur "No flashlight" ✅). Pesan "Camera in use": ✅ unit test, belum di device.

### M5 — Progress & timer (F-40, F-41, F-43 sebagian)
Pekerjaan:
- [x] `OngoingClassifier` pure: chronometer + ongoing → timer (timer yang di-pause tetap timer selama notifikasinya masih ongoing); `EXTRA_PROGRESS_MAX`/indeterminate → progress; panggilan, navigasi, dan MediaStyle dikecualikan. Tanpa daftar package hardcode.
- [x] `NotificationSource` merutekan progress/timer sebelum filter pesan; update progress di-throttle 500 ms (item baru, selesai, dan perubahan determinate/indeterminate selalu lolos).
- [x] Progress selesai: notifikasi hilang di ≥ 90 % atau berubah jadi notifikasi selesai → mini event "Done · nama file" (kategori event baru "Download finished"); hilang di bawah itu = dibatalkan, tanpa event.
- [x] Timer: waktu dihitung dari `notification.when` (countdown/stopwatch), berdetak tiap detik hanya saat terlihat dan berjalan; saat pause menampilkan teks app; tombol = aksi notifikasi asli app jam (dijalankan per index).
- [x] UI: pill timer (ikon + waktu berjalan), pill progress (ikon app + cincin/spinner), kartu timer (waktu besar + tombol app), kartu progress (maks 3 baris + "+N more").
- [x] Pengaturan: kartu progress dan kartu timer bisa dimatikan.
- [x] Debug-only: notifikasi download dan timer 2 menit asli (+1:00 / Pause-Resume / Stop berfungsi).
- [x] F-43 pilih app progress + gaya: `progressOnlyAllowedApps`/`progressAllowedApps` (disaring di reducer; mempersempit daftar langsung menghapus yang sudah tampil), gaya ikon (ikon asli app / lencana unduhan), warna bar dan track (null = dari warna app / putih tipis). Dipakai di pil dan kartu progress. (Blokir per app dari halaman notifikasi tetap berlaku.)

Acceptance di device (✅ = emulator Pixel 10 Pro / API 37 dengan notifikasi debug asli; 2026-10-15):
- [~] Download dari Play Store/Chrome → progress tampil & bergerak: ✅ untuk notifikasi progress standar (pill cincin, kartu 45 %, "Done" di akhir). ❌ belum dengan Play Store/Chrome asli.
- [~] Timer → hitung mundur sinkron ✅ (1:59 → 1:56 → 1:53); +1:00 ✅, pause/resume ✅ (label "Resume" dari app), stop ✅. ❌ belum dengan Google Clock asli; stopwatch: ✅ unit test.
- [ ] Timer selesai → status selesai dengan tombol stop: belum dicek (tergantung notifikasi alarm app jam).

### M6 — Pintasan, gesture lengkap & otomasi (F-54..F-57, F-70..F-73)
Pekerjaan:
- [x] Model Barcode: `BarcodeFormat` (10 format: QR_CODE, EAN_13, EAN_8, UPC_A, UPC_E, CODE_128, CODE_39, CODE_93, CODABAR, ITF) + `BarcodeContent` dengan default kosong/nonaktif.
- [x] Validator pure (`BarcodeValidator`): validasi panjang dan checksum (EAN-13, EAN-8, UPC-A, UPC-E), batas <= 80 digit dan panjang genap ITF, batas panjang Code 39 & Code 93, charset Code 39/93/128/Codabar, panjang wajar QR Code; hasil sealed `Ok` / `Error(message)` yang mudah dibaca user; test kontrak validator↔encoder di JVM memastikan seluruh isi valid selalu berhasil di-encode.
- [x] Encoder pure (`BarcodeEncoder`): render matriks modul piksel `BarcodeMatrix` (`List<BooleanArray>`) via ZXing `MultiFormatWriter` dengan margin 0 (tanpa Android Bitmap); tolak isi tidak valid.
- [x] UI kartu barcode (`ExpandedBarcodeCard`): Canvas dengan integer scaling (modul tidak pecah), pelat putih rounded + quiet zone, rotasi 180° (`flipped`), warna kustom dari `BarcodeContent.color`, label di bawah kode; fallback teks pesan error yang jelas jika pembuatan kode gagal (tidak diam/kosong).
- [x] Integrasi kartu Shortcuts (`ExpandedShortcutsCard`): barcode tampil di bagian atas kartu Shortcuts saat aktif dan valid; tampil barcode saja jika tidak ada pintasan; brightness window dinaikkan ke 100% (`screenBrightness = 1f`) saat barcode tampil di overlay (`OverlayWindowController`) dan kembali saat hilang.
- [x] Pengaturan & DataStore (`SettingsRepository`): `barcode` dan `barcodeCardEnabled` dengan backward compatibility.
- [x] UI app pendamping (`ShortcutsTab`): kartu pengaturan barcode (pemilih format, input isi & label, palet warna, switch flip, switch aktif, pesan error validator real-time, live preview, serta penanganan error encoder jika pembuatan kode gagal).
- [ ] Scan barcode via kamera (ML Kit Code Scanner) & impor gambar galeri (ML Kit Barcode Scanning) — belum (lingkup M6a difokuskan pada input ketik, validasi, dan render ZXing; ML Kit tidak ditambahkan di run ini agar bebas izin kamera).
- [x] Model CustomIntent (F-76): `CustomIntentType` (ACTIVITY, SERVICE, BROADCAST) + `CustomIntent` (id, label, type, action, component?, extras, icon) dengan encoding deterministik bebas `org.json` yang aman untuk extras multi-baris dan karakter khusus.
- [x] Validator pure (`CustomIntentValidator`): validasi action tidak boleh kosong dan format component `package/class` jika diisi.
- [x] Pengaturan & DataStore (`SettingsRepository`): kunci `automation.intents` untuk persistensi `customIntents` dengan backward compatibility.
- [x] Aksi gesture (`GestureAction.CUSTOM_INTENT`): pemetaan gesture ke custom intent berdasar id, exhaustive `when` di reducer dan UI label, reducer pure mengeluarkan `Effect.SendCustomIntent` hanya jika intent terdaftar di settings.
- [x] Eksekutor (`CustomIntentRunner`): bangun Intent (action, component class, extras), eksekusi sesuai tipe (`startActivity` + `FLAG_ACTIVITY_NEW_TASK`, `startForegroundService`, `sendBroadcast`); penanganan error komprehensif (`ActivityNotFoundException`, `SecurityException` untuk unexported component di Android 12+, `IllegalStateException` untuk pembatasan background di Android 14+, dll.) dengan pesan ramah user.
- [x] Integrasi effect (`PlatformEffectHandler`): eksekusi `Effect.SendCustomIntent` via runner saat gesture dipicu. Hasil eksekusi **sengaja tidak ditampilkan** di jalur ini (tidak ada callback/flow yang dikonsumsi) — jalur diagnostik error adalah tombol "Send test" di `IntentsTab`.
- [x] Feedback saat intent yang dipicu **gesture** gagal: `MiniEventType.ACTION_FAILED` (kategori null = selalu tampil, tidak bisa dimatikan lewat daftar pop-up karena itu umpan balik atas aksi pengguna sendiri) dengan judul "Action failed" dan nama intent; penjelasan lengkap tetap di tombol "Send test". Intent yang berhasil tetap senyap.
- [x] UI editor & tombol tes (`IntentsTab`): tab "Custom Actions" di app pendamping untuk tambah, ubah, hapus intent (label, tipe, action, component, extras key-value, icon picker) dengan tombol "Send test" yang menampilkan pesan sukses atau error asli di UI.
- [x] Model & persistensi kartu statis (M6c-1, PRD baris 81 & §2.6 baris 513): `StaticCardType` (SHORTCUTS, BARCODE, SLIDERS, INTENT), `StaticCardViewMode` (CAROUSEL, LIST), `StaticCard` (id, type, label), `encodeAll`/`decodeAll` berbasis karakter kontrol bebas `org.json` dengan sanitasi label, helper urutan murni (`moveUp`, `moveDown`, `without`), `Settings` (`staticCards`, `startCardId`, `staticCardViewMode`, properti derivasi `effectiveStaticCards` dengan fallback backward-compatible ke Shortcuts/Barcode/CustomIntents, `effectiveStartCardId`), dan persistensi `SettingsRepository` (`cards.static`, `cards.staticStart`, `cards.staticViewMode`).
- [x] UI kelola kartu statis di app pendamping (M6c-2): tab "Cards" di `CompanionMainScreen`, pemilih mode tampilan Carousel/List (`staticCardViewMode`), daftar kartu berurutan (`effectiveStaticCards`) dengan kontrol naik/turun/hapus dan penanda kartu awal (`startCardId`), fungsi logika murni seeding kartu dari mode turunan (`Settings.seedStaticCards`), dropdown menu penambahan kartu unik berdasarkan tipe yang belum ada (`StaticCardType`) dan dialog pengisian label.
- [x] Kartu statis tampil sebagai halaman carousel (M6c-3a): `CardPage` sealed interface (`Dynamic`/`Static` dengan `key` stabil), `CapsuleState.pages` (halaman dinamis saat ada aktivitas, statis saat idle), `activePage` (fokus awal dari `effectiveStartCardId`), `hasActivity=false` saat hanya statis, `mode` EXPANDED untuk statis, `cycleCard` atas `pages`, `focusedPageKey: String?` menggantikan `focusedCard: CapsuleCardType?`, `OPEN_SHORTCUTS` gesture ke kartu statis jika dikonfigurasi. Overlay merender `Dynamic` seperti semula dan `Static` untuk SHORTCUTS, BARCODE, SLIDERS, INTENT. Commit: `5ddbd45`.
- [x] Koreksi hasil review M6c-3a: (a) brightness barcode (`OverlayWindowController`) kini sadar-halaman — sebelumnya `activeCard` bernilai `null` untuk halaman statis sehingga **peningkatan brightness ikut hilang** saat barcode tampil sebagai kartu statis; (b) kartu statis INTENT kini benar-benar berfungsi lewat `UserIntent.RunCustomIntent` + `ExpandedIntentCard` (sebelumnya `-> Unit`, halaman carousel kosong); (c) `customIntentIcon` dipindah ke `ui/components/CustomIntentIcons.kt` supaya overlay dan editor berbagi satu pemetaan ikon; (d) 9 test baru di `StaticCardPagesTest` — sebelumnya 158 test lulus **tanpa satu pun** menguji perilaku baru ini. Total **167 test lulus**, 0 gagal, 0 lint error.
- [x] Scan barcode dari gambar galeri (M6d, F-57): `ActivityResultContracts.PickVisualMedia` (Android Photo Picker, **tanpa izin**) + ML Kit **bundled** `com.google.mlkit:barcode-scanning:17.3.0` (jalan offline, jadi varian unbundled `play-services-mlkit-barcode-scanning` sengaja tidak dipakai). Kebijakan pemilihan hasil dipisah menjadi fungsi murni `BarcodeScanPolicy.pick` yang diuji di JVM (6 test), sedangkan lapisan ML Kit tinggal plumbing tipis; pemetaan format memakai konstanta `Barcode.FORMAT_*` ML Kit langsung, bukan angka dari ingatan. Tombol **"Scan from image"** di tab Barcode mengisi isi + format sekaligus; kegagalan tampil sebagai pesan yang menyebut penyebabnya, bukan "failed" generik. **Kamera sengaja tidak dipakai** karena §1.6 baris 192 melarang izin `CAMERA`. Catatan: F-57 dan baris 478 spesifikasi juga menyebut opsi `play-services-code-scanner`; itu dikerjakan di M6d-2 di bawah.
- [x] Scan barcode lewat kamera **tanpa izin kamera** (M6d-2, `play-services-code-scanner:16.1.0`): UI scanner berjalan di dalam proses Google Play Services, jadi aplikasi tidak perlu memegang izin `CAMERA` — inilah satu-satunya jalur kamera yang dipakai. AAR-nya sudah diperiksa langsung dan **tidak mendeklarasikan satu pun izin**. Tombol **"Scan with camera"** berdampingan dengan "Scan from image", dan keduanya bermuara pada penanganan hasil yang sama. Modul scanner diunduh Play Services saat pertama dipakai; kalau Play Services tidak ada atau terlalu tua, atau unduhan modul ditolak, kegagalannya dilaporkan apa adanya. Poin penting: jalur ini **tidak** memakai `Tasks.await` — scanner interaktif butuh main thread untuk menampilkan activity-nya, sehingga memakai listener + `suspendCancellableCoroutine` agar UI tidak beku; menutup scanner dihitung sebagai `BarcodeScanResult.Cancelled`, bukan error. APK hanya bertambah ~230 KB karena modelnya tidak ikut dibundel. 4 test baru (`BarcodeFormatsTest`) mengunci agar kedua jalur meminta daftar format yang sama persis, sehingga tidak ada format yang ditawarkan di UI tetapi tidak pernah diminta.
- [x] ML Kit ternyata menyuntik `android.permission.INTERNET` ke manifest gabungan lewat `com.google.android.datatransport:transport-backend-cct` (pipa telemetri logging-nya). §1.6 baris 195 menyatakan **INTERNET tidak dipakai**, jadi izin itu dicabut paksa dengan `tools:node="remove"`. Manifest gabungan sekarang bersih dari INTERNET; `CAMERA` juga tidak ada (izin tetap 5). Ukuran APK debug naik dari ~66 MB ke ~92 MB karena model barcode dibundel; menyusut lagi setelah R8 di release build.
- [x] **Mode LIST digambar di overlay** (M6c-3b). `staticCardViewMode = LIST` kini benar-benar mengubah tampilan, tidak lagi tersimpan tanpa efek. Kartu statis yang sama ditumpuk vertikal oleh `ExpandedStaticList`, yang memanggil `StaticCardContent` — renderer yang **sama** dengan carousel, sehingga satu kartu tidak mungkin tampil berbeda antar mode. LIST hanya dipakai saat idle; aktivitas nyata selalu memakai carousel (spec baris 81 menyebut carousel/list sebagai penyajian kartu statis saat kapsul dibuka tanpa aktivitas).
  Tinggi panel dijaga satu sumber kebenaran: `CapsuleState.listRows` → `OverlayPlacement.expandedContentHeightDp()`, dipakai **baik oleh window controller maupun Compose**, sehingga ukuran window dan isinya tidak bisa melenceng. Tinggi dibatasi `MAX_LIST_ROWS = 2` lalu daftarnya menggulir; tanpa batas itu window akan tumbuh ke bawah menutupi layar setiap kali user menambah kartu.
  **Perlu verifikasi visual di device**: tampilan baris bertumpuk, batas 2 baris, dan gulir di dalam kapsul belum pernah dilihat mata manusia — unit test hanya membuktikan geometri dan kondisinya, bukan bahwa ia enak dilihat.

Acceptance di device:
- [ ] Pilih 4 app → muncul di kartu pintasan dengan ikon themed (13+) → tap membuka app dari layar mana pun.
- [ ] Kontak via picker tanpa izin kontak; telepon membuka dialer; WhatsApp membuka chat yang benar.
- [ ] Setiap gesture bisa dipetakan ke setiap aksi; screenshot tidak memuat kapsul; lock screen bekerja.
- [ ] Scroll-to-top di Chrome, Settings, Gmail, Instagram.
- [ ] Profil Tasker yang mendengarkan `com.iqbalwork.capsule.action.EVENT` terpicu saat double tap.
- [ ] Pemetaan gesture berbeda saat ada media aktif vs idle (F-75) bekerja.
- [ ] Intent builder: aksi Broadcast ke MacroDroid (trigger "Intent Received") dan aksi Activity ke app lain bekerja; tombol tes menampilkan error yang jelas saat target salah.
- [ ] Kartu statis bisa ditambah, diurutkan, dijadikan kartu awal. **Mode carousel & list**: keduanya siap diuji — carousel bergeser kiri/kanan, list menumpuk vertikal dan menggulir saat kartu lebih dari 2.
- [ ] Barcode: ketik EAN-13 valid → barcode tampil & bisa di-scan kasir/app scanner lain; isi tidak valid → pesan error.
- [ ] Barcode: pilih gambar QR/EAN dari galeri → isi dan format terisi otomatis; gambar tanpa barcode → pesan yang jelas.
- [ ] Barcode: **Scan with camera** → UI scanner Play Services terbuka (unduhan modul saat pertama kali), hasil mengisi isi + format; menutup scanner tidak memunculkan error; tanpa Google Play Services → pesan yang jelas. Cek `adb shell dumpsys package com.iqbalwork.capsule | grep -i camera` tetap kosong.

### M7 — Personalisasi & visibilitas lanjutan (F-06, F-81, F-82, F-84, F-85, F-92)
- [x] **F-84 daftar app tersembunyi** (`Settings.hiddenPackages`) dan **F-85 perilaku lock screen** jadi enum 3 nilai (`LockScreenBehavior.SHOW / SHOW_WITHOUT_CONTENT / HIDE`, menggantikan boolean `hideNotificationTextWhenLocked`). Keduanya diterapkan di **satu tempat**: `CapsuleState.isAllowedOnScreen`, tempat yang memang sudah memutuskan boleh-tidaknya kapsul tampil — sehingga aturan ini tidak bisa dilangkahi jalur lain. App yang belum diketahui (`foregroundPackage == null`) tidak pernah dianggap tersembunyi, supaya daftar hide tidak menelan kapsul di jendela waktu sebelum service melaporkan app.
- [x] **Migrasi setelan F-85** mengikuti urutan expand → migrate → verify → **berhenti sebelum contract**: kunci baru `visibility.lockScreenBehavior` ditulis bersama kunci lama `notification.hideContentWhenLocked` (dual write), dan pembacaan memakai kunci baru bila ada — kalau tidak, diturunkan dari boolean lama (`true` → SHOW_WITHOUT_CONTENT, `false` → SHOW, tidak ada keduanya → default). Jadi pilihan tersimpan tidak hilang diam-diam, dan build lama masih membaca perilaku yang ia harapkan. Kunci lama **belum dihapus**; itu langkah terpisah yang harus diminta eksplisit.
- [x] **UI pengaturan (M7a-2)**: perilaku lock screen jadi pemilih tiga tombol di tab Capsule (menggantikan switch lama), dan **daftar app tersembunyi** ditambahkan ke section "Hide the capsule" yang sudah ada — bukan section baru, supaya semua yang berkaitan menyembunyikan kapsul ada di satu tempat. Menambah app memakai `AppPickerDialog` yang sudah dipakai Shortcuts/Gestures. Hanya **nama package** yang disimpan (itu yang dibandingkan aturan visibilitas); label diambil dari `PackageManager` untuk ditampilkan, dan package yang tidak bisa diresolve jatuh ke nama package-nya sendiri alih-alih hilang dari daftar.
- [x] **F-81 shape & warna** dan **F-82 animasi** (M7b). `Settings` bertambah `shapeStyle` (PILL / ROUNDED_RECT / SQUIRCLE), `customBackground`, `borderDp` (0–3), `borderColor`, `shadowDp` (0–6), `animationSpeed` (0.5–2), `bounce` (5 level), `expandStyle` (ORGANIC / RIGID), `reduceAnimations`. Default **ROUNDED_RECT** supaya radius sudut yang sudah diatur user tidak berubah; PILL mengabaikan radius dan membulat penuh. Squircle adalah rounded rect dengan sudut superellipse (`SquircleShape`). Semua kurva animasi diturunkan di satu tempat (`CapsuleMotion.of(settings)`, data murni): kecepatan menskalakan stiffness dengan kuadrat, bounce menggeser damping, RIGID = `tween(200/speed)`, reduce = tween 90 ms tanpa slide kartu.
- [x] **Jeda shrink jendela overlay mengikuti motion** (`CapsuleMotion.shrinkDelayMillis` = 2,5× waktu settle collapse; 750 ms pada default, sama seperti konstanta lama). Tanpa ini, bounce tinggi atau kecepatan rendah akan memotong tepi kapsul yang masih bergerak.
- [x] **Background kustom dipaksa tetap gelap** (`readableShellColor`): kartu memakai teks putih, jadi warna terang diredupkan ke arah hitam dengan hue yang sama. Shadow dibatasi 6 dp karena jendela overlay hanya menyisakan margin kecil di sekeliling kapsul. Shadow dan border digambar di luar `clip`, kalau tidak terpotong.
- [x] **F-06 prioritas kartu yang bisa diatur** (M7c). `Settings.cardPriority` (kunci `cards.priority`, daftar nama dipisah koma) menggantikan konstanta privat `CardPriority` di `CapsuleState`; default sama persis dengan urutan lama, jadi tidak ada perubahan perilaku sampai user mengubahnya. `normalizeCardPriority` membuat urutan apa pun aman dipakai (duplikat dibuang, kartu yang hilang ditambahkan di belakang menurut urutan default, nama tak dikenal dilewati), sehingga build lama/baru yang bertukar data tidak bisa membuat kartu lenyap. Hanya 5 kartu aktivitas (notifikasi, timer, media, progress, charging) yang bisa dipindah (`moveCard`); Controls dan Shortcuts dibuka atas permintaan dan selalu di belakang. UI: bagian "Card priority" di tab Capsule dengan tombol Up/Down dan Reset. Urutan hanya menentukan susunan carousel dan halaman pertama; fokus ke notifikasi baru tetap sama seperti sebelumnya.
- [x] **F-92 panduan keepalive per OEM** (M7d). `detectOem(manufacturer, brand)` memilih panduan awal (POCO/Redmi dikenali lewat brand, Realme didahulukan dari Oppo karena bisa melapor keduanya); `OemGuides.guideFor` memberi langkah khusus tiap merek ditambah satu langkah umum (Battery → Unrestricted) dan catatan bahwa nama menu beda antar versi. Kartu "Keep the capsule running" ada di tab Status dengan chip untuk melihat merek lain, serta tombol App info, daftar optimasi baterai (`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, tanpa izin tambahan) dan, khusus Xiaomi, layar Autostart lewat nama komponen dengan fallback ke App info bila tidak ada. Isi langkah ditulis dari pengetahuan umum tiap merek dan **belum diverifikasi di perangkat fisik**; acceptance 24 jam di Xiaomi tetap harus dijalankan.
- [x] **F-08 profil layar** (P1). Kalibrasi kini per panel × orientasi: `ScreenProfileKey(sisi pendek px, sisi panjang px, orientasi)`, sehingga memutar ponsel tetap panel yang sama sedangkan ganti resolusi (FHD+ ↔ QHD+) atau membuka lipatan memberi profil lain. `Settings.profiles` menyimpan geometri tiap profil (kunci `geometry.profiles`, satu baris per profil; baris rusak dilewati); `portrait`/`landscape` lama tetap sebagai **templat** untuk layar tanpa profil, jadi kalibrasi yang sudah ada tidak hilang dan profil baru dimulai dari sana. Maks 12 profil, yang terlama dilupakan. Ukuran panel dibaca dari `maximumWindowMetrics` (`ScreenSize`) oleh service dan oleh app pendamping, jadi menyunting dari app tetap mengenai profil yang benar. Tab Capsule menampilkan ukuran layar dan apakah sudah punya profil sendiri. Belum dicoba di perangkat dengan resolusi yang bisa diganti.
- [x] **F-09 mode kapsul** (P1). `PlacementMode.CAMERA` (bawaan, perilaku lama) vs `INDEPENDENT`: kapsul di tengah layar tepat di bawah status bar (`INDEPENDENT_GAP_DP` = 4), tidak terikat lubang kamera, dan karena tidak menyentuh tepi atas, semua gestur andal. Offset user tetap berlaku di kedua mode. Tinggi status bar dibaca dari `windowInsets` di `OverlayWindowController`. `CardWidthMode.CONTENT` mempertahankan lebar ringkas (maks 360 dp, ukuran yang dirancang untuk kartu), `FULL` selebar layar dikurangi gutter. Radius sudut kartu bisa diatur (8–40 dp, bawaan 32) menggantikan konstanta `ExpandedCornerRadius`. Target kalibrasi merah disembunyikan di mode mandiri. Bagian "Capsule mode" di tab Capsule. Padding zona mati (§2.4.3 poin 6): di mode CAMERA kartu yang terbuka bertambah tinggi sebesar bagian status bar yang belum dilewati tepi atas kapsul (`WindowPlacement.contentTopInsetPx`), dan kontennya mulai di bawahnya; mode mandiri dan kapsul tertutup tidak terpengaruh. Region sentuh kartu terbuka kini mengikuti tinggi jendela. **Sengaja tidak dibuat**: `miniScale`/`miniAlignment`/`fadeWhenNoCutout` hanya ada di sketsa skema setelan (§2.6), bukan di syarat F-09 maupun fitur lain. Belum dicoba di perangkat.
- [x] **F-86 palet siang/malam** (P1, sebagian). `resolveAppearance(settings, night)` (murni, dites) memilih latar, outline, dan shadow menurut mode terang/gelap sistem (`isSystemInDarkTheme`): latar malam terpisah (`customBackgroundNight`, jatuh ke latar siang bila kosong), `outlineInDark` (min. 1 dp, hanya saat gelap), `shadowInLight` (min. 3 dp, hanya saat terang; keduanya hanya menambah, tidak mengurangi pilihan F-81), transparansi latar 0–60% (`shellAlpha`, hanya alfa latar kapsul), dan `cutoutGradient` (hitam di lubang kamera memudar ke warna latar). **Tidak dibuat**: warna *foreground* per varian dan latar yang benar-benar terang. Kartu media, notifikasi, timer, dan pengisian melukis gradien gelapnya sendiri dan memakai teks putih tetap, sehingga varian siang terang butuh desain ulang tiap kartu; latar tetap dipaksa cukup gelap (`readableShellColor`). Transparansi tampak di pil dan kartu statis, bukan di kartu yang melukis latarnya sendiri. Belum dicoba di perangkat.

Acceptance di device:
- [ ] Shape, warna kustom, border, shadow, gaya animasi, reduce motion terlihat langsung di overlay.
- [ ] Kecepatan animasi & intensitas bounce berubah terasa; palet siang/malam berganti saat tema sistem berganti.
- [ ] Profil layar: ganti resolusi layar (mis. FHD+ ↔ QHD+ di Samsung) atau rotasi → geometri yang tersimpan untuk kombinasi itu dipakai.
- [ ] Mode "mandiri" → kapsul di bawah status bar dan swipe di bagian atas kartu bekerja.
- [ ] Per-app hide list bekerja; lock screen 3 mode bekerja.
- [ ] Ikuti panduan keepalive di Xiaomi → service tetap hidup 24 jam (cek dengan log waktu terakhir `onAccessibilityEvent`).
- [ ] Urutan prioritas kartu yang diatur user dipatuhi.

### M8 — Polish & rilis
- [ ] Profiling: RAM idle, baterai 1 jam, jank expand/collapse — semua memenuhi §1.3 di 3 device matriks.
- [x] Aksesibilitas, bagian kode (M8b). **Kapsul**: gestur (geser, ketuk dua kali, tekan lama) tidak bisa dipakai TalkBack, jadi kapsul kini punya semantics (`ui/overlay/CapsuleSemantics.kt`): saat tertutup menjadi satu elemen yang membacakan isinya (`capsuleSummary`, patuh aturan privasi lock screen yang sama dengan kartu) dan mengumumkan perubahan secara sopan (`LiveRegionMode.Polite`); aksi *Buka/Tutup*, *Kartu berikutnya/sebelumnya*, *Putar/jeda* dijalankan lewat `UserIntent.Perform(action)` yang memakai jalur reducer yang sama dengan gestur tetapi **mengabaikan binding gestur user**, supaya pengguna TalkBack tidak terkunci bila tap-nya di-rebind. Saat terbuka, isi kartu tetap bisa difokus satu per satu. **Kontrol**: slider kartu kustom punya `progressBarRangeInfo` + `setProgress` dan nama; semua `clickable` punya `role`; baris switch di app pendamping digabung (`toggleable`, "judul, sakelar, aktif"); slider pengaturan punya nama dan nilai berunit; swatch warna jadi radio bernama dengan target sentuh ≥48 dp. **Font besar**: teks overlay berhenti membesar di 1,3× (ukuran kartu tetap harus pas dengan lubang kamera); di app pendamping pemilih segmented berubah jadi chip yang membungkus di atas 1,3×. **RTL**: `supportsRtl` sudah aktif dan Compose memakai start/end; build debug menambah pseudo-locale (en-XA, ar-XB) untuk uji. Posisi dan arah geser kapsul sengaja tetap fisik (menempel ke lubang kamera). **Belum diverifikasi di perangkat**: pembacaan TalkBack, tampilan font besar, dan tampilan RTL belum dicoba; lint menandai `localeConfig` memakai "id" sedangkan folder `values-in` (nama lama Android untuk Indonesia, seharusnya setara), perlu dicek di HP.
- [ ] Screenshot test semua kartu; accessibility (TalkBack membaca isi kartu di app pendamping), RTL, font besar.
- [x] Terjemahan EN + ID (M8a). Semua teks UI (±550 string) dipindah ke `res/values/strings_*.xml` dengan padanan `values-in`. Teks dari kode tanpa Context (validator barcode, hasil scan, validasi/eksekusi intent) memakai `UiText` (`core/UiText.kt`: id resource + argumen, atau `Raw` untuk pesan sistem) supaya tetap murni dan tes menilai id/argumen, bukan kata-kata. Enum yang tampil di UI memakai `@StringRes labelRes`; `StaticCardType.label` tetap kunci bawaan Inggris yang tidak ditampilkan (label bawaan disimpan sebagai kunci itu agar mengikuti bahasa app). Teks event mini dibuat saat event terjadi lewat `context.getString`. Bahasa app bisa dipilih per-app lewat `locales_config` (en, id). `StringResourcesTest` menjaga kunci EN/ID sama dan placeholder-nya sama. Nama teknis (format barcode, istilah intent) sengaja tidak diterjemahkan. **Belum**: tinjauan terjemahan ID oleh penutur asli dan uji tampilan di bahasa ID (teks lebih panjang bisa terpotong).
- [~] Play Console (M8c): draf ada di [PLAY_CONSOLE.md](PLAY_CONSOLE.md) dan [PRIVACY_POLICY.md](PRIVACY_POLICY.md) (deklarasi aksesibilitas dipetakan ke kode, skrip video, justifikasi izin, draf Data safety). Backup: DataStore dikecualikan dari cloud-backup tetapi ikut device-transfer (aturan di `res/xml/data_extraction_rules.xml`). **Belum selesai**: jawaban Data safety tidak boleh "tidak mengumpulkan data" sebelum pengungkapan data ML Kit / Google Play services diverifikasi. Video disclosure belum direkam di perangkat.
- [ ] Release build (R8) diuji ulang seluruh acceptance M0–M7 secara cepat (smoke).

### Backlog P2 (setelah rilis)
F-07 screen corner radius · F-42 live notification generik · F-58 widget · F-77 pencarian app · F-96 preset device · F-66 USB/OTG · F-67 AirPods · F-74 intent masuk · F-95 backup/restore.

## 3.4 Risiko

| Risiko | Dampak | Mitigasi |
|---|---|---|
| Play menolak penggunaan AccessibilityService | Tidak bisa rilis | Disclosure sesuai policy sejak M0, config minimal, video deklarasi; siapkan distribusi alternatif (GitHub/F-Droid) |
| OEM mematikan service | Kapsul hilang acak | Panduan keepalive per OEM, deteksi & notifikasi user saat service mati (dari app pendamping) |
| Format notifikasi app jam/download berubah | Timer/progress rusak | Parser berbasis field standar (chronometer, progress), test fixture per versi, fallback ke tampilan notifikasi biasa |
| Insets fullscreen tidak terbaca di overlay pada OEM tertentu | Kapsul menutupi video/game | Fallback deteksi via `AccessibilityWindowInfo` + per-app hide list |
| Hidden API baterai Bluetooth tidak tersedia | Baterai TWS tidak tampil | Tampilkan tanpa persen; AirPods via BLE di P2 |
| Background activity launch diblokir dari overlay | Pintasan/tap kartu tidak membuka app | Uji di Android 15+ sejak M1 (tap kartu media membuka player) |
| Brightness max berbeda per OEM | Slider tidak akurat | Wizard kalibrasi + gamma mapping |
| Android 17 Advanced Protection mematikan layanan aksesibilitas non-tool | Kapsul tidak bisa jalan untuk user yang mengaktifkan mode itu | F-97: deteksi & jelaskan ke user; tidak ada workaround — jangan coba mengakali |

---

# BAGIAN 4 — PANDUAN EKSEKUSI UNTUK CODING AGENT

Gunakan satu sesi agent **per milestone**. Template prompt:

```text
Kamu software engineer Android (Kotlin + Jetpack Compose) di repo /Users/iqbalfauzi/Personal/App/capsule
(package com.iqbalwork.capsule).

Baca docs/CAPSOOL_PRD_SPEC_PLAN_V3.md, terutama §2 (spec) dan milestone <MX> di §3.3.

Kerjakan HANYA milestone <MX>. Aturan:
- Ikuti arsitektur §2.2: reducer pure, side effect lewat EffectHandler, data dari Source sistem.
- Jangan menaruh data simulasi/hardcode di src/main. Simulator hanya di src/debug.
- Jangan menambah izin di manifest selain yang tercantum di §1.6 untuk fitur milestone ini.
- Tambahkan unit test untuk reducer/parser baru.
- Jalankan ./gradlew testDebugUnitTest assembleDebug lintDebug sampai lulus.
- Perbarui checklist <MX> di dokumen: centang hanya pekerjaan kode yang selesai; biarkan
  "Acceptance di device" tidak dicentang (itu diverifikasi manusia di HP).
- Di akhir, tulis ringkasan: apa yang selesai, apa yang belum, apa yang perlu dites manual.
- Commit dengan pesan yang menjelaskan isi sebenarnya, format: feat(<area>): <ringkas> (M<X>).
```

Verifikasi di HP dilakukan manual (atau oleh agent dengan akses `adb` + device terhubung), lalu hasilnya dicatat di checklist acceptance beserta model device & versi Android.

---

# LAMPIRAN A — Catatan riset pasar (v3.1)

Daftar fitur v3.1 dilengkapi dari inspeksi **level fitur** sebuah aplikasi island-style yang sudah ada di Play Store: hanya manifest (izin, komponen, konfigurasi service) dan teks UI yang terlihat oleh pengguna. **Tidak ada** dekompilasi kode, tidak ada kode/aset/teks yang disalin, dan tidak ada mekanisme lisensi yang diakali. Semua implementasi Capsool ditulis sendiri berdasarkan spesifikasi di dokumen ini dan dokumentasi publik Android.

Temuan yang memengaruhi keputusan:

| Temuan | Keputusan di Capsool |
|---|---|
| `minSdk` 32 | `minSdk` 31 (§2.1) |
| Layanan aksesibilitas: `flagRetrieveInteractiveWindows`, `canRetrieveWindowContent`, `canPerformGestures`, tidak tampil di lock screen | Konfigurasi minimal kita (§2.4.1) sejalan; `canPerformGestures` tetap `false` sampai ada fitur yang butuh |
| Notification listener dengan `default_filter_types = conversations\|alerting\|ongoing\|silent` | Diadopsi (§2.5.2) |
| Heads-up ditangani dengan memandu user mematikan pop-up per app | F-26 |
| Model kartu dinamis vs kartu statis, gesture dua konteks, intent builder | §1.4 A (paragraf dua jenis kartu), F-75, F-76 |
| Profil layar per resolusi & orientasi, mode kapsul menyatu/mandiri, zona mati tepi atas | F-08, F-09, §2.4.3 |
| Peringatan Advanced Protection Android 17 | F-97, risiko §3.4 |
| Memakai `QUERY_ALL_PACKAGES`, `READ_CONTACTS`, `CAMERA`, `WRITE_SECURE_SETTINGS`, `INTERNET`, analytics & iklan | **Tidak diadopsi.** Capsool memakai `LauncherApps`, contact picker, Code Scanner Play services, dan tanpa internet (§1.6) |
