# PC Receiver Prototype — V1.2

Receives the Android Desktop Bridge H.264 packet stream over a USB-connected transport endpoint and decodes it for development/testing.

## V1.2 scope

- Protocol framing is defined in `protocol/ADBVideoPacket.md`.
- Receiver core is intentionally transport-agnostic.
- The first development transport is a localhost TCP adapter so the decoder can be tested before USB hardware exists.
- A future USB adapter will feed the exact same packet parser.

This prototype does **not** claim HDMI/DisplayPort output yet.
