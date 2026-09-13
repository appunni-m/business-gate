#!/usr/bin/env python3
"""Collect bounded developer measurements; never promote or modify the app registry.

The installation is selected from an APK supplied at runtime. No external identity
is embedded or exported. Node mode requires the caller to have established the
selected path on a consenting profile/account/confirmation screen; do not use it
to explore conversations or message containers. The separate chat-node mode needs
an explicit owner exception for one exact-number test chat and a fresh profile check.
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
import unicodedata

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


def decode_report(raw):
    if len(raw.encode()) > 24_000:
        raise ValueError('Measurement exceeded the report limit')
    report = json.loads(raw)
    if not isinstance(report, dict):
        raise ValueError('Invalid measurement report')
    return report


def safe_navigation_label(value):
    if not isinstance(value, str) or not 1 <= len(value) <= 40:
        return False
    separators = " _'’-"
    if any(unicodedata.category(c)[0] not in ('L', 'M') and c not in separators for c in value):
        return False
    normalized = unicodedata.normalize('NFKC', value).lower()
    compact = ''.join(c for c in normalized if c not in separators)
    return all(hashlib.sha256(candidate[i:i + 8].encode()).hexdigest() != 'ec8202b6f9fb16f9e26b66367afa4e037752f3c09a18cefab426165e06a424b1'
               for candidate in (normalized, compact) for i in range(len(candidate) - 7))


def parse_report(raw, expected_mode=None):
    report = decode_report(raw)
    if report.get('result') == 'failed':
        reason = report.get('reason', '')
        raise ValueError('Measurement rejected: ' + (reason if isinstance(reason, str) and re.fullmatch(r'[A-Z_]{1,64}', reason) else 'MEASUREMENT_FAILED'))
    if report.get('schemaVersion') != 1 or report.get('result') != 'observed' or report.get('physicalQualification') is not False or type(report.get('mutationPerformed')) is not bool or (expected_mode not in ('block-trial','unblock-trial') and report.get('mutationPerformed') is not (expected_mode in ('send-draft', 'open-business-block', 'open-business-unblock'))):
        raise ValueError('Invalid measurement scope')
    if expected_mode is not None and report.get('mode') != expected_mode:
        raise ValueError('Measurement result does not match the requested mode')
    capture = report.get('measurement')
    if expected_mode == 'send-draft' and (not isinstance(capture, dict) or capture.get('messageDispatchAcknowledged') is not True or capture.get('draftMatched') is not True or capture.get('recipientProfileChecked') is not True or capture.get('activeFocusedWindow') is not True):
        raise ValueError('Message dispatch was not acknowledged')
    if expected_mode not in ('send-draft', 'open-business-block', 'open-business-unblock', 'block-trial', 'unblock-trial') and isinstance(capture, dict) and any(key in capture for key in ('messageDispatchAcknowledged', 'draftMatched', 'recipientProfileChecked', 'blockEntryAcknowledged', 'unblockEntryAcknowledged')):
        raise ValueError('Unexpected message dispatch metadata')
    if expected_mode == 'open-business-block' and (not isinstance(capture, dict) or capture.get('blockEntryAcknowledged') is not True or capture.get('recipientProfileChecked') is not True or capture.get('activeFocusedWindow') is not True):
        raise ValueError('Block entry was not acknowledged')
    if expected_mode == 'open-business-unblock' and (not isinstance(capture, dict) or capture.get('unblockEntryAcknowledged') is not True or capture.get('recipientProfileChecked') is not True or capture.get('activeFocusedWindow') is not True):
        raise ValueError('Unblock entry was not acknowledged')
    if expected_mode in ('block-trial', 'unblock-trial'):
        if (not isinstance(capture, dict) or type(capture.get('finalActionAttempted')) is not bool
                or capture['finalActionAttempted'] is not report['mutationPerformed']
                or capture.get('recipientRecheckedAfter') is not True or capture.get('reportSelected') is not False
                or capture.get('blockedStateVerified') is not (expected_mode == 'block-trial')
                or capture.get('unblockedStateVerified') is not (expected_mode == 'unblock-trial')
                or capture.get('secondConfirmationScreen') is not (expected_mode == 'block-trial' and report['mutationPerformed'])):
            raise ValueError('Action trial lacks a measured result')
    if expected_mode == 'navigation-label':
        summary = capture.get('selectedText') if isinstance(capture, dict) else None
        if (report.get('surfaceAttestation') != 'navigation' or not isinstance(summary, dict)
                or summary.get('inspected') is not True or not safe_navigation_label(summary.get('navigationLabel'))):
            raise ValueError('Invalid navigation title report')
    elif isinstance(capture, dict) and isinstance(capture.get('selectedText'), dict) and 'navigationLabel' in capture['selectedText']:
        raise ValueError('Unexpected navigation title')
    if expected_mode == 'receiver-trial' and (not isinstance(capture, dict) or capture.get('receiverIdentityPresent') is not True or capture.get('receiverIdentitySyntaxVerified') is not True or capture.get('navigationAction') != 'receiver-trial'):
        raise ValueError('Receiving-account navigation did not verify an identity')
    if expected_mode in ('open-profile', 'open-contact', 'scroll-profile-forward', 'scroll-profile-backward'):
        if not isinstance(capture, dict) or capture.get('navigationAction') != expected_mode or capture.get('activeFocusedWindow') is not True:
            raise ValueError('Profile navigation was not dispatched')
    elif expected_mode != 'receiver-trial' and isinstance(capture, dict) and 'navigationAction' in capture:
        raise ValueError('Unexpected navigation action')
    if expected_mode in ('focus-tab', 'focus-overflow', 'focus-profile'):
        if (not isinstance(capture, dict) or capture.get('inputFocusRequested') is not True
                or not isinstance(capture.get('node'), dict) or capture['node'].get('focused') is not True
                or capture.get('activeFocusedWindow') is not True):
            raise ValueError('Navigation input focus was not confirmed')
    elif expected_mode is not None and isinstance(capture, dict) and capture.get('inputFocusRequested', False) is not False:
        raise ValueError('Capture unexpectedly requested input focus')
    if expected_mode in ('root', 'structure', 'self-test', 'focus-tab', 'focus-overflow', 'focus-profile', 'open-profile', 'open-contact', 'send-draft', 'scroll-profile-forward', 'scroll-profile-backward') and isinstance(capture, dict):
        if capture.get('selectedDescription', {'inspected': False}) != {'inspected': False}:
            raise ValueError('Structural capture cannot inspect descriptions')
    if expected_mode in ('structure', 'focus-tab', 'focus-overflow', 'focus-profile', 'open-profile', 'open-contact', 'send-draft', 'scroll-profile-forward', 'scroll-profile-backward'):
        capture = report.get('measurement')
        if not isinstance(capture, dict) or capture.get('selectedText') != {'inspected': False}:
            raise ValueError('Structural capture cannot inspect text')
    if expected_mode in ('root', 'self-test'):
        capture = report.get('measurement')
        if (not isinstance(capture, dict) or capture.get('path') != [] or capture.get('ancestors') != []
                or type(capture.get('acquiredNodes')) is not int or capture['acquiredNodes'] != 1
                or capture.get('selectedText') != {'inspected': False}
                or capture.get('activeFocusedWindow') is not True):
            raise ValueError('Root capture exceeded its scope')
        if expected_mode == 'root' and report.get('screenClassification') != 'unclassified':
            raise ValueError('Root capture cannot establish screen identity')
    if expected_mode == 'chat-node':
        if (not isinstance(capture, dict) or capture.get('recipientProfileCheckedForRead') is not True
                or capture.get('activeFocusedWindow') is not True or report.get('surfaceAttestation') != 'authorized-chat'
                or not isinstance(capture.get('path'), list) or capture['path'][:2] != [4, 1]):
            raise ValueError('Chat recipient was not verified')
    elif isinstance(capture, dict) and ('recipientProfileCheckedForRead' in capture
            or any(key in capture.get(section, {}) for section, key in
                   (('selectedText', 'observedText'), ('selectedDescription', 'observedDescription')))):
        raise ValueError('Unexpected chat content')
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=('environment', 'root', 'structure', 'node', 'navigation-label', 'focus-tab', 'focus-overflow', 'focus-profile', 'open-profile', 'open-contact', 'send-draft', 'chat-node', 'block-trial', 'unblock-trial', 'receiver-trial', 'open-business-block', 'open-business-unblock', 'profile-identity', 'scroll-profile-forward', 'scroll-profile-backward', 'self-test'))
    parser.add_argument('--serial', required=True)
    parser.add_argument('--target-apk', type=Path)
    parser.add_argument('--path', default='')
    parser.add_argument('--expected-phone')
    parser.add_argument('--expected-text-sha256')
    parser.add_argument('--surface', choices=('navigation', 'receiver', 'profile', 'block-confirmation', 'unblock-confirmation', 'composer', 'synthetic'))
    parser.add_argument('--enable-emulator-service', action='store_true', help='Temporarily enable only this tool on an emulator; restore exact settings afterward')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        parser.error('The output already exists; choose a new measurement filename')
    if not re.fullmatch(r'[A-Za-z0-9._:-]{1,128}', args.serial):
        parser.error('Invalid serial')
    if args.mode in ('node', 'structure', 'navigation-label') and args.surface is None:
        parser.error('Node capture needs an explicitly established non-message surface')
    if args.mode == 'navigation-label' and args.surface != 'navigation':
        parser.error('Navigation label reading requires the established options-menu surface')
    if args.mode not in ('node', 'structure', 'navigation-label', 'focus-tab', 'focus-overflow', 'focus-profile', 'open-profile', 'open-contact', 'send-draft', 'chat-node', 'block-trial', 'unblock-trial', 'receiver-trial', 'open-business-block', 'open-business-unblock', 'profile-identity', 'scroll-profile-forward', 'scroll-profile-backward') and args.path:
        parser.error('This mode cannot select a child path')
    if args.mode == 'focus-tab' and args.path not in ('', '5,0', '7,3'):
        parser.error('Supply one of the measured navigation tab paths')
    if args.mode in ('focus-profile', 'open-profile') and args.path != '0,4':
        parser.error('Supply the measured profile navigation path, 0,4')
    if args.mode == 'open-contact' and args.path != '2':
        parser.error('Supply the measured contact header path, 2')
    if args.expected_phone is not None and (args.mode not in ('node', 'send-draft', 'chat-node', 'block-trial', 'unblock-trial', 'open-business-block', 'open-business-unblock', 'profile-identity', 'scroll-profile-forward', 'scroll-profile-backward') or (args.mode == 'node' and args.surface not in ('profile', 'receiver')) or not re.fullmatch(r'\+[1-9][0-9]{6,14}', args.expected_phone)):
        parser.error('Phone matching requires a canonical number and an established identity field')
    if args.expected_text_sha256 is not None and (args.mode not in ('node', 'send-draft') or (args.mode == 'node' and args.surface != 'composer') or not re.fullmatch(r'[a-f0-9]{64}', args.expected_text_sha256)):
        parser.error('Draft matching requires an established composer field and a SHA-256 digest')
    if args.mode == 'receiver-trial' and args.path:
        parser.error('Receiving-account trials use the measured own-profile route without a path')
    if args.mode in ('block-trial', 'unblock-trial') and (args.path or args.expected_phone is None):
        parser.error('Block trial requires the exact expected number and its fixed measured route')
    if args.mode in ('open-business-block', 'open-business-unblock') and (args.path != '0,22' or args.expected_phone is None):
        parser.error('Block entry requires its measured control and exact expected number')
    if args.mode == 'profile-identity' and (args.path != '0,2' or args.expected_phone is None):
        parser.error('Profile identity requires its measured phone field and the exact expected number')
    if args.mode.startswith('scroll-profile-') and (args.path != '0' or args.expected_phone is None):
        parser.error('Profile scrolling requires the measured list and a fresh exact recipient check')
    if args.mode == 'chat-node' and (args.expected_phone is None or not (args.path == '4,1' or args.path.startswith('4,1,'))):
        parser.error('Chat inspection requires explicit owner authorization, a fresh exact recipient check and the measured message-list path')
    if args.mode == 'send-draft' and (args.path != '4,5' or args.expected_phone is None or args.expected_text_sha256 is None):
        parser.error('Sending requires the measured send control, exact recipient and exact draft digest')
    if args.mode == 'focus-overflow' and args.path not in ('3', '4'):
        parser.error('Supply the measured overflow path, 3 or 4')
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
        self_test = args.mode == 'self-test'
        command = adb + ['shell', 'am'] + (['start', '-W'] if self_test else ['broadcast'])
        command += ['--es', 'mode', args.mode, '--es', 'target', target, '--es', 'request', request]
        if args.mode in ('focus-tab', 'focus-overflow', 'focus-profile', 'open-profile', 'open-contact', 'send-draft', 'chat-node', 'block-trial', 'unblock-trial', 'receiver-trial', 'open-business-block', 'open-business-unblock', 'profile-identity', 'scroll-profile-forward', 'scroll-profile-backward'):
            command += ['--es', 'expectedVersion', str(expected[0]), '--es', 'expectedSigner', expected[1][0], '--ei', 'expectedApi', str(expected[2])]
        if args.mode in ('focus-tab', 'focus-overflow', 'focus-profile', 'open-profile', 'open-contact', 'send-draft', 'chat-node', 'block-trial', 'unblock-trial', 'receiver-trial', 'open-business-block', 'open-business-unblock', 'profile-identity', 'scroll-profile-forward', 'scroll-profile-backward') and args.path:
            command += ['--es', 'path', args.path]
        if args.mode in ('node', 'structure', 'navigation-label'):
            command += ['--es', 'surface', args.surface]
            if args.path:
                command += ['--es', 'path', args.path]
        if args.expected_phone is not None:
            command += ['--es', 'expectedPhone', args.expected_phone]
        if args.expected_text_sha256 is not None:
            command += ['--es', 'expectedTextSha256', args.expected_text_sha256]
        command += ['-n', PACKAGE + ('/.MeasurementActivity' if self_test else '/.MeasurementReceiver')]
        if original:
            services = [] if original['enabled_accessibility_services'] in ('null', '') else original['enabled_accessibility_services'].split(':')
            if SERVICE not in services and PACKAGE + '/' + PACKAGE + '.MeasurementService' not in services:
                services.append(SERVICE)
            shell('settings', 'put', 'secure', 'enabled_accessibility_services', ':'.join(services))
            shell('settings', 'put', 'secure', 'accessibility_enabled', '1')
        launch = run(command, 30)
        if ('Status: ok' if self_test else 'Broadcast completed: result=0') not in launch:
            raise RuntimeError('Measurement entry did not complete')
        deadline = time.monotonic() + 25
        while time.monotonic() < deadline:
            response = subprocess.run(adb + ['shell', 'run-as', PACKAGE, 'cat', result_path], capture_output=True, text=True, timeout=5)
            if response.returncode == 0:
                raw = response.stdout
                break
            time.sleep(0.2)
        else:
            raise RuntimeError('Measurement result was not committed within the time limit')
        decoded = decode_report(raw)
        if isinstance(decoded, dict) and decoded.get('result') == 'failed':
            reason = decoded.get('reason', '')
            if not isinstance(reason, str) or not re.fullmatch(r'[A-Z_]{1,64}', reason):
                reason = 'MEASUREMENT_FAILED'
            args.output.parent.mkdir(parents=True, exist_ok=True)
            with args.output.open('x') as stream:
                json.dump({'schemaVersion': 1, 'mode': args.mode, 'result': 'failed', 'reason': reason,
                           'physicalQualification': False, **({key: decoded[key] for key in ('trialPhase', 'confirmationAttempted') if key in decoded} if args.mode in ('block-trial', 'unblock-trial') else {}), **({'dispatchOutcome': 'unconfirmed'} if args.mode in ('send-draft', 'open-business-block', 'open-business-unblock', 'block-trial', 'unblock-trial') else {'mutationPerformed': False})}, stream, indent=2)
                stream.write('\n')
        report = parse_report(raw, args.mode)
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
    print('PASS bounded ' + args.mode + ' measurement; physical qualification remains false')


if __name__ == '__main__':
    try:
        main()
    except subprocess.TimeoutExpired:
        print('FAIL measurement command timed out', file=sys.stderr)
        sys.exit(1)
    except (ValueError, RuntimeError) as error:
        print('FAIL ' + str(error), file=sys.stderr)
        sys.exit(1)
