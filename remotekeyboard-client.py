#!/usr/bin/env python3
"""
Remote Keyboard TLS client - PC-side replacement for telnet.
Connects to the Remote Keyboard Android app over TLS (encrypted).

Usage:
    python3 remotekeyboard-client.py <PHONE_IP> [PORT]

Example:
    python3 remotekeyboard-client.py 192.168.1.42
    python3 remotekeyboard-client.py 192.168.1.42 2323

The server uses a self-signed certificate; certificate verification is
intentionally skipped (trust-on-first-use model over your local WiFi).

Requires Python 3.3+. No third-party packages needed.
Press Ctrl+] to disconnect.
"""

import ssl
import socket
import sys
import threading
import os

try:
    import termios
    import tty
    UNIX = True
except ImportError:
    UNIX = False  # Windows fallback (line-buffered mode)


def recv_loop(sock):
    """Print everything the server sends to stdout."""
    try:
        while True:
            data = sock.recv(4096)
            if not data:
                break
            sys.stdout.buffer.write(data)
            sys.stdout.buffer.flush()
    except OSError:
        pass
    finally:
        os.write(sys.stdout.fileno(), b"\r\n[Disconnected]\r\n")


def run_unix(sock):
    """Raw-mode keyboard loop for Linux/macOS."""
    fd = sys.stdin.fileno()
    old_settings = termios.tcgetattr(fd)
    try:
        tty.setraw(fd)
        while True:
            ch = sys.stdin.buffer.read(1)
            if not ch or ch == b'\x1d':  # Ctrl+] to quit
                break
            sock.sendall(ch)
    except OSError:
        pass
    finally:
        termios.tcsetattr(fd, termios.TCSADRAIN, old_settings)


def run_windows(sock):
    """Line-buffered fallback for Windows."""
    print("(Windows mode: type a line and press Enter to send. Type 'quit' to exit.)")
    while True:
        try:
            line = input()
        except EOFError:
            break
        if line.lower() == "quit":
            break
        sock.sendall((line + "\n").encode("utf-8"))


def main():
    if len(sys.argv) < 2:
        print(f"Usage: python3 {sys.argv[0]} <PHONE_IP> [PORT]")
        sys.exit(1)

    host = sys.argv[1]
    port = int(sys.argv[2]) if len(sys.argv) > 2 else 2323

    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE   # self-signed cert on LAN is acceptable

    print(f"Connecting to {host}:{port} over TLS …")
    try:
        raw_sock = socket.create_connection((host, port), timeout=10)
        tls_sock = ctx.wrap_socket(raw_sock, server_hostname=host)
    except Exception as e:
        print(f"Connection failed: {e}")
        sys.exit(1)

    cipher = tls_sock.cipher()
    print(f"Connected  |  {cipher[1]} {cipher[0]}")
    if UNIX:
        print("Press Ctrl+] to disconnect.\r")
    print()

    t = threading.Thread(target=recv_loop, args=(tls_sock,), daemon=True)
    t.start()

    try:
        if UNIX:
            run_unix(tls_sock)
        else:
            run_windows(tls_sock)
    finally:
        try:
            tls_sock.close()
        except OSError:
            pass


if __name__ == "__main__":
    main()
