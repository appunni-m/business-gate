#!/usr/bin/env python3
"""Preserve the signed-in development emulator outside temporary and project folders."""
import argparse
import os
from pathlib import Path
import subprocess

NAME = 'BusinessGateIntegration'
IMAGE = 'system-images;android-36;google_apis_playstore;arm64-v8a'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=('status', 'create', 'start'))
    args = parser.parse_args()
    base = Path.home() / '.local/share/business-gate/android'
    avds = base / 'avd'
    descriptor, data = avds / (NAME + '.ini'), avds / (NAME + '.avd')
    if args.action == 'status':
        print('Persistent emulator: ' + str(data))
        print('Available' if descriptor.is_file() and data.is_dir() else 'Missing; create does not restore any previous sign-in')
        return
    sdk = Path(os.environ['ANDROID_HOME'])
    env = dict(os.environ, ANDROID_AVD_HOME=str(avds), ANDROID_USER_HOME=str(base / 'user'))
    if args.action == 'create':
        if descriptor.exists() or data.exists():
            raise SystemExit('Existing emulator data found. Use start; it will not be overwritten.')
        avds.mkdir(parents=True, exist_ok=True, mode=0o700)
        (base / 'user').mkdir(parents=True, exist_ok=True, mode=0o700)
        subprocess.run([str(sdk / 'cmdline-tools/latest/bin/avdmanager'), 'create', 'avd', '--name', NAME,
                        '--package', IMAGE, '--device', 'pixel_6', '--path', str(data)], input='no\n',
                       text=True, check=True, env=env, timeout=120)
        return
    if not descriptor.is_file() or not data.is_dir():
        raise SystemExit('Persistent emulator missing. Create it explicitly; no signed-in device will be replaced.')
    check = subprocess.run([str(sdk / 'platform-tools/adb'), '-s', 'emulator-5574', 'emu', 'avd', 'name'],
                           text=True, capture_output=True, timeout=10)
    if check.returncode == 0:
        if NAME in check.stdout.splitlines():
            print('Existing account emulator is already running on emulator-5574')
            return
        raise SystemExit('Port 5574 belongs to a different emulator; leaving it untouched.')
    # Normal writable userdata, with no wipe, force-create, snapshot reset or data deletion.
    log = base / 'integration-emulator.log'
    with log.open('ab') as output:
        process = subprocess.Popen([str(sdk / 'emulator/emulator'), '-avd', NAME, '-port', '5574',
                                    '-no-audio', '-no-boot-anim', '-no-snapshot', '-gpu', 'swiftshader_indirect', '-memory', '3072'],
                                   env=env, stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
    print('Started emulator process ' + str(process.pid) + '; data: ' + str(data))
    print('Startup log: ' + str(log))


if __name__ == '__main__':
    main()
