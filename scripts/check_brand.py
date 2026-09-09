#!/usr/bin/env python3
"""Reject restricted product names without reproducing them in this project.

Digests are policy fingerprints, never an integration or runtime identifier.
Scans source paths and text plus every entry of APK/AAB/ZIP artifacts supplied.
"""
import hashlib
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

def scan(path):
    check(str(path), str(path).encode())
    if path.suffix.lower() in {'.apk', '.aab', '.zip', '.jar'}:
        with zipfile.ZipFile(path) as archive:
            for name in archive.namelist():
                check(str(path) + ':' + name, name.encode())
                data = archive.read(name)
                check(str(path) + ':' + name, data)
                check(str(path) + ':' + name, data.replace(b'\x00', b''))
    else:
        check(str(path), path.read_bytes())

paths = [Path(arg) for arg in sys.argv[1:]]
if not paths:
    paths = [p for p in Path('.').rglob('*') if p.is_file() and not any(part in SKIP for part in p.parts)]
for path in paths:
    scan(path)
if failures:
    print('Brand rule failed in: ' + ', '.join(sorted(set(failures))))
    sys.exit(1)
print(f'PASS brand rule: {len(paths)} source files/artifacts; no restricted name')
