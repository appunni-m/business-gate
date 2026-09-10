#!/usr/bin/env python3
"""Owned compact-screen and IME tests in both native navigation configurations."""
import hashlib
import json
import os
from pathlib import Path
import re
import signal
import subprocess
import sys
import traceback

from verification_evidence import environment, views

NAVIGATION = {
    'threebutton': 'com.android.internal.systemui.navbar.threebutton',
    'gestural': 'com.android.internal.systemui.navbar.gestural',
}


def parse_probe_navigation(report):
    modes = re.findall(r'(?m)^INSTRUMENTATION_RESULT: stream=NAVIGATION_MODE ([012])\s*$', report)
    if len(report) > 4000 or 'FAIL' in report or len(modes) != 1 or not re.search(r'(?m)^INSTRUMENTATION_CODE: -1\s*$', report):
        raise RuntimeError('Invalid owned navigation resource result: ' + report[:1600])
    return modes[0]


def navigation_snapshot(run, read_mode=None):
    overlays = run('shell', 'cmd', 'overlay', 'list', 'android')
    entries = re.findall(r'^\[([ x])\] (com\.android\.internal\.systemui\.navbar\.(?:threebutton|twobutton|gestural))$', overlays, re.M)
    active = sorted(package for enabled, package in entries if enabled == 'x')
    available = sorted(package for _, package in entries)
    raw_mode = read_mode() if read_mode else run('shell', 'cmd', 'overlay', 'lookup', 'android', 'android:integer/config_navBarInteractionMode')
    try:
        mode = int(raw_mode, 0)
    except ValueError as error:
        raise RuntimeError('Unrecognized Android navigation resource: ' + raw_mode[:100]) from error
    if len(active) > 1 or not set(NAVIGATION.values()).issubset(available) or mode not in (0, 1, 2):
        raise RuntimeError('Navigation setup unavailable: ' + json.dumps({'active': active, 'available': available, 'mode': mode}))
    # No active overlay is valid: Android then uses its base/vendor resource value.
    return {'active': active, 'available': available, 'mode': mode}


def restore_navigation_commands(snapshot):
    if snapshot['active']:
        return [('shell', 'cmd', 'overlay', 'enable-exclusive', '--category', '--user', '0', snapshot['active'][0])]
    return [('shell', 'cmd', 'overlay', 'disable', '--user', '0', package) for package in snapshot['available']]


def main():
    Path('output/verification/navigation-failure.json').unlink(missing_ok=True)
    def interrupted(signum, frame):
        raise SystemExit('Owned emulator test interrupted')
    signal.signal(signal.SIGTERM, interrupted)
    signal.signal(signal.SIGHUP, interrupted)
    serial = os.environ['GATE_TEST_SERIAL']
    if not re.fullmatch(r'emulator-[0-9]+', serial):
        raise SystemExit('Use an isolated emulator')
    adb = [str(Path(os.environ['ANDROID_HOME']) / 'platform-tools/adb'), '-s', serial]
    def run(*args, timeout=30):
        try:
            return subprocess.run(adb + list(args), capture_output=True, text=True, timeout=timeout, check=True).stdout.strip()
        except subprocess.CalledProcessError as error:
            detail = ((error.stdout or '') + (error.stderr or '')).strip()[-1600:]
            raise RuntimeError('Navigation command ' + ' '.join(args) + f' exited {error.returncode}: ' + detail) from error
    api = run('shell', 'getprop', 'ro.build.version.sdk')
    if api not in ('29', '36'):
        raise SystemExit('Navigation matrix is defined for API 29 and 36')
    if api == '29':
        run('install', '-r', 'release-probe/build/outputs/apk/debug/release-probe-debug.apk')
    try:
        read_mode = (lambda: parse_probe_navigation(run('shell', 'am', 'instrument', '-r', '-w', '-e', 'mode', 'system-navigation',
                     'io.github.appunnim.businessgate.probe/.ReleaseProbe'))) if api == '29' else None
        run_matrix(run, api, read_mode)
    finally:
        if api == '29':
            run('uninstall', 'io.github.appunnim.businessgate.probe')


