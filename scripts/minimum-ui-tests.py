#!/usr/bin/env python3
"""Verify native UI, keyboard and saved-task behavior on the minimum Android API."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys


def main():
    serial = os.environ['GATE_TEST_SERIAL']
    if not re.fullmatch(r'emulator-[0-9]+', serial):
        raise SystemExit('Use an isolated emulator')
    adb = [str(Path(os.environ['ANDROID_HOME']) / 'platform-tools/adb'), '-s', serial]
    def run(*args):
        return subprocess.run(adb + list(args), capture_output=True, text=True, check=True, timeout=180).stdout
    if run('shell', 'getprop', 'ro.build.version.sdk').strip() != '29':
        raise SystemExit('Minimum UI suite requires API 29')
    folder = Path('output/api29-device-tests')
    folder.mkdir(parents=True, exist_ok=True)
    modes = ('ui', 'focus', 'dialogs', 'interaction', 'service-lifecycle', 'prepare-task', 'verify-task', 'reminder', 'reminder-channel')
    for mode in modes:
        (folder / f'{mode}.txt').unlink(missing_ok=True)
    subprocess.run([sys.executable, 'scripts/verification_evidence.py', 'environment'], check=True)
    environment = Path('output/verification/environment.json')
    environment.rename('output/verification/api29-environment.json')
    run('install', '-r', 'app/build/outputs/apk/debug/app-debug.apk')
    run('install', '-r', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')
    env = dict(os.environ, GATE_TEST_REPORTS='api29-device-tests')
    for mode in modes[:5]:
        run('shell', 'am', 'force-stop', 'io.github.appunnim.businessgate.debug')
        report = run('shell', 'am', 'instrument', '-w', '-e', 'mode', mode,
                     'io.github.appunnim.businessgate.debug.test/io.github.appunnim.businessgate.GateInstrumentation')
        subprocess.run([sys.executable, 'scripts/assert_instrumentation.py', mode], input=report, text=True, env=env, check=True)
    subprocess.run([sys.executable, 'scripts/reminder-tests.py'], env=env, check=True, timeout=420)
    subprocess.run([sys.executable, 'scripts/saved-task-tests.py'], env=env, check=True, timeout=300)
    subprocess.run([sys.executable, 'scripts/navigation-tests.py'], check=True, timeout=1400)
    cases = []
    for mode in modes:
        raw = (folder / f'{mode}.txt').read_bytes()
        match = re.search(rb'PASS ([1-9][0-9]*) [^\n]+; mode=' + mode.encode() + rb'\s*$', raw)
        if not match or b'FAIL' in raw:
            raise SystemExit('Minimum UI evidence failed')
        cases.append({'mode': mode, 'status': 'passed', 'assertions': int(match[1]),
                      'reportSha256': hashlib.sha256(raw).hexdigest()})
    path = Path('output/verification/verification.json')
    evidence = json.loads(path.read_text())
    evidence['navigation']['29'] = json.loads(Path('output/verification/navigation-api29.json').read_text())
    evidence['minimumUi'] = {'environment': json.loads(Path('output/verification/api29-environment.json').read_text()), 'cases': cases}
    path.write_text(json.dumps(evidence, indent=2) + '\n')
    print('PASS minimum API UI, focus, dialogs, IME/Back and actual saved-task restoration')


if __name__ == '__main__':
    main()
