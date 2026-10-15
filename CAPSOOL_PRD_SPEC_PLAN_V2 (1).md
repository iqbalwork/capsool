# Capsool: Dynamic Island for Android — PRD, Technical Specification & Execution Plan (v2.1 Full Parity)

---

# BAGIAN 1: PRODUCT REQUIREMENT DOCUMENT (PRD)

## 1.1 Visi Produk & Benchmark
**Capsool** adalah re-implementasi clean-room dari aplikasi **Material Capsule: Island Style** (`com.pryshedko.mtisland` v15.9), dibangun di atas **Jetpack Compose + Material Design 3 Expressive**.
Tujuan: Mencapai **100% Feature Parity** terhadap seluruh kapabilitas MT Island (termasuk fitur Pro/Premium yang terkunci di versi aslinya), tanpa iklan, tanpa DRM/packer PairIP, dan dengan efisiensi memori tingkat tinggi.

---

## 1.2 Matriks Perbandingan & Daftar Lengkap Fitur (100% Parity)

Berikut adalah audit seluruh fitur dari `com.pryshedko.mtisland` yang diimplementasikan di Capsool:

### A. Tipe Kartu Dinamis (Dynamic Cards Stack & Carousel)
1. **Media Player Card**:
   - Mode Compact (masuk dalam carousel swipeable) & Mode Full-Size (seek bar permanen + volume bar).
   - Cover album berputar (*rotating album art*) di Mini Capsule saat lagu aktif.
   - Animasi sintetis visualizer equalizer di Mini Capsule.
   - Ekstraksi warna dinamis (Material You / Monet) dari artwork album untuk latar belakang kapsul.
   - Auto-hide timeout: Instan, 5s, 15s, 30s, 60s, 2m, auto-hide saat pause, atau saat aplikasi musik sedang di foreground.
   - Fallback cover art & metadata khusus Spotify, YouTube Music, dan pemutar lokal.
2. **System Sliders Card**:
   - **Volume Slider**: 3 opsi gaya visual:
     * Android 15 (Single continuous pill).
     * Android 16 (Split segmented pills).
     * Samsung One UI (Rounded slider).
     * Toggle cepat Ringer Mode: Normal $\leftrightarrow$ Getar $\leftrightarrow$ Senyap/DND.
   - **Brightness Slider**: Slider kecerahan dengan wizard kalibrasi manual Android 13+ (`WRITE_SETTINGS`), serta toggle Adaptive Brightness.
   - **Flashlight Slider**: Toggle senter kamera + slider level kekuatan cahaya senter (Camera2 API `turnOnTorchWithStrengthLevel`).
3. **Charging & Battery Card**:
   - Deteksi AC Charging, USB Charging, Wireless Charging, dan Full Charged.
   - Pembacaan persentase baterai real-time, status temperatur baterai, dan peringatan Low Battery ($\le 5\%$).
4. **Live Progress Card (Download/Upload)**:
   - Tracking progress bar unduhan/unggahan sistem dan aplikasi (Google Play Store, Telegram, IDM, Browser) secara real-time.
5. **Timer & Stopwatch Card**:
   - Integrasi sinkronisasi dengan Google Clock (`com.google.android.deskclock`).
   - Tampilan live countdown timer / stopwatch aktif dengan tombol aksi (+1 menit, pause/resume, dismiss).
6. **Shortcuts & Barcode Card**:
   - **App Shortcuts**: Peluncur aplikasi favorit dengan ikon Themed Material You atau ikon asli.
   - **Contact Shortcuts**: Panggilan langsung (`CALL_PHONE`) atau chat WhatsApp ke kontak terpilih.
   - **Barcode & QR Code**: Penyimpanan dan visualisasi loyalty card via Google ML Kit (format: EAN-13, EAN-8, Code 128, Code 39, QR Code, UPC-A, UPC-E).
   - **Pintasan ChatGPT**: Tombol panggil instan ke aplikasi OpenAI ChatGPT (`com.openai.chatgpt`).
   - **Pintasan Sistem**: Wi-Fi, Bluetooth, Tampilan, Lokasi, Baterai.
7. **Notification & Quick Reply Card**:
   - Pengalihan notifikasi Heads-up ke dalam kapsul (Mode Minimized vs Mode Expanded).
   - Efek Glow pendaran cahaya di pinggir kapsul sesuai warna dominan ikon aplikasi pengirim.
   - Avatar kontak 42dp + badge aplikasi (WhatsApp `#25D366`, Telegram `#24A1DE`).
   - Tombol aksi cepat: "Reply" (inline reply via RemoteInput), "Mark as read", "Mute/Dismiss".
8. **AppWidget Container Card**:
   - Penanaman widget Android pihak ketiga ke dalam kapsul via `AppWidgetHost` (`BIND_APPWIDGET`).
