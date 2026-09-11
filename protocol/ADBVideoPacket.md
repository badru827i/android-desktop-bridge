# ADB Video Packet Protocol V1

## Header

All integers are little-endian.

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
| Reserved | 2 | Must be zero |

## Payload

For V1, payload is an H.264 access unit or codec configuration data emitted by Android `MediaCodec`.

A receiver must validate magic, version, header size and payload length before accepting a packet.

## Transport

V1 deliberately separates framing from transport. USB bulk transport, TCP development transport, or another byte-stream transport can carry the same packet format.

## Security

The receiver must not execute payload data. USB devices should be explicitly selected by the user, and production deployments should authenticate/authorize the peer before accepting a stream.
