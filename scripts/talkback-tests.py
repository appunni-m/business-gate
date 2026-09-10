#!/usr/bin/env python3
"""Temporarily enable the installed screen reader for owned emulator tests; restore settings."""
import hashlib
import json
import os
from pathlib import Path
import re
import signal
import subprocess
import sys


def main():
    def interrupted(signum, frame):
        raise SystemExit('Owned screen-reader test interrupted')
    signal.signal(signal.SIGTERM, interrupted)
    signal.signal(signal.SIGHUP, interrupted)
    serial = os.environ['GATE_TEST_SERIAL']
    if not re.fullmatch(r'emulator-[0-9]+', serial):
        raise SystemExit('Use a dedicated emulator for the owned screen-reader test')
    adb = [str(Path(os.environ['ANDROID_HOME']) / 'platform-tools/adb'), '-s', serial]

    def run(*args, timeout=30):
        return subprocess.run(adb + list(args), check=True, capture_output=True, text=True, timeout=timeout).stdout.strip()

    provider = 'com.google.android.marvin.talkback'
    if not run('shell', 'pm', 'path', provider).startswith('package:'):
        raise SystemExit('The dedicated emulator has no installed TalkBack; this case is unrun')
    saved = {key: run('shell', 'settings', 'get', 'secure', key) for key in ('enabled_accessibility_services', 'accessibility_enabled')}
    services = [] if saved['enabled_accessibility_services'] == 'null' else saved['enabled_accessibility_services'].split(':')
    if not any(value.startswith(provider + '/') for value in services):
        services.append(provider + '/' + provider + '.TalkBackService')
    output = ''
    try:
        run('install', '-r', 'app/build/outputs/apk/debug/app-debug.apk')
        run('install', '-r', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')
        run('shell', 'settings', 'put', 'secure', 'enabled_accessibility_services', ':'.join(services))
        run('shell', 'settings', 'put', 'secure', 'accessibility_enabled', '1')
        run('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
        run('shell', 'wm', 'dismiss-keyguard')
        run('shell', 'am', 'force-stop', 'io.github.appunnim.businessgate.debug')
        output = run('shell', 'am', 'instrument', '-w', '-e', 'mode', 'talkback', '-e', 'trace', 'true',
                     'io.github.appunnim.businessgate.debug.test/io.github.appunnim.businessgate.GateInstrumentation', timeout=180)
    finally:
        errors = []
        try:
            run('shell', 'am', 'force-stop', 'io.github.appunnim.businessgate.debug')
        except (OSError, subprocess.SubprocessError):
            errors.append('debug process cleanup')
        for key, value in saved.items():
            try:
                if value == 'null':
                    run('shell', 'settings', 'delete', 'secure', key)
                else:
                    run('shell', 'settings', 'put', 'secure', key, value)
                if run('shell', 'settings', 'get', 'secure', key) != value:
                    errors.append(key)
            except (OSError, subprocess.SubprocessError):
                errors.append(key)
        if errors:
            raise SystemExit('Could not restore emulator settings: ' + ', '.join(errors))
    result = subprocess.run([sys.executable, 'scripts/assert_instrumentation.py', 'talkback'], input=output, text=True)
    if result.returncode:
        raise SystemExit(result.returncode)
    version = run('shell', 'pm', 'list', 'packages', '--show-versioncode', provider)
    match = re.search(r'versionCode:([0-9]+)', version)
    report = {'schemaVersion': 1, 'provider': 'TalkBack', 'providerVersionCode': match[1] if match else 'unavailable',
              'androidApi': run('shell', 'getprop', 'ro.build.version.sdk'),
              'apkSha256': hashlib.sha256(Path('app/build/outputs/apk/debug/app-debug.apk').read_bytes()).hexdigest(),
              'result': 'passed', 'settingsRestored': True,
              'scope': 'Actual TalkBack native Tab traversal and owned-node focus/state checks. Speech audio, reader-specific shortcuts and physical qualification are not measured.'}
    Path('output/device-tests/talkback-evidence.json').write_text(json.dumps(report, indent=2) + '\n')


if __name__ == '__main__':
    main()
