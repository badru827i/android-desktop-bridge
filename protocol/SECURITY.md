# Android Desktop Bridge Security Model

## Goals

The bridge protects screen content, input events, control commands, and paired-session credentials while keeping Android permissions explicit and user-controlled.

## Local user authentication

1. Starting a desktop session requires Android system authentication.
2. If a supported biometric is available, the system biometric prompt can use fingerprint or face authentication according to the device's enrolled authenticators.
3. Device credential is the fallback: the Android system may accept the user's configured PIN, pattern, or password.
4. The app never receives or stores the fingerprint/face template, PIN, pattern, or password. Verification is performed by Android's system authentication service.
5. Do not implement a custom password vault or biometric database inside the bridge.

Android's `BiometricPrompt` supports biometric authenticators and `DEVICE_CREDENTIAL`; capability should be checked at runtime rather than assumed.

## Trust model

1. A receiver is **untrusted until paired**.
2. Pairing is explicit and initiated by the user on both sides.
3. Only a paired receiver may request a desktop session.
4. A session has a fresh random session identifier and sequence state.
5. Every transport packet is authenticated; production transports should use an encrypted channel such as TLS or an equivalent authenticated key-exchange design.
6. Packets with invalid authentication, malformed headers, stale sequence numbers, unsupported codec IDs, or excessive payload sizes are rejected.

## Unauthorized-session response

The bridge must treat failed authentication, invalid pairing credentials, repeated replay/authentication failures, and unexpected control messages as a security event. It should immediately terminate the current desktop session, stop video/input/control transport, invalidate the session key, and require fresh user authentication and pairing before reconnecting.

The app must not claim to identify or physically "kick out" an attacker from Android itself. The security boundary is the bridge session: an unauthorized peer is denied and disconnected from that session.

## Pairing

The UI should display a short human-verifiable pairing code or QR payload. Do not use a hard-coded shared secret and do not put credentials/API keys in the repository.

A production implementation should derive session keys using an authenticated key agreement and keep long-term keys in the platform secure keystore where available.

## Replay and ordering protection

Each session uses a monotonically increasing packet sequence number plus a fresh session identifier. Receivers must reject duplicate or older sequence values according to the protocol's replay window.

## Input security

Mouse, keyboard, touch, and control commands are separate from video payloads. The app must not enable Accessibility, USB control, MediaProjection, camera, microphone, or other privileged capabilities without the corresponding Android/user consent flow.

## Video payload safety

Video payloads are data only. Receivers must never execute packet contents as code, shell commands, scripts, or file paths. Decoder errors terminate or reset the session instead of being treated as commands.

## Resource limits

Receivers should enforce limits on header size, payload size, frame dimensions, frame rate, queue depth, and decode time to reduce memory-exhaustion and denial-of-service risk.

Recommended initial limits:

- Maximum packet payload: 16 MiB
- Maximum frame rate: 60 FPS
- Maximum session queue: implementation-defined bounded queue
- Reject unsupported codec IDs before allocation/decoder setup
- Use bounded authentication failure counters and short lockout/backoff windows

## USB security

USB devices are selected explicitly by the user. Do not automatically trust a newly attached device. Production USB transport should authenticate the receiver before screen/input streaming begins.

## Privacy

The bridge should make active capture obvious to the user, keep MediaProjection in the required foreground-service flow, and stop capture when the user ends the session or Android revokes the projection.

## Development transport

The current TCP receiver is a development adapter. It should remain localhost-only by default. Do not expose it to a LAN or the public internet without transport authentication and encryption.

## Secrets

Never commit private keys, pairing secrets, access tokens, API keys, or certificates containing private key material. Use Android Keystore, OS credential stores, or environment/secret management appropriate to the target platform.
