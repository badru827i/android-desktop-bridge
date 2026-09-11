# Protocol V1.2

The Android encoder produces H.264 access units. `ADBVideoPacket` wraps each unit with metadata so the transport can be USB bulk, TCP for development, or another byte stream.

Next implementation step: replace the development TCP adapter with Android USB host/device transport and a receiver-side USB endpoint. The packet parser remains unchanged.
