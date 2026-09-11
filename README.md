# Android Desktop Bridge

Android Desktop Bridge ialah projek untuk membawa pengalaman desktop-style kepada telefon Android melalui monitor luaran.

## V1 — Lightweight Adaptive Desktop UI

- Desktop shell Android yang ringan
- Taskbar / dock minimal
- Window manager ringkas
- Mouse dan keyboard
- Tiada blur/transparency/animasi berat
- Sasaran utama 1280×720 (720p), 16:9
- Resolusi dan UI scaling boleh menyesuaikan kemampuan telefon, bridge dan monitor
- Profil Lite, Balanced dan Performance
- Penggunaan hardware video encoder jika tersedia
- Aplikasi Android kekal menggunakan aplikasi/perkakasan sebenar telefon

## V1.1 — Real MediaProjection + H.264

- User consent melalui Android MediaProjection
- Foreground capture service bertipe `mediaProjection`
- VirtualDisplay ke encoder Surface
- H.264 hardware encoder melalui MediaCodec
- 1280×720, 30 FPS dan 4 Mbps sebagai konfigurasi permulaan
- Encoder capability detection
- Callback untuk encoded access units dan output format
- Clean shutdown bila projection dihentikan

## Device-aware

Sistem mengambil kira RAM, CPU/GPU, hardware encoder, USB capability, monitor dan beban sistem sebelum memilih konfigurasi desktop. Resolusi yang tidak disokong tidak akan dipaksa.

## Permission & Consent

Aplikasi meminta hanya permission yang diperlukan. MediaProjection memerlukan persetujuan pengguna untuk setiap sesi capture pada Android moden. Akses Accessibility, USB atau device-control juga memerlukan persetujuan melalui mekanisme Android yang sesuai. Tiada akses penuh secara senyap dan fungsi asas tidak memerlukan root.

## Architecture

```text
Android Phone
     |
     | USB-C
     v
Desktop Bridge App
     |
     | MediaProjection
     v
VirtualDisplay
     |
     | Surface
     v
Hardware H.264 Encoder
     |
     | Encoded video
     v
USB Video Transport / Receiver  ← V1.2
     |
     | HDMI / DisplayPort
     v
Portable Monitor
```

## Status

🚧 V1.1 capture pipeline implemented. USB video transport / receiver is planned for V1.2.

## Prinsip

1. Ringan
2. Responsif
3. Adaptive mengikut spesifikasi telefon
4. Minimum visual effects
5. Permission-first
6. Tidak memerlukan root untuk fungsi asas
