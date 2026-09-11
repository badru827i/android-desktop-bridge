#!/usr/bin/env python3
"""V1.2 development receiver: parse ADBV packets from a byte stream.

This intentionally stops at decoded access units. A later adapter can feed
USB bytes into the same Receiver without changing the packet format.
"""
import socket
import struct
import sys

MAGIC = b"ADBV"
HEADER = struct.Struct("<4sBBHIQ IHHHH")
HEADER_SIZE = HEADER.size  # 32


def recv_exact(sock, n):
    data = bytearray()
    while len(data) < n:
        chunk = sock.recv(n - len(data))
        if not chunk:
            return None
        data.extend(chunk)
    return bytes(data)


def serve(host="127.0.0.1", port=8765):
    with socket.create_server((host, port), reuse_port=False) as server:
        print(f"ADBV receiver listening on {host}:{port}")
        conn, addr = server.accept()
        with conn:
            print("connected:", addr)
            while True:
                header = recv_exact(conn, HEADER_SIZE)
                if header is None:
                    break
                magic, version, flags, hsize, seq, timestamp, payload_size, width, height, fps, reserved = HEADER.unpack(header)
                if magic != MAGIC or version != 1 or hsize != HEADER_SIZE or reserved != 0:
                    raise ValueError("invalid ADBV header")
                if payload_size > 16 * 1024 * 1024:
                    raise ValueError("payload too large")
                payload = recv_exact(conn, payload_size)
                if payload is None:
                    raise ConnectionError("truncated payload")
                key = bool(flags & 0x01)
                print(f"seq={seq} pts={timestamp} {width}x{height}@{fps} size={payload_size} key={key}")
                if flags & 0x04:
                    break


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8765
    serve(port=port)
