# ADB Video Packet Protocol V1

## Header

All integers are little-endian. The header remains fixed at 32 bytes.

| Field | Size | Description |
|---|---:|---|
| Magic | 4 | ASCII `ADBV` |
| Version | 1 | Protocol version, currently `1` |
| Flags | 1 | `0x01` keyframe, `0x02` config/codec data, `0x04` end-of-stream |
| Header size | 2 | Bytes in header, currently 32 |
| Sequence | 4 | Monotonic packet sequence number |
| Timestamp | 8 | Presentation timestamp in microseconds |
| Payload size | 4 | Encoded payload bytes |
| Width | 2 | Video width |
| Height | 2 | Video height |
| FPS | 2 | Nominal frame rate |
| Codec ID | 2 | `1` = H.264/AVC, `2` = H.265/HEVC, `3` = H.266/VVC |

## Codec negotiation

The Android sender discovers actual runtime encoder capabilities and chooses the highest supported codec in this order: H.266/VVC, H.265/HEVC, then H.264/AVC. The receiver must advertise which codec IDs it can decode before the stream starts. The sender must choose the highest codec supported by **both** sides. If no modern codec is available, H.264 is the compatibility fallback.

The bridge must verify encoder capability for the requested resolution and frame rate before selecting a codec. H.266 is opportunistic only; the app must never assume that `video/vvc` exists on a particular phone.

## Payload

For V1, payload is an H.264, H.265, or H.266 access unit, or codec configuration data emitted by Android `MediaCodec`.

A receiver must validate magic, version, header size, codec ID, and payload length before accepting a packet.

## Transport

V1 deliberately separates framing from transport. USB bulk transport, TCP development transport, or another byte-stream transport can carry the same packet format.

## Security

The receiver must not execute payload data. USB devices should be explicitly selected by the user, and production deployments should authenticate/authorize the peer before accepting a stream.
