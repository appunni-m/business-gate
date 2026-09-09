#!/usr/bin/env python3
"""Sign the verified release APK using one encrypted repository secret."""
import base64
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile


def signing_fingerprint(tools, apk):
    report = subprocess.check_output([
        str(tools / 'apksigner'), 'verify', '--verbose', '--print-certs', str(apk),
    ], text=True)
    fingerprints = re.findall(r'^Signer #\d+ certificate SHA-256 digest: ([0-9a-f]+)$', report, re.M)
    if len(fingerprints) != 1:
        raise RuntimeError('Expected exactly one APK signing certificate')
    return fingerprints[0]


def main():
    source, destination = map(Path, sys.argv[1:])
    raw = os.environ.get('ANDROID_SIGNING', '')
    if not raw:
        raise SystemExit('Missing ANDROID_SIGNING repository Actions secret. See docs/delivery.md.')
    try:
        secret = json.loads(raw)
        required = {'keystore', 'keyAlias', 'storePassword', 'keyPassword'}
        if set(secret) != required or not all(isinstance(v, str) and v for v in secret.values()):
            raise ValueError()
        key_bytes = base64.b64decode(secret['keystore'], validate=True)
    except (ValueError, TypeError):
        raise SystemExit('ANDROID_SIGNING must contain the signing JSON described in docs/delivery.md.') from None
    # GitHub masks the JSON secret itself; also mask individual credentials.
    if os.environ.get('GITHUB_ACTIONS') == 'true':
        for value in secret.values():
            safe = value.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
            print('::add-mask::' + safe, flush=True)
    tools = Path(os.environ['ANDROID_HOME']) / 'build-tools/35.0.0'
    subprocess.run([str(tools / 'zipalign'), '-c', '-P', '16', '4', str(source)], check=True)
    destination.parent.mkdir(parents=True, exist_ok=True)
    environment = {key: value for key, value in os.environ.items() if key != 'ANDROID_SIGNING'}
    environment.update(GATE_STORE_PASSWORD=secret['storePassword'], GATE_KEY_PASSWORD=secret['keyPassword'])
    with tempfile.TemporaryDirectory(prefix='business-gate-signing-') as directory:
        keystore = Path(directory) / 'publisher.p12'
        with open(keystore, 'xb', opener=lambda path, flags: os.open(path, flags, 0o600)) as stream:
            stream.write(key_bytes)
        subprocess.run([
            str(tools / 'apksigner'), 'sign', '--ks', str(keystore),
            '--ks-key-alias', secret['keyAlias'], '--ks-pass', 'env:GATE_STORE_PASSWORD',
            '--key-pass', 'env:GATE_KEY_PASSWORD', '--v4-signing-enabled', 'false',
            '--out', str(destination), str(source),
        ], env=environment, check=True)
    expected = Path('config/signing-certificate.sha256').read_text().strip()
    if signing_fingerprint(tools, destination) != expected:
        destination.unlink()
        raise SystemExit('APK signing identity differs from the pinned publisher certificate.')
    print('PASS signed APK integrity and pinned publisher identity')


if __name__ == '__main__':
    main()
