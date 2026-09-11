# Android Desktop Bridge — Android V1.1

V1.1 adds the real Android screen-capture pipeline.

## V1.1 pipeline

```text
User consent
    ↓
MediaProjection
    ↓
VirtualDisplay
    ↓
MediaCodec Surface input
    ↓
Hardware H.264 encoder
    ↓
Encoded access units
    ↓
USB transport (V1.2)
```

## Included

- Android MediaProjection consent flow
- Foreground service with `mediaProjection` service type
- 1280×720 default capture target
- 30 FPS default
- 4 Mbps default H.264 bitrate
- Hardware/software capability discovery through MediaCodecList
- Surface-input H.264 encoding
- Key-frame and encoded-byte callbacks
- Clean shutdown when the user/system stops projection
- No root required

## Important

V1.1 produces an encoded H.264 stream inside the Android app. It does **not** yet turn that stream into HDMI/DisplayPort. USB packet transport and the external receiver/decoder are V1.2 work.

Android 14+ requires user consent for each MediaProjection capture session. The service must declare the `mediaProjection` foreground-service type and the matching foreground-service permission.

## Build

Open `android-app` as an Android Studio project and build the `app` module.

Minimum Android version: API 26.
Target SDK: API 35.
