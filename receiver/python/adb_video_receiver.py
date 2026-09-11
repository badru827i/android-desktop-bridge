#!/usr/bin/env python3
"""Scrcpy-style development receiver for ADBV/H.264 video."""
import socket
import struct
import subprocess
import sys

MAGIC = b"ADBV"
HEADER = struct.Struct("<4sBBHIQIHHHH")
HEADER_SIZE = HEADER.size  # 32
MAX_PAYLOAD = 16 * 1024 * 1024
CODECS = {1: "H.264/AVC", 2: "H.265/HEVC", 3: "H.266/VVC"}


def recv_exact(sock, n):
    data = bytearray()
    while len(data) < n:
        chunk = sock.recv(n - len(data))
        if not chunk:
            return None
        data.extend(chunk)
    return bytes(data)


def serve(host="127.0.0.1", port=27183, display=True):
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((host, port))
    server.listen(1)
    print(f"ADBV receiver listening on {host}:{port}")
    conn, addr = server.accept()
    ffplay = None
    with conn:
        print("connected:", addr)
        conn.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        while True:
            header = recv_exact(conn, HEADER_SIZE)
            if header is None:
                break
            magic, version, flags, hsize, seq, timestamp, payload_size, width, height, fps, codec_id = HEADER.unpack(header)
            if magic != MAGIC or version != 1 or hsize != HEADER_SIZE or codec_id not in CODECS:
                raise ValueError("invalid ADBV header")
            if payload_size > MAX_PAYLOAD:
                raise ValueError("payload too large")
            payload = recv_exact(conn, payload_size)
            if payload is None:
                raise ConnectionError("truncated payload")

            if seq < 3 or flags & 0x03:
                print(f"seq={seq} {width}x{height}@{fps} codec={CODECS[codec_id]} size={payload_size}")

            if display:
                if codec_id != 1:
                    raise RuntimeError("This test receiver currently displays H.264 only")
                if ffplay is None:
                    try:
                        ffplay = subprocess.Popen(
                            ["ffplay", "-loglevel", "warning", "-fflags", "nobuffer", "-flags", "low_delay", "-f", "h264", "-i", "pipe:0"],
                            stdin=subprocess.PIPE,
                        )
                    except FileNotFoundError:
                        print("ffplay not found; packet parsing will continue")
                        display = False
                if ffplay and ffplay.stdin:
                    try:
                        ffplay.stdin.write(payload)
                        ffplay.stdin.flush()
                    except (BrokenPipeError, OSError):
                        display = False

            if flags & 0x04:
                break

    if ffplay and ffplay.stdin:
        try:
            ffplay.stdin.close()
            ffplay.wait(timeout=2)
        except Exception:
            ffplay.kill()
    server.close()


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 27183
    serve(port=port, display=True)
