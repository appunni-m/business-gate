#!/usr/bin/env python3
"""Exercise actual debug notifications and reset only that synthetic app between channel cases."""
import os
from pathlib import Path
import re
import signal
import subprocess
import sys


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
    def run(*args, timeout=30):
        return subprocess.run(adb + list(args), capture_output=True, text=True, timeout=timeout, check=True).stdout
    try:
        for mode in ('reminder', 'reminder-channel'):
            if 'Success' not in run('shell', 'pm', 'clear', target):
                raise SystemExit('Could not reset the synthetic debug notification state')
            result = run('shell', 'am', 'instrument', '-w', '-e', 'mode', mode, '-e', 'trace', 'true',
                         target + '.test/io.github.appunnim.businessgate.GateInstrumentation', timeout=180)
            subprocess.run([sys.executable, 'scripts/assert_instrumentation.py', mode], input=result, text=True, check=True)
    finally:
        if 'Success' not in run('shell', 'pm', 'clear', target):
            raise SystemExit('Could not clean up the synthetic debug notification state')


if __name__ == '__main__':
    main()
