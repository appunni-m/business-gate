#!/usr/bin/env python3
"""Kill only the background debug process and verify Android restores its existing task."""
import hashlib
import json
import os
from pathlib import Path
import re
import signal
import subprocess
import sys
import time


def main():
    def interrupted(signum, frame):
        raise SystemExit('Owned emulator test interrupted')
    signal.signal(signal.SIGTERM, interrupted)
    signal.signal(signal.SIGHUP, interrupted)
    serial = os.environ['GATE_TEST_SERIAL']
    if not re.fullmatch(r'emulator-[0-9]+', serial):
        raise SystemExit('Use an isolated emulator')
    adb = [str(Path(os.environ['ANDROID_HOME']) / 'platform-tools/adb'), '-s', serial]
    target = 'io.github.appunnim.businessgate.debug'
    component = target + '/io.github.appunnim.businessgate.ui.MainActivity'

    def run(*args, timeout=30):
        return subprocess.run(adb + list(args), capture_output=True, text=True, timeout=timeout, check=True).stdout.strip()

    def pid():
        result = subprocess.run(adb + ['shell', 'pidof', target], capture_output=True, text=True, timeout=30)
        if result.returncode not in (0, 1):
            raise RuntimeError('Could not inspect the owned process')
        return result.stdout.strip()

    def probe(mode):
        report = run('shell', 'am', 'instrument', '-w', '-e', 'mode', mode,
                     'io.github.appunnim.businessgate.probe/.ReleaseProbe', timeout=120)
        subprocess.run([sys.executable, 'scripts/assert_instrumentation.py', mode], input=report, text=True, check=True)

    def launch():
        run('shell', 'am', 'start', '-W', '-a', 'android.intent.action.MAIN', '-c',
            'android.intent.category.LAUNCHER', '-f', '0x10200000', '-n', component)

    run('install', '-r', 'release-probe/build/outputs/apk/debug/release-probe-debug.apk')
    try:
        run('shell', 'am', 'force-stop', target)
        launch()
        probe('prepare-task')
        before = pid()
        if not before:
            raise SystemExit('No owned process to test')
        run('shell', 'input', 'keyevent', 'KEYCODE_HOME')
        time.sleep(2)
        run('shell', 'am', 'kill', target)
        deadline = time.monotonic() + 10
        while pid():
            if time.monotonic() >= deadline:
                raise SystemExit('Background debug process did not die; task restoration is unverified')
            time.sleep(.2)
        launch()
        after = pid()
        if not after or before == after:
            raise SystemExit('Process replacement was not established')
        probe('verify-task')
        apk = Path('app/build/outputs/apk/debug/app-debug.apk')
        (Path('output') / os.environ.get('GATE_TEST_REPORTS', 'device-tests') / 'saved-task-evidence.json').write_text(json.dumps({
            'schemaVersion': 1, 'apkSha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
            'processDeathObserved': True, 'processChanged': True, 'result': 'passed',
            'scope': 'Existing debug task restored after background am kill; no force-stop between preparation and restoration.'}, indent=2) + '\n')
    finally:
        run('uninstall', 'io.github.appunnim.businessgate.probe')


if __name__ == '__main__':
    main()