9. **Otomasi & Broadcast Intent**:
   - Pengiriman sinyal *Broadcast Intent* saat kapsul disentuh / digeser untuk integrasi Tasker, MacroDroid, atau Automate.

### B. Mini Capsule Events (Animasi Mikro 2–4 Detik)
- **TWS & Bluetooth**: Menampilkan nama earphone + persentase baterai saat tersambung.
- **Dukungan AirPods**: Integrasi pembacaan baterai AirPods.
- **Hardware USB/OTG**: Indikator saat flashdisk/kabel OTG dicolokkan atau dicabut.
- **Wi-Fi State**: Indikator saat Wi-Fi tersambung atau terputus.
- **Mode Dering**: Transisi visual saat profil dering berganti (Ring, Vibrate, Silent).
- **Mode Pesawat**: Peringatan aktivasi Airplane Mode.
- **Device Unlock**: Animasi penyambutan saat layar dinyalakan dan dibuka kunci.

### C. Kalibrasi Cutout & Layar
- Penyesuaian posisi kamera depan: Tengah (*Center*), Kiri (*Left*), Kanan (*Right*), atau koordinat kustom X & Y.
- Pengaturan Width, Height, dan Corner Radius kapsul.
- **Interactive Red Target Calibrator**: Lingkaran target merah `#FF3B30` interaktif yang tampil tepat di atas kamera depan untuk mempermudah penyelarasan fisik.
- Screen Corner Radius adjustment (menyamarkan radius lengkungan sudut layar).
- Panduan *Keepalive* khusus perangkat Xiaomi / HyperOS / MIUI.

### D. Fitur Pro Unlocked (Tanpa Batasan)
- **Unlimited Active Cards**: Tidak ada batasan jumlah kartu yang dapat aktif di stack carousel.
- **Full Custom Palette**: Pilihan kustomisasi warna latar belakang, border (0–3dp / filled), shadow elevation, dan gradient cutout.
- **Opacity Slider Kustom**: Opsi transparansi idle 0% (rekomendasi OLED: 0 draw calls / aman dari burn-in), 20%, 40%, 60%, 80%, hingga 100%.
- **5-Way Global Gestures**:
  - Single Tap, Double Tap, Long Press, Swipe Left, Swipe Right.
  - Aksi kustom: Switch Card, Screenshot, Lock Screen, Open Notification Shade, Open Quick Settings, Flashlight.

---

# BAGIAN 2: TECHNICAL SPECIFICATION & ARCHITECTURE

## 2.1 Arsitektur Layanan Sistem

1. **`CapsoolAccessibilityService`**:
   - `android:accessibilityFlags="flagRetrieveInteractiveWindows"`
   - `android:canPerformGestures="true"`
   - Menambahkan `ComposeView` via `WindowManager.addView` dengan `TYPE_ACCESSIBILITY_OVERLAY`.
   - **CATATAN KRITIKAL**: Jangan lakukan pengecekan `Settings.canDrawOverlays(this)` di AccessibilityService karena tipe window aksesibilitas sudah diotorisasi langsung oleh izin aksesibilitas.
2. **`CapsuleNotificationListenerService`**:
   - Filter `conversations|alerting|ongoing|silent`.
   - Menangkap `MediaSessionManager` active sessions, `RemoteInput` actions, dan `Notification.EXTRA_PROGRESS`.
3. **`HardwareStateTracker` & `BatteryBroadcastReceiver`**:
   - Memantau `ACTION_POWER_CONNECTED`, `ACTION_POWER_DISCONNECTED`, `ACTION_BATTERY_CHANGED`.
   - Memantau `RINGER_MODE_CHANGED_ACTION`, `BluetoothDevice.ACTION_ACL_CONNECTED`.

---

## 2.2 Token Desain M3 Expressive

```kotlin
object CapsuleTokens {
    // Warm Dark Brown M3 Expressive Palette
    val Surface = Color(0xFF1A1615)
    val SurfaceContainerLowest = Color(0xFF120F0E)
    val SurfaceContainer = Color(0xFF231F1E)
    val SurfaceContainerHigh = Color(0xFF292423)
    val SurfaceContainerHighest = Color(0xFF342E2D)
    val Primary = Color(0xFFFFB690)             // Warm Peach
    val OnPrimary = Color(0xFF542200)
    val PrimaryContainer = Color(0xFF733512)
    val OnSurface = Color(0xFFEDE0DC)
    val OnSurfaceVariant = Color(0xFFD0C4BE)
    val Outline = Color(0xFF998E89)
    val OutlineVariant = Color(0xFF4E4440)
    val CalibratorRed = Color(0xFFFF3B30)       // Apple Target Red
    val WhatsAppBadge = Color(0xFF25D366)
    val TelegramBadge = Color(0xFF24A1DE)

    // Motion Springs
    val FruitSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 380f)
    val FruitSpringDp = spring<Dp>(dampingRatio = 0.72f, stiffness = 380f)
    val M3ElasticMotion = spring<Float>(dampingRatio = 0.62f, stiffness = 520f)
}
```

