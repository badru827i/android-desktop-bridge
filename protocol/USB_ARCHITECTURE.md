# USB transport architecture

The software protocol is transport-agnostic. ADBV packets can move over localhost TCP during development, USB networking/tethering, or a dedicated USB video bridge.

## Production target

`Android phone -> USB-C -> receiver/bridge hardware -> decoder -> HDMI/DP -> monitor`

The receiver may expose a USB endpoint and forward ADBV packets to a hardware or software decoder. The Android app should not claim that a passive USB-C-to-HDMI cable can create video on phones without DisplayPort Alt Mode.

## Development path

1. Android app creates ADBV packets.
2. Pair/authenticate the receiver.
3. Send packets over a local TCP adapter for testing.
4. Move the same packet stream to USB networking/tethering.
5. Replace that adapter with dedicated receiver hardware when available.

The ADBV packet format does not change between these transports.
