# Protocol V1.2

ADBV is transport-agnostic. Codec IDs: H264=1, H265=2, H266=3. Sender chooses the highest codec supported by both endpoints. H266 is optional.

Transports: localhost TCP for development, USB networking/tethering, or dedicated USB bridge hardware. See USB_ARCHITECTURE.md.

Input/control is separate from video. See INPUT.md. Security rules are in SECURITY.md.
