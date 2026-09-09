#!/usr/bin/env python3
"""Create a private publisher key once. Never print its secret material."""
import base64
import hashlib
import json
import os
from pathlib import Path
import secrets
import subprocess

root = Path(__file__).resolve().parents[1]
private = root / '.signing'
pin = root / 'config/signing-certificate.sha256'
if private.exists() or pin.exists():
    raise SystemExit('Signing setup already exists; refusing to replace the update identity.')
private.mkdir(mode=0o700)
password = secrets.token_urlsafe(48)
environment = dict(os.environ, GATE_KEY_PASSWORD=password)
keytool = str(Path(os.environ['JAVA_HOME']) / 'bin/keytool')
keystore = private / 'business-gate.p12'
subprocess.run([
    keytool, '-genkeypair', '-noprompt', '-storetype', 'PKCS12',
    '-keystore', str(keystore), '-storepass:env', 'GATE_KEY_PASSWORD',
    '-keypass:env', 'GATE_KEY_PASSWORD', '-alias', 'business-gate',
    '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000',
    '-dname', 'CN=Business Gate',
], env=environment, check=True)
keystore.chmod(0o600)
certificate = subprocess.check_output([
    keytool, '-exportcert', '-keystore', str(keystore), '-alias', 'business-gate',
    '-storepass:env', 'GATE_KEY_PASSWORD',
], env=environment)
secret = private / 'github-secret.json'
with open(secret, 'x', opener=lambda path, flags: os.open(path, flags, 0o600)) as stream:
    json.dump({
        'keystore': base64.b64encode(keystore.read_bytes()).decode('ascii'),
        'storePassword': password, 'keyPassword': password, 'keyAlias': 'business-gate',
    }, stream)
pin.parent.mkdir(exist_ok=True)
pin.write_text(hashlib.sha256(certificate).hexdigest() + '\n')
print('Created private signing backup in .signing/ and public certificate fingerprint in config/.')
print('Store the entire github-secret.json as the ANDROID_SIGNING repository Actions secret.')
