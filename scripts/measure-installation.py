#!/usr/bin/env python3
"""Collect bounded developer measurements; never promote or modify the app registry.

The installation is selected from an APK supplied at runtime. No external identity
is embedded or exported. Node mode requires the caller to have established the
selected path on a consenting profile/account/confirmation screen; do not use it
to explore conversations or message containers.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys
import time
import uuid

PACKAGE = 'io.github.appunnim.businessgate.measure'
SERVICE = PACKAGE + '/.MeasurementService'


def selected_signer(certificates, api):
    selected = []
    for line in certificates.splitlines():
        if ' certificate SHA-256 digest: ' not in line:
            continue
        ordinary = re.fullmatch(r'Signer #[0-9]+ certificate SHA-256 digest: ([a-f0-9]{64})', line)
        ranged = re.fullmatch(r'Signer \(minSdkVersion=([0-9]+), maxSdkVersion=([0-9]+)\) certificate SHA-256 digest: ([a-f0-9]{64})', line)
        if ordinary:
            selected.append(ordinary[1])
        elif ranged:
            lower, upper = int(ranged[1]), int(ranged[2])
            if lower < 1 or lower > upper:
                raise ValueError('Invalid signer platform range')
            if lower <= api <= upper:
                selected.append(ranged[3])
        else:
            raise ValueError('Unsupported APK signer report')
    if len(selected) != 1:
        raise ValueError('Selected APK signing identity is ambiguous for this Android version')
    return selected


def parse_report(raw):
    if len(raw.encode()) > 24_000:
        raise ValueError('Measurement exceeded the report limit')
    report = json.loads(raw)
    if not isinstance(report, dict):
        raise ValueError('Invalid measurement report')
    if report.get('result') == 'failed':
        reason = report.get('reason', '')
        raise ValueError('Measurement rejected: ' + (reason if isinstance(reason, str) and re.fullmatch(r'[A-Z_]{1,64}', reason) else 'MEASUREMENT_FAILED'))
    if report.get('schemaVersion') != 1 or report.get('result') != 'observed' or report.get('physicalQualification') is not False or report.get('mutationPerformed') is not False:
        raise ValueError('Invalid measurement scope')
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=('environment', 'node', 'self-test'))
    parser.add_argument('--serial', required=True)
    parser.add_argument('--target-apk', type=Path)
    parser.add_argument('--path', default='')
    parser.add_argument('--surface', choices=('receiver', 'profile', 'block-confirmation', 'unblock-confirmation', 'synthetic'))
    parser.add_argument('--enable-emulator-service', action='store_true', help='Temporarily enable only this tool on an emulator; restore exact settings afterward')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        parser.error('The output already exists; choose a new measurement filename')
    if not re.fullmatch(r'[A-Za-z0-9._:-]{1,128}', args.serial):
        parser.error('Invalid serial')
    if args.mode == 'node' and args.surface is None:
        parser.error('Node capture needs an explicitly established non-message surface')
    if args.path and (not re.fullmatch(r'(0|[1-9][0-9]?)(,(0|[1-9][0-9]?)){0,11}', args.path) or any(int(i) > 63 for i in args.path.split(','))):
        parser.error('Path must contain at most 12 indices from 0 through 63')
    if args.enable_emulator_service and not args.serial.startswith('emulator-'):
        parser.error('Physical-device service consent must be granted in Android settings')
    sdk = Path(os.environ['ANDROID_HOME'])
    adb = [str(sdk / 'platform-tools/adb'), '-s', args.serial]

    def run(command, timeout=30):
        result = subprocess.run(command, text=True, capture_output=True, timeout=timeout)
        if result.returncode:
            # Platform errors can contain the selected installation identity; no raw error dump.
            raise RuntimeError('Device/tool command failed with exit ' + str(result.returncode))
        return result.stdout.strip()

    def shell(*parts):
        return run(adb + ['shell', shlex.join(parts)])

    expected = None
    if args.mode == 'self-test':
        if not args.serial.startswith('emulator-'):
            parser.error('The synthetic self-test is for a dedicated emulator')
        target = PACKAGE
    else:
        if not args.target_apk or not args.target_apk.is_file():
            parser.error('Supply the selected installation APK at runtime')
        certificates = run([str(sdk / 'build-tools/35.0.0/apksigner'), 'verify', '--print-certs', str(args.target_apk)], 60)
        badging = run([str(sdk / 'build-tools/35.0.0/aapt2'), 'dump', 'badging', str(args.target_apk)])
        match = re.search(r"^package: name='([A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)+)'", badging, re.M)
        if not match:
            raise ValueError('No valid installation identity in supplied APK')
        target = match[1]
        version = re.search(r"^package: .*versionCode='([0-9]+)'", badging, re.M)
        api = shell('getprop', 'ro.build.version.sdk')
        if not api.isdigit() or int(api) < 29 or version is None:
            raise ValueError('Selected APK or device version is unsupported')
        signers = selected_signer(certificates, int(api))
        expected = (int(version[1]), signers, int(api))
    original = {}
    request = uuid.uuid4().hex
    result_path = 'files/' + request + '.json'
    if args.enable_emulator_service and args.mode != 'environment':
        if shell('getprop', 'ro.kernel.qemu') != '1':
            raise ValueError('Device is not an emulator')
        for key in ('enabled_accessibility_services', 'accessibility_enabled'):
            original[key] = shell('settings', 'get', 'secure', key)
    try:
        command = adb + ['shell', 'am', 'start', '-W', '--es', 'mode', args.mode, '--es', 'target', target, '--es', 'request', request]
        if args.mode == 'node':
            command += ['--es', 'surface', args.surface]
            if args.path:
                command += ['--es', 'path', args.path]
        command += ['-n', PACKAGE + '/.MeasurementActivity']
        if original:
            services = [] if original['enabled_accessibility_services'] in ('null', '') else original['enabled_accessibility_services'].split(':')
            if SERVICE not in services and PACKAGE + '/' + PACKAGE + '.MeasurementService' not in services:
                services.append(SERVICE)
            shell('settings', 'put', 'secure', 'enabled_accessibility_services', ':'.join(services))
            shell('settings', 'put', 'secure', 'accessibility_enabled', '1')
        launch = run(command, 30)
        if 'Status: ok' not in launch:
            raise RuntimeError('Measurement activity did not start')
        deadline = time.monotonic() + 25
        while time.monotonic() < deadline:
            response = subprocess.run(adb + ['shell', 'run-as', PACKAGE, 'cat', result_path], capture_output=True, text=True, timeout=5)
            if response.returncode == 0:
                raw = response.stdout
                break
            time.sleep(0.2)
        else:
            raise RuntimeError('Measurement result was not committed within the time limit')
        report = parse_report(raw)
        probe_apk = Path(__file__).resolve().parent.parent / 'qualification-probe/build/outputs/apk/debug/qualification-probe-debug.apk'
        if report.get('probeApkSha256') != hashlib.sha256(probe_apk.read_bytes()).hexdigest():
            raise ValueError('Installed measurement tool differs from the locally built APK')
        if report['environment']['packageSha256'] != hashlib.sha256(target.encode()).hexdigest():
            raise ValueError('Measurement returned a different installation')
        if expected and (report['environment']['versionCode'], report['environment']['signingSha256'], report['environment']['api']) != expected:
            raise ValueError('Installed version or signer differs from the selected APK')
        if args.target_apk:
            report['selectedApkSha256'] = hashlib.sha256(args.target_apk.read_bytes()).hexdigest()
    finally:
        cleanup_failed = False
        try:
            shell('run-as', PACKAGE, 'rm', '-f', result_path, 'files/' + request + '.tmp')
        except (RuntimeError, subprocess.TimeoutExpired):
            cleanup_failed = True
        for key, value in original.items():
            try:
                if value == 'null':
                    shell('settings', 'delete', 'secure', key)
                else:
                    shell('settings', 'put', 'secure', key, value)
            except (RuntimeError, subprocess.TimeoutExpired):
                cleanup_failed = True
        for key, value in original.items():
            try:
                if shell('settings', 'get', 'secure', key) != value:
                    cleanup_failed = True
            except (RuntimeError, subprocess.TimeoutExpired):
                cleanup_failed = True
        if cleanup_failed:
            raise RuntimeError('Measurement cleanup or accessibility restoration could not be verified')
    report['accessibilitySettingsRestored'] = bool(original)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    # Refuse to overwrite earlier measurements, including failed/stale versions.
    with args.output.open('x') as stream:
        json.dump(report, stream, indent=2)
        stream.write('\n')
    print('PASS read-only ' + args.mode + ' measurement; physical qualification remains false')


if __name__ == '__main__':
    try:
        main()
    except subprocess.TimeoutExpired:
        print('FAIL measurement command timed out', file=sys.stderr)
        sys.exit(1)
    except (ValueError, RuntimeError) as error:
        print('FAIL ' + str(error), file=sys.stderr)
        sys.exit(1)
