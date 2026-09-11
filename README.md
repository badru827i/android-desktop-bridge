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

## Device-aware

Sistem mengambil kira RAM, CPU/GPU, hardware encoder, USB capability, monitor dan beban sistem sebelum memilih konfigurasi desktop. Resolusi yang tidak disokong tidak akan dipaksa.

## Permission & Consent

Aplikasi meminta hanya permission yang diperlukan. Akses seperti MediaProjection, Accessibility, USB atau device-control memerlukan persetujuan pengguna melalui mekanisme Android yang sesuai. Tiada akses penuh secara senyap dan V1 tidak memerlukan root untuk fungsi asas.

## Architecture

```text
Android Phone
     |
     | USB-C
     v
Desktop Bridge App
     |
     | Screen Capture / Hardware Encode
     v
USB Video Transport / Receiver
     |
     | HDMI / DisplayPort
     v
Portable Monitor
```

## Status

🚧 V1 sedang dibangunkan.

## Prinsip

1. Ringan
2. Responsif
3. Adaptive mengikut spesifikasi telefon
4. Minimum visual effects
5. Permission-first
6. Tidak memerlukan root untuk fungsi asas