def run_matrix(run, api, read_mode):
    saved = {'navigation': navigation_snapshot(run, read_mode), 'size': run('shell', 'wm', 'size'), 'density': run('shell', 'wm', 'density'),
             'font': run('shell', 'settings', 'get', 'system', 'font_scale'), 'night': run('shell', 'cmd', 'uimode', 'night').split()[-1]}
    path = Path('output/verification') / f'navigation-api{api}.json'
    path.parent.mkdir(parents=True, exist_ok=True)
    result = {'schemaVersion': 1, 'api': api, 'complete': False, 'cases': [],
              'originalNavigation': saved['navigation'],
              'modeReadMethod': 'owned framework-resource probe' if read_mode else 'Android shell overlay lookup',
              'scope': 'Owned keyboard Back/IME and compact 200% layouts in both system navigation configurations. No gesture input or physical qualification.'}
    try:
        run('shell', 'am', 'force-stop', 'io.github.appunnim.businessgate.debug')
        run('shell', 'wm', 'size', '720x1280')
        run('shell', 'wm', 'density', '360')
        run('shell', 'settings', 'put', 'system', 'font_scale', '1.0')
        run('shell', 'cmd', 'uimode', 'night', 'no')
        for name, package in NAVIGATION.items():
            run('shell', 'cmd', 'overlay', 'enable-exclusive', '--category', '--user', '0', package)
            selected = navigation_snapshot(run, read_mode)
            if selected['active'] != [package] or selected['mode'] != {'threebutton': 0, 'gestural': 2}[name]:
                raise RuntimeError('Requested navigation configuration did not apply')
            folder = f'navigation-api{api}-{name}'
            image_folder = folder + '-images'
            env = dict(os.environ, GATE_TEST_REPORTS=folder, GATE_LAYOUT_REPORTS=image_folder,
                       GATE_LAYOUT_DISPLAYS='large', GATE_LAYOUT_THEMES='light', GATE_LAYOUT_SCALES='largest',
                       GATE_LAYOUT_ORIENTATIONS='portrait landscape')
            report_dir = Path('output') / folder
            report_dir.mkdir(parents=True, exist_ok=True)
            modes = ('interaction', 'layout-light-largest-portrait-large', 'layout-light-largest-landscape-large')
            for mode in modes:
                (report_dir / f'{mode}.txt').unlink(missing_ok=True)
            for image in (Path('output') / image_folder).glob('*.png'):
                image.unlink()
            run('shell', 'am', 'force-stop', 'io.github.appunnim.businessgate.debug')
            report = run('shell', 'am', 'instrument', '-w', '-e', 'mode', 'interaction',
                         'io.github.appunnim.businessgate.debug.test/io.github.appunnim.businessgate.GateInstrumentation', timeout=180)
            subprocess.run([sys.executable, 'scripts/assert_instrumentation.py', 'interaction'], input=report, text=True, env=env, check=True)
            subprocess.run(['scripts/ui-layout-tests.sh'], env=env, timeout=600, check=True)
            evidence = {'navigation': name, 'status': 'passed', 'environment': environment(), 'reports': [], 'images': []}
            for mode in modes:
                raw = (report_dir / f'{mode}.txt').read_bytes()
                matched = re.search(rb'PASS ([1-9][0-9]*) [^\n]+; mode=' + mode.encode() + rb'\s*$', raw)
                if not matched or b'FAIL' in raw:
                    raise RuntimeError('Incomplete navigation report')
                evidence['reports'].append({'mode': mode, 'assertions': int(matched[1]), 'sha256': hashlib.sha256(raw).hexdigest(),
                                            'metrics': [dict(re.findall(r'([a-z][a-z0-9_]{0,39})=([0-9]{1,20})', line)) for line in raw.decode('utf-8').splitlines() if line.startswith('METRIC ')]})
                if mode.startswith('layout-'):
                    for view in views(mode):
                        image = Path('output') / image_folder / f'{mode}-{view}.png'
                        raw = image.read_bytes()
                        if raw[:8] != b'\x89PNG\r\n\x1a\n':
                            raise RuntimeError('Missing owned navigation rendering')
                        evidence['images'].append({'file': image.name, 'sha256': hashlib.sha256(raw).hexdigest()})
            result['cases'].append(evidence)
        result['complete'] = True
    finally:
        errors = []
        commands = [('shell', 'am', 'force-stop', 'io.github.appunnim.businessgate.debug')]
        for key in ('size', 'density'):
            match = re.search(r'Override ' + key + r': ([0-9x]+)', saved[key])
            commands.append(('shell', 'wm', key, match[1] if match else 'reset'))
        commands.append(('shell', 'settings', 'delete', 'system', 'font_scale') if saved['font'] == 'null' else
                        ('shell', 'settings', 'put', 'system', 'font_scale', saved['font']))
        commands.append(('shell', 'cmd', 'uimode', 'night', saved['night']))
        commands.extend(restore_navigation_commands(saved['navigation']))
        for command in commands:
            try:
                run(*command)
            except (OSError, RuntimeError, subprocess.SubprocessError):
                errors.append(' '.join(command[:4]))
        try:
            for key in ('size', 'density'):
                if run('shell', 'wm', key) != saved[key]:
                    errors.append(key + ' readback')
            if run('shell', 'settings', 'get', 'system', 'font_scale') != saved['font']:
                errors.append('font readback')
            if run('shell', 'cmd', 'uimode', 'night').split()[-1] != saved['night']:
                errors.append('theme readback')
            if navigation_snapshot(run, read_mode) != saved['navigation']:
                errors.append('navigation readback')
        except (OSError, RuntimeError, subprocess.SubprocessError):
            errors.append('settings readback')
        result['settingsRestored'] = not errors
        path.write_text(json.dumps(result, indent=2) + '\n')
        if errors:
            raise SystemExit('Could not restore owned test configuration: ' + ', '.join(errors))
    print(f'PASS compact API {api} keyboard/IME and 200% layouts in both navigation configurations')


if __name__ == '__main__':
    try:
        main()
    except BaseException:
        detail = traceback.format_exc(limit=5)[-3500:]
        path = Path('output/verification/navigation-failure.json')
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps({'complete': False, 'failure': detail}, indent=2) + '\n')
        if os.environ.get('GITHUB_ACTIONS') == 'true':
            escaped = detail.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
            print('::error title=Owned navigation verification::' + escaped, flush=True)
        raise
