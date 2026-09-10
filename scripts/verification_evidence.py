#!/usr/bin/env python3
"""Retain the exact owned test matrix and toolchain, including failed/unrun cases."""
import hashlib
import itertools
import json
import os
from pathlib import Path
import platform
import re
import struct
import subprocess
import sys

NATIVE = ('all', 'ui', 'focus', 'dialogs', 'interaction', 'service-lifecycle', 'privacy-boundary', 'performance', 'prepare-attention',
          'verify-attention', 'prepare-recovery-entry', 'verify-recovery-entry',
          'prepare-recovery-confirm', 'verify-recovery-confirm')
LAYOUT = tuple('layout-' + '-'.join(parts) for parts in itertools.product(
    ('light', 'dark'), ('normal', 'large', 'largest'), ('portrait', 'landscape'), ('standard', 'large')))

ALL_MODES = NATIVE + LAYOUT + ('prepare-task', 'verify-task', 'reminder', 'reminder-channel')
DIALOGS = ('addNumber', 'settings', 'compatibility', 'diagnostics', 'accessDisclosure', 'notificationDisclosure', 'salesDisclosure', 'clearLocal', 'manualBlock', 'unblockNow')

def views(mode):
    return ('layout', 'layout-expanded', 'layout-action') + (tuple('dialog-' + name for name in DIALOGS) if '-largest-' in mode else ())


def valid_navigation(value, api, commit, run_id=None):
    if not isinstance(value, dict) or value.get('complete') is not True or value.get('settingsRestored') is not True:
        return False
    cases = value.get('cases', [])
    expected = {'interaction', 'layout-light-largest-portrait-large', 'layout-light-largest-landscape-large'}
    expected_images = {f'{mode}-{view}.png' for mode in expected if mode.startswith('layout-') for view in views(mode)}
    try:
        return (value.get('api') == api and len(cases) == 2
                and {case['navigation'] for case in cases} == {'threebutton', 'gestural'}
                and all(case['status'] == 'passed' and case['environment']['sourceCommit'] == commit
                        and case['environment']['device']['api'] == api
                        and (run_id is None or (case['environment'].get('runId') == run_id and case['environment'].get('workingTreeModified') is False))
                        and len(case['reports']) == 3 and {report['mode'] for report in case['reports']} == expected
                        and all(type(report['assertions']) is int and report['assertions'] > 0 and re.fullmatch(r'[0-9a-f]{64}', report['sha256']) for report in case['reports'])
                        and len(case['images']) == 26 and {image['file'] for image in case['images']} == expected_images
                        and all(re.fullmatch(r'[0-9a-f]{64}', image['sha256']) for image in case['images']) for case in cases))
    except (KeyError, TypeError):
        return False


def command(args):
    result = subprocess.run(args, capture_output=True, text=True, timeout=30, check=True)
    return (result.stdout + result.stderr).strip()[:8000]


def environment():
    sdk = Path(os.environ['ANDROID_HOME'])
    serial = os.environ['GATE_TEST_SERIAL']
    if not re.fullmatch(r'emulator-[0-9]+', serial):
        raise ValueError('Owned verification requires an isolated emulator')
    adb = [str(sdk / 'platform-tools/adb'), '-s', serial]
    device = {name: command(adb + ['shell', 'getprop', prop]) for name, prop in (
        ('api', 'ro.build.version.sdk'), ('abi', 'ro.product.cpu.abi'), ('fingerprint', 'ro.build.fingerprint'))}
    for name, args in (('size', ['wm', 'size']), ('density', ['wm', 'density']),
                       ('fontScale', ['settings', 'get', 'system', 'font_scale']),
                       ('night', ['cmd', 'uimode', 'night']),
                       ('navigationSetting', ['settings', 'get', 'secure', 'navigation_mode'])):
        device[name] = command(adb + ['shell'] + args)
    image = os.environ.get('GATE_SYSTEM_IMAGE', '')
    image_revision = 'not supplied'
    if image:
        if not re.fullmatch(r'system-images;android-[0-9]+;[a-z_]+;[a-z0-9_-]+', image):
            raise ValueError('Invalid system-image coordinate')
        image_revision = (sdk.joinpath(*image.split(';')) / 'source.properties').read_text()
    return {'schemaVersion': 1, 'sourceCommit': os.environ.get('GITHUB_SHA') or command(['git', 'rev-parse', 'HEAD']),
            'workingTreeModified': bool(command(['git', 'status', '--porcelain'])),
            'runId': os.environ.get('GITHUB_RUN_ID'), 'runAttempt': os.environ.get('GITHUB_RUN_ATTEMPT'),
            'runnerImage': os.environ.get('ImageOS', platform.system()), 'runnerVersion': os.environ.get('ImageVersion', ''),
            'hostArchitecture': platform.machine(), 'java': command([str(Path(os.environ['JAVA_HOME']) / 'bin/java'), '-version']),
            'gradle': command(['./gradlew', '--version']), 'adb': command([str(sdk / 'platform-tools/adb'), 'version']),
            'emulator': command([str(sdk / 'emulator/emulator'), '-version']),
            'buildTools': (sdk / 'build-tools/35.0.0/source.properties').read_text(),
            'systemImage': image, 'systemImageRevision': image_revision, 'device': device,
            'scope': 'Owned synthetic emulator evidence. No physical or connected-app qualification.'}


