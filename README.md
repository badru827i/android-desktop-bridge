# Android Desktop Bridge

Android Desktop Bridge ialah projek untuk membawa pengalaman desktop-style kepada telefon Android melalui monitor luaran.

## Konsep

Telefon kekal sebagai peranti utama yang menjalankan aplikasi, CPU/GPU, RAM, storan dan sambungan rangkaian.

Monitor luaran digunakan sebagai paparan desktop, manakala sistem bridge mengurus paparan dan input antara telefon dengan monitor.

## Sasaran V1

- Desktop-style UI yang ringan
- Taskbar / dock
- Sokongan mouse dan keyboard
- Aplikasi dalam tetingkap
- Fokus pada respons pantas dan latency rendah
- Sasaran paparan 16:9, terutama 1080p
- Mod Balanced, Performance dan Battery Saver

## Architecture awal

```text
Android Phone
     |
     | USB-C
     v
Desktop Bridge / Transport
     |
     | HDMI / DisplayPort
     v
Portable Monitor
```

## Status

🚧 V1 sedang dibangunkan.

## Nota teknikal

Telefon yang tidak menyokong native USB-C DisplayPort Alt Mode memerlukan seni bina aktif untuk menghantar video ke monitor. Reka bentuk V1 akan mengkaji pendekatan USB video transport / receiver yang sesuai.
