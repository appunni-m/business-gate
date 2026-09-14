#!/usr/bin/env python3
import hashlib
import importlib.util
import json
import re
import shlex
import subprocess
import argparse
import os
import time
import uuid
from pathlib import Path

parser = argparse.ArgumentParser(description='Measure only structured notification identity metadata; never export notification bodies or raw identities.')
parser.add_argument('--serial', required=True)
parser.add_argument('--target-apk', required=True, type=Path)
parser.add_argument('--expected-phone', required=True)
parser.add_argument('--expected-receiver', required=True)
parser.add_argument('--enable-emulator-listener', action='store_true')
parser.add_argument('--output', required=True, type=Path)
args = parser.parse_args()
if not re.fullmatch(r'[A-Za-z0-9._:-]{1,128}', args.serial):
    parser.error('Invalid serial')
if any(not re.fullmatch(r'\+[1-9][0-9]{6,14}', p) for p in (args.expected_phone, args.expected_receiver)):
    parser.error('Expectations require canonical international numbers')
if args.enable_emulator_listener and not args.serial.startswith('emulator-'):
    parser.error('Physical notification access must be granted in Android settings')
ROOT = Path(__file__).resolve().parent.parent
SDK = Path(os.environ['ANDROID_HOME'])
ADB = [str(SDK / 'platform-tools/adb'), '-s', args.serial]
PROBE = 'io.github.appunnim.businessgate.measure'
COMPONENT = PROBE + '/.NotificationMeasurementService'
APK = ROOT / 'qualification-probe/build/outputs/apk/debug/qualification-probe-debug.apk'
DEST = args.output
if DEST.exists():
    raise RuntimeError('Report already exists')

def run(parts, binary=False):
    p = subprocess.run(parts, capture_output=True, text=not binary, timeout=30)
    if p.returncode:
        raise RuntimeError('Scoped command failed: ' + str(p.returncode))
    return p.stdout

def shell(*parts):
    return run(ADB + ['shell', shlex.join(parts)]).strip()

spec = importlib.util.spec_from_file_location('measurement', ROOT / 'scripts/measure-installation.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
target_apk = str(args.target_apk)
badging = run([str(SDK / 'build-tools/35.0.0/aapt2'), 'dump', 'badging', target_apk])
target = re.search("^package: name='([^']+)'", badging, re.M)[1]
version = re.search("^package: .*versionCode='([0-9]+)'", badging, re.M)[1]
api = shell('getprop', 'ro.build.version.sdk')
certificates = run([str(SDK / 'build-tools/35.0.0/apksigner'), 'verify', '--print-certs', target_apk])
signer = module.selected_signer(certificates, int(api))[0]
receiver = args.expected_receiver
run(ADB + ['install', '-r', str(APK)])
original = shell('settings', 'get', 'secure', 'enabled_notification_listeners')
full = PROBE + '/' + PROBE + '.NotificationMeasurementService'
already = COMPONENT in original.split(':') or full in original.split(':')
request = uuid.uuid4().hex
try:
    if args.enable_emulator_listener and not already:
        if shell('getprop', 'ro.kernel.qemu') != '1':
            raise RuntimeError('Device is not an emulator')
        shell('cmd', 'notification', 'allow_listener', COMPONENT)
    shell('am', 'broadcast', '-n', PROBE + '/.MeasurementReceiver', '--es', 'mode', 'notifications', '--es', 'target', target,
          '--es', 'expectedVersion', version, '--es', 'expectedSigner', signer, '--ei', 'expectedApi', api,
          '--es', 'expectedPhone', args.expected_phone, '--es', 'expectedReceiver', receiver, '--es', 'request', request)
    raw = None
    for i in range(60):
        p = subprocess.run(ADB + ['shell', 'run-as', PROBE, 'cat', 'files/' + request + '.json'], capture_output=True, text=True, timeout=5)
        if p.returncode == 0:
            raw = p.stdout
            break
        time.sleep(0.2)
    if raw is None:
        raise RuntimeError('Metadata probe timed out')
    result = module.parse_report(raw, 'notifications')
    if result.get('result') == 'observed':
        assert result['environment']['packageSha256'] == hashlib.sha256(target.encode()).hexdigest()
        assert result['probeApkSha256'] == hashlib.sha256(APK.read_bytes()).hexdigest()
        assert result['environment']['versionCode'] == int(version)
        assert result['environment']['signingSha256'] == [signer]
    DEST.parent.mkdir(parents=True, exist_ok=True)
    with DEST.open('x') as out:
        json.dump(result, out, indent=2)
        out.write('\n')
    print('Observed structured notification metadata; identities and message bodies were not exported.')
finally:
    shell('run-as', PROBE, 'rm', '-f', 'files/' + request + '.json', 'files/' + request + '.tmp')
    if args.enable_emulator_listener and not already:
        shell('cmd', 'notification', 'disallow_listener', COMPONENT)
    after = shell('settings', 'get', 'secure', 'enabled_notification_listeners')
    if after != original:
        if original == 'null':
            shell('settings', 'delete', 'secure', 'enabled_notification_listeners')
        else:
            shell('settings', 'put', 'secure', 'enabled_notification_listeners', original)
    assert shell('settings', 'get', 'secure', 'enabled_notification_listeners') == original, 'Listener settings changed'
