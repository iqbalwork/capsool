# M6 — Acceptance di emulator (2026-10-16)

Catatan jujur: ini **bukan** pengganti uji di HP. Angka dan gambar di bawah datang dari emulator
`Pixel_10_Pro_Fold` (image `google_apis_playstore`, API 37, x86_64, layar 1080x2364 @ 390 dpi),
bukan perangkat nyata. Beberapa hal hanya bisa dinilai mata di HP — daftarnya di bagian akhir.

APK yang diuji dibangun ulang dari sumber di `ebd1b4b` (branch `feature/m6`, == remote) supaya
artefak yang diuji benar-benar cocok dengan commit yang di-push, bukan sisa build lama.

## Yang terbukti bekerja

| Yang diuji | Hasil | Bukti |
|---|---|---|
| Pemasangan & proses hidup | ya | `adb install` Success; PID 4474 hidup |
| Service aksesibilitas ter-bind | ya | `dumpsys accessibility` → `Bound services{label=Capsool}` |
| Kapsul tergambar saat idle | ya | window `Capsool` `bounds=Rect(885,0-1078,84)` → pill 193x84 px (79x34 dp) |
| Kapsul menempel di cutout | ya | cutout perangkat ada di sisi **kanan**; kapsul ikut ke kanan, bukan memaksa ke tengah |
| Tap membuka kapsul | ya | `bounds` berubah `885,0-1078,84` → `593,0-1078,474` |
| Long press membuka area kartu statis | ya | muncul kartu dengan teks dari kode kita sendiri, `ExpandedShortcutsCard.kt:71` |
| Akses notifikasi | ya | tab Status hijau: "On. The capsule can follow music and video players." |
| Setelan Carousel/List tersimpan | ya | memilih List → `strings` pada `settings.preferences_pb` memuat `LIST` |

Jalur setelan uji: tab **Cards** → `View mode` → **List** → nilai benar-benar tertulis ke DataStore
di `/data/data/com.iqbalwork.capsule/files/datastore/settings.preferences_pb`. Jadi rantai
UI → Settings → penyimpanan utuh.

## Yang belum bisa diuji di emulator, dan sebabnya

**Mode List belum pernah terlihat dengan kartu sungguhan.** Tinggi panel tetap 474 px (194,5 dp)
baik dalam mode Carousel maupun List, padahal aritmetika kode memperkirakan 204 dp (497 px) untuk
satu baris. Setelah ditelusuri, ini **bukan cacat**: pada instalasi bersih
`Settings.effectiveStaticCards` kosong (tidak ada shortcut, isi barcode kosong, tidak ada custom
intent), sehingga `pages` kosong, syarat ketiga `showsStaticList` (`pages.any { it is CardPage.Static }`)
gagal, `listRows` bernilai 0, dan tinggi panel memang kembali ke nilai carousel. Isi yang terlihat
di kapsul adalah **empty state bawaan overlay**, bukan kartu statis.

Untuk menguji List dengan sungguhan, kapsul harus punya minimal satu kartu statis nyata — yaitu
dengan mengisi shortcut, isi barcode, atau custom intent lebih dulu.

## Temuan yang perlu diperhatikan di HP

1. **Wilayah sentuh kapsul bertumpuk dengan status bar.** Kapsul menempati `y 0..84`, status bar
   `y 0..127`. Di emulator ini tap/long press di wilayah itu membuka shade notifikasi, bukan
   kapsul; shade kemudian menutupi kapsul karena keduanya sama-sama berlabuh di sudut kanan.
   Setelah SystemUI dimatikan sementara, sentuhan sampai ke kapsul dan kapsul membuka seperti
   seharusnya — jadi perangkatnya yang menerima sentuhan, bukan kapsulnya yang rusak. Perlu
   dipastikan di HP asli bahwa kapsul tetap bisa disentuh saat status bar hidup.
2. **Kartu melebar terpotong tepi layar.** Karena cutout perangkat ini berada dekat tepi kanan,
   kapsul jangkar ke situ dan kartu selebar 360 dp terpotong layar; hanya ~485 px yang terlihat.
   Ini konsekuensi wajar dari cutout tak-simetris, bukan salah hitung — tapi di HP dengan cutout
   di tepi, tampilan akan seperti ini.
3. **Notifikasi yang dipos lewat `adb shell cmd notification post` tidak muncul di kapsul**,
   padahal akses notifikasi menyala. Belum diselidiki; bisa jadi karena notifikasi dari
   `com.android.shell` disaring, atau karena kartu notifikasi butuh syarat lain.

## Bahan yang berguna untuk uji di HP

Tab **Debug** (build debug saja) berisi pemicu event palsu yang membuat pengujian tidak perlu
aplikasi nyata: media session, notifikasi chat dengan tombol balas, unduhan berprogres,
timer 2 menit, colokan charger, baterai lemah, mode getar, Wi-Fi, dan status media.

## Cara mengembalikan emulator

Pengujian ini sempat mematikan SystemUI agar sentuhan tidak direbut status bar. Sudah
dikembalikan (`pm enable --user 0 com.android.systemui`, `settings delete global policy_control`).
