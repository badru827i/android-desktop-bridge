# Android Desktop Bridge

Android Desktop Bridge membawa pengalaman desktop-style ke telefon Android melalui monitor luaran, dengan telefon kekal sebagai CPU/GPU/RAM/storage/network/camera.

## V1.3 architecture

```text
Android Phone
  -> MediaProjection -> Surface encoder
  -> H.266 if available, else H.265, else H.264
  -> ADBV packetizer
  -> secure paired transport
  -> receiver decoder/display
  -> HDMI / DisplayPort -> portable monitor

Keyboard / Mouse / Touch -> separate authenticated control channel
```

## Included

- Lightweight desktop shell: taskbar, launcher, windows, minimal effects.
- Adaptive Lite/Balanced/Performance profiles based on device memory and runtime codec capability.
- 720p-first design with 480p fallback and 30/60 FPS profiles.
- Runtime codec fallback: H.266 -> H.265 -> H.264. H.266 is optional.
- MediaProjection foreground capture with explicit Android user consent.
- Fixed 32-byte ADBV video packet format with payload limits.
- Pairing/session state with peer blocking and replay/sequence protection hooks.
- Separate keyboard/mouse/touch protocol.
- USB production architecture documented without pretending a passive cable can create unsupported video output.
- Python receiver/parser and optional FFplay development display path.

## USB reality

A normal Android app cannot assume that every USB-C port supports DisplayPort Alt Mode or arbitrary custom USB gadget endpoints. The production design therefore uses either USB networking/tethering for development or a dedicated USB bridge/receiver that terminates the video stream and outputs HDMI/DP.

## Security

Use explicit pairing, authenticated sessions, encrypted production transport, short-lived session state, sequence/replay protection, packet/rate limits, and user-approved Android permissions. An unauthorized peer is rejected and its session can be terminated/blocked; the app does not control the Android OS or physically eject an attacker.

## Status

V1.3 software layers are being assembled in the repository. The Android capture/encoding code is present, but end-to-end physical USB-to-HDMI hardware, receiver firmware, and on-device validation still require the actual bridge hardware/test devices.

## Build

Android project: `android-app/` (compileSdk 35, minSdk 26). Python development receiver: `receiver/python/`.

## Principles

1. Ringan
2. Responsif
3. Adaptive ikut telefon/receiver/monitor
4. Minimum visual effects
5. Permission-first
6. No root required for basic capture
