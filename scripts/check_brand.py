#!/usr/bin/env python3
"""Reject restricted product names without reproducing them in this project.

Digests are policy fingerprints, never an integration or runtime identifier.
Scans source paths and text plus every entry of APK/AAB/ZIP artifacts supplied.
"""
import hashlib
import base64
import json
import re
import sys
import zipfile
from pathlib import Path

RESTRICTED = {"ec8202b6f9fb16f9e26b66367afa4e037752f3c09a18cefab426165e06a424b1"}
SKIP = {'.git', '.gradle', '.signing', 'build', 'output', 'tmp', '__pycache__'}
failures = []

def check(label, data):
    text = data.decode('utf-8', errors='ignore').casefold()
    # Substrings catch compound identifiers; bytes are also checked in UTF-16 resources.
    for length in (8,):
        for token in re.findall(r'[a-z]+', text):
            for i in range(max(0, len(token)-length+1)):
                if hashlib.sha256(token[i:i+length].encode()).hexdigest() in RESTRICTED:
                    failures.append(label)
                    return

def check_payload(label, data):
    check(label, data)
    if label.endswith('supplied-sender-rules.json'):
        try:
            if len(data)>128_000:raise ValueError('size')
            supplied=json.loads(data)
            if supplied['encoding']!='base64-utf8-json':raise ValueError('encoding')
            decoded=base64.b64decode(supplied['payload'],validate=True)
            rows=json.loads(decoded.decode('utf-8'))
            if not isinstance(rows,list) or not all(isinstance(name,str) for name in rows):raise ValueError('names')
            check(label+':decoded', json.dumps(rows,ensure_ascii=False).encode('utf-8'))
        except (ValueError,KeyError,TypeError):
            failures.append(label+':invalid-payload')

def scan(path):
    check(str(path), str(path).encode())
    if path.suffix.lower() in {'.apk', '.aab', '.zip', '.jar'}:
        with zipfile.ZipFile(path) as archive:
            for name in archive.namelist():
                check(str(path) + ':' + name, name.encode())
                data = archive.read(name)
                check_payload(str(path) + ':' + name, data)
                check(str(path) + ':' + name, data.replace(b'\x00', b''))
    else:
        check_payload(str(path), path.read_bytes())

def main():
    paths = [Path(arg) for arg in sys.argv[1:]]
    if not paths:
        paths = [p for p in Path('.').rglob('*') if p.is_file() and not any(part in SKIP for part in p.parts)]
    for path in paths:
        scan(path)
    if failures:
        print('Brand rule failed in: ' + ', '.join(sorted(set(failures))))
        return 1
    print(f'PASS brand rule: {len(paths)} source files/artifacts; no restricted name')
    return 0

if __name__ == '__main__':
    sys.exit(main())
