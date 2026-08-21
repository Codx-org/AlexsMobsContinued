#!/usr/bin/env python3
"""Minimal RCON client. Usage: rcon.py [--port N] [--password P] 'cmd' ['cmd' ...]

The console channel for a dev `runServer`: MDG's ignores piped stdin, so RCON is the
only way to talk to a NeoForge/Forge one. Enable it per node in
`versions/<node>/run/server.properties` (`enable-rcon=true`, `rcon.password=amcdev`,
and a distinct `rcon.port` if you want two nodes' rigs side by side).

Each argument is one command; output is echoed as `>> cmd` then the reply. Note that
`/data get entity <sel> <path>` echoes only the *value* — never the key.
"""
import socket, struct, sys

def pkt(req_id, ptype, body):
    payload = struct.pack('<ii', req_id, ptype) + body.encode() + b'\x00\x00'
    return struct.pack('<i', len(payload)) + payload

def read_pkt(sock):
    raw = b''
    while len(raw) < 4:
        chunk = sock.recv(4 - len(raw))
        if not chunk:
            raise ConnectionError('closed')
        raw += chunk
    (length,) = struct.unpack('<i', raw)
    data = b''
    while len(data) < length:
        chunk = sock.recv(length - len(data))
        if not chunk:
            raise ConnectionError('closed')
        data += chunk
    req_id, ptype = struct.unpack('<ii', data[:8])
    return req_id, ptype, data[8:-2].decode('utf-8', 'replace')

def main():
    args = sys.argv[1:]
    port = 25575
    password = 'amcdev'
    while args and args[0] in ('--port', '--password'):
        if args[0] == '--port':
            port = int(args[1])
        else:
            password = args[1]
        args = args[2:]
    s = socket.create_connection(('127.0.0.1', port), timeout=10)
    s.sendall(pkt(1, 3, password))
    rid, _, _ = read_pkt(s)
    if rid == -1:
        print('AUTH FAILED', file=sys.stderr); sys.exit(2)
    for cmd in args:
        s.sendall(pkt(2, 2, cmd))
        _, _, body = read_pkt(s)
        print(f'>> {cmd}\n{body}')
    s.close()

if __name__ == '__main__':
    main()