---

# BAGIAN 3: EXECUTION PLAN & CHECKLIST

## Tahapan Pekerjaan (Milestones)

### Milestone 1: Core Overlay & 5-Way Gestures
- [ ] Hapus check `canDrawOverlays` di `CapsoolAccessibilityService`.
- [ ] Implementasikan deteksi gesture 5-arah di `CapsuleRootView.kt` (Single Tap, Double Tap, Long Press, Swipe Left, Swipe Right) lengkap dengan haptic feedback.
- [ ] Sinkronkan `RedTargetCalibratorView.kt` dengan `CutoutPreferencesRepository`.

### Milestone 2: Dynamic Cards Carousel & Micro-Events
- [ ] Implementasikan horizontal carousel di `CardStackManager.kt` agar swipe kiri/kanan memutar kartu aktif secara mulus.
- [ ] Sambungkan kartu:
  - `ExpandedMediaCard`: scrubber + cover art rotating + equalizer wave.
  - `ExpandedSliderCard`: Volume (pilihan 3 style: A15, A16, OneUI), Brightness, Flashlight.
  - `ExpandedChargingCard`: Persentase dan indikator AC/USB/Wireless.
  - `ExpandedNotificationCard`: Action buttons reply, read, dismiss.
  - `ExpandedTimerCard`: Sinkronisasi Google Clock.
  - `ExpandedShortcutsCard`: App & Contact shortcuts + ML Kit Barcode view.
- [ ] Implementasikan Mini Capsule Events (pop-up 3-5 detik saat TWS connect, charger dicolok, dering ganti).

### Milestone 3: Companion App & Diagnostics Polish
- [ ] Companion App 3 Tab (`FeaturesTab`, `CapsuleSettingsTab`, `AppSettingsTab`) berfungsi 100%.
- [ ] Tombol diagnostik untuk memicu simulasi semua event (Charging, TWS, Notifikasi) langsung dari tab pengaturan.

---

# BAGIAN 4: PROMPT RESMI UNTUK ANTIGRAVITY (AGY)

Jalankan prompt berikut di terminal untuk mengeksekusi implementasi:

```bash
agy -p 'Kamu bertugas sebagai software engineer Android (Kotlin + Jetpack Compose) di repository /home/iqbalf/Projects/MaterialCapsule.

BACA SPESIFIKASI:
Baca file: /home/iqbalf/Projects/MaterialCapsule/spec/CAPSOOL_PRD_SPEC_PLAN_V2.md

FOKUS PENGERJAAN (100% PARITY DENGAN MATERIAL CAPSULE):
1. ACCESSIBILITY OVERLAY REPAIR (app/src/main/java/com/iqbalf/capsule/engine/accessibility/CapsoolAccessibilityService.kt):
   - Hapus pengecekan `Settings.canDrawOverlays(this)` di `setupAccessibilityOverlay()` karena AccessibilityService menggunakan TYPE_ACCESSIBILITY_OVERLAY yang tidak memerlukan permission SYSTEM_ALERT_WINDOW.
   - Pastikan overlay langsung terpasang saat `onServiceConnected()` dipanggil.

2. GESTURE 5-ARAH LENGKAP (app/src/main/java/com/iqbalf/capsule/ui/CapsuleRootView.kt):
   - Pasang deteksi gestur lengkap di CapsuleRootView:
     * Tap: Toggle Expand / Collapse.
     * Double Tap: Aksi pintasan (Play/Pause atau Toggle Mute).
     * Long Press: Expand ke kartu penuh dengan haptic feedback.
     * Horizontal Drag (Swipe Left / Swipe Right): Kirim event UserAction.SwipeLeft dan UserAction.SwipeRight ke CapsuleStateEngine untuk memindahkan kartu aktif di CardStackManager.

3. CAROUSEL CARD STACK & KARTU LENGKAP (CardStackManager.kt, CapsuleStateReducer.kt, ui/components/):
   - Dukung transisi kartu berikutnya (nextCard / previousCard) saat menerima swipe horizontal.
   - Pastikan seluruh kartu (Media, Sliders [Volume/Brightness/Torch], Charging, Notification, Timer, Shortcuts) aktif di stack carousel.
   - Terapkan animasi spring (FruitSpring) pada transisi kartu.

4. VERIFIKASI & ACCEPTANCE:
   - Jalankan `./gradlew testDebugUnitTest` dan pastikan semua test lulus 100%.
   - Jalankan `./gradlew assembleDebug` dan pastikan build sukses.
   - Commit dengan pesan: `feat(core): implement full 100% feature parity with Material Capsule`' --print-timeout 20m --dangerously-skip-permissions
```