def matrix(directory, env):
    cases = []
    for mode in ALL_MODES:
        path = directory / 'device-tests' / f'{mode}.txt'
        case = {'mode': mode, 'status': 'unrun', 'assertions': 0, 'images': []}
        if path.exists():
            raw = path.read_bytes()
            text = raw.decode('utf-8', errors='replace')
            match = re.search(r'(?m)^(?:INSTRUMENTATION_RESULT: stream=)?PASS ([0-9]+) [^\n]+; mode=' + re.escape(mode) + r'\s*$', text)
            passed = len(raw) <= 64_000 and match is not None and int(match[1]) > 0 and 'FAIL' not in text
            case.update(status='passed' if passed else 'failed', assertions=int(match[1]) if passed else 0,
                        report=path.name, reportSha256=hashlib.sha256(raw).hexdigest())
            case['metrics'] = [dict(re.findall(r'([a-z][a-z0-9_]{0,39})=([0-9]{1,20})', line)) for line in text.splitlines() if line.startswith('METRIC ')]
            if mode.startswith('layout-'):
                for view in views(mode):
                    image = directory / 'ui-layout' / f'{mode}-{view}.png'
                    if not image.exists():
                        case['status'] = 'failed'
                        case['missingEvidence'] = True
                        continue
                    data = image.read_bytes()
                    if len(data) < 24 or data[:8] != b'\x89PNG\r\n\x1a\n' or data[12:16] != b'IHDR':
                        case['status'] = 'failed'
                        case['invalidImage'] = True
                        continue
                    width, height = struct.unpack('>II', data[16:24])
                    case['images'].append({'file': image.name, 'sha256': hashlib.sha256(data).hexdigest(), 'widthPx': width, 'heightPx': height})
        cases.append(case)
    navigation_path = directory / 'verification/navigation-api36.json'
    navigation = json.loads(navigation_path.read_text()) if navigation_path.exists() else None
    return {'schemaVersion': 1, 'environment': env, 'navigation': {'36': navigation},
            'complete': all(c['status'] == 'passed' for c in cases) and valid_navigation(navigation, '36', env.get('sourceCommit'), env.get('runId')),
            'cases': cases, 'assertions': sum(c['assertions'] for c in cases),
            'imageCount': sum(len(c['images']) for c in cases),
            'physicalQualification': 'unrun', 'talkBackTraversal': 'unrun',
            'scope': 'Only the listed synthetic modes are covered; a passed matrix is not full design completion.'}


def main():
    directory = Path('output/verification')
    directory.mkdir(parents=True, exist_ok=True)
    if sys.argv[1:] == ['reset']:
        for mode in ALL_MODES:
            (Path('output/device-tests') / f'{mode}.txt').unlink(missing_ok=True)
            if mode.startswith('layout-'):
                for view in views(mode):
                    (Path('output/ui-layout') / f'{mode}-{view}.png').unlink(missing_ok=True)
        for name in ('environment.json', 'verification.json', 'navigation-api36.json', 'navigation-api29.json'):
            (directory / name).unlink(missing_ok=True)
        return
    if sys.argv[1:] == ['environment']:
        (directory / 'environment.json').write_text(json.dumps(environment(), indent=2) + '\n')
        return
    if sys.argv[1:] == ['native-modes']:
        print(' '.join(NATIVE))
        return
    env = json.loads((directory / 'environment.json').read_text()) if (directory / 'environment.json').exists() else {'missing': True}
    result = matrix(Path('output'), env)
    result['complete'] = result['complete'] and not env.get('missing')
    (directory / 'verification.json').write_text(json.dumps(result, indent=2) + '\n')
    summary = os.environ.get('GITHUB_STEP_SUMMARY')
    if summary:
        with open(summary, 'a') as stream:
            stream.write('\n### Owned emulator results\n\n| Mode | Result | Assertions |\n| --- | --- | --- |\n')
            for case in result['cases']:
                stream.write(f'| {case["mode"]} | {case["status"]} | {case["assertions"]} |\n')
            stream.write('\nCompact API 36 navigation evidence: ' + ('passed' if valid_navigation(result['navigation']['36'], '36', env.get('sourceCommit'), env.get('runId')) else 'failed or unrun') + '.\n')
            stream.write('\nPhysical qualification and actual TalkBack traversal remain unrun.\n')
    print(f'Owned evidence: {len(result["cases"])} cases, {result["assertions"]} assertions, {result["imageCount"]} images; complete={result["complete"]}')
    if not result['complete']:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
