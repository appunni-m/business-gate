#!/usr/bin/env python3
"""Describe the actual distributable, including a checksum and source provenance."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile
from compatibility import validate
from verification_evidence import ALL_MODES, valid_navigation

apk = Path('output/delivery/business-gate.apk')
tools = Path(os.environ['ANDROID_HOME']) / 'build-tools/35.0.0'
badging = subprocess.check_output([str(tools / 'aapt2'), 'dump', 'badging', str(apk)], text=True)
package = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging)
if not package or package[1] != 'io.github.appunnim.businessgate':
    raise SystemExit('Unexpected distribution package')
code = int(package[2])
name = package[3]
repository = os.environ['GITHUB_REPOSITORY']
commit = os.environ['GITHUB_SHA']
run_url = f"https://github.com/{repository}/actions/runs/{os.environ['GITHUB_RUN_ID']}"
sha = hashlib.sha256(apk.read_bytes()).hexdigest()
verification_path = apk.parent / 'verification.json'
verification = json.loads(verification_path.read_text())
environment = verification['environment']
if (verification['complete'] is not True or environment['sourceCommit'] != commit or environment['workingTreeModified'] is not False
        or environment['runId'] != os.environ['GITHUB_RUN_ID'] or environment['device']['api'] != '36'
        or len(verification['cases']) != len(ALL_MODES)
        or {case['mode'] for case in verification['cases']} != set(ALL_MODES)
        or any(case['status'] != 'passed' or case['assertions'] < 1 for case in verification['cases'])
        or verification['imageCount'] != 152):
    raise SystemExit('Incomplete or mismatched owned verification evidence')
if any(not valid_navigation(verification.get('navigation', {}).get(api), api, commit, os.environ['GITHUB_RUN_ID']) for api in ('29', '36')):
    raise SystemExit('Incomplete compact navigation evidence')
minimum_ui = verification.get('minimumUi', {})
minimum_env = minimum_ui.get('environment', {})
if (minimum_env.get('device', {}).get('api') != '29' or minimum_env.get('sourceCommit') != commit
        or minimum_env.get('runId') != os.environ['GITHUB_RUN_ID']
        or len(minimum_ui.get('cases', [])) != 9
        or {case['mode'] for case in minimum_ui['cases']} != {'ui', 'focus', 'dialogs', 'interaction', 'service-lifecycle', 'prepare-task', 'verify-task', 'reminder', 'reminder-channel'}
        or any(case['status'] != 'passed' or case['assertions'] < 1 for case in minimum_ui['cases'])):
    raise SystemExit('Minimum-API UI evidence is incomplete or mismatched')
upgrade_environment = json.loads(Path('output/verification/environment.json').read_text())
if upgrade_environment['device']['api'] != '29' or upgrade_environment['sourceCommit'] != commit or upgrade_environment['runId'] != os.environ['GITHUB_RUN_ID']:
    raise SystemExit('Minimum-API signed upgrade environment differs')
upgrade = []
for mode in ('prepare-upgrade', 'verify-upgrade'):
    raw = Path('output/device-tests', mode + '.txt').read_bytes()
    report = raw.decode('utf-8')
    matched = re.search(r'(?m)^(?:INSTRUMENTATION_RESULT: stream=)?PASS ([0-9]+) installed-release assertions; mode=' + mode + r'\s*$', report)
    if not matched or int(matched[1]) < 1 or 'FAIL' in report or len(raw) > 64_000:
        raise SystemExit('Incomplete signed upgrade evidence')
    upgrade.append({'mode': mode, 'assertions': int(matched[1]), 'reportSha256': hashlib.sha256(raw).hexdigest()})
previous = Path('output/previous/business-gate.apk')
verification['signedUpgrade'] = {'environment': upgrade_environment, 'cases': upgrade,
    'currentApkSha256': sha, 'previousApkSha256': hashlib.sha256(previous.read_bytes()).hexdigest() if previous.exists() else None}
verification_path.write_text(json.dumps(verification, indent=2) + '\n')
with zipfile.ZipFile(apk) as archive:
    compatibility = validate(lambda name: archive.read('assets/' + name))
tag = f'build-{code}'
info = {
    'applicationId': package[1], 'versionCode': code, 'versionName': name,
    'minimumAndroidApi': int(re.search(r"^(?:minSdkVersion|sdkVersion):'(\d+)'", badging, re.M)[1]),
    'targetAndroidApi': int(re.search(r"^targetSdkVersion:'(\d+)'", badging, re.M)[1]),
    'commit': commit, 'workflowRun': run_url, 'releaseTag': tag,
    'sha256': sha, 'signingCertificateSha256': Path('config/signing-certificate.sha256').read_text().strip(),
    'channel': 'development', **compatibility,
    'verificationSha256': hashlib.sha256(verification_path.read_bytes()).hexdigest(),
}
if info['minimumAndroidApi'] != 29 or info['targetAndroidApi'] != 36:
    raise SystemExit('Distribution SDK baseline changed')
(apk.parent / 'build-info.json').write_text(json.dumps(info, indent=2) + '\n')
files = [apk, apk.parent / 'build-info.json', verification_path]
(apk.parent / 'SHA256SUMS').write_text(''.join(
    f'{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.name}\n' for path in files))
capability = ('Connected-app actions are supported only for the qualified environments listed in build-info.json.'
              if compatibility['connectedAppActionsEnabled'] else
              '**Connected-app blocking remains disabled.** This build provides the native interface and local controls; no connected-app environment is physically qualified.')
notes = f'''Business Gate {name} — development build.

Download **business-gate.apk** below and open it on Android 10 or newer. Allow installation from your browser or file manager if Android asks. Later builds from this channel install as updates and preserve local choices.

{capability}

The APK is signed with the pinned private publisher key. Automated checks cover the safety suite, schema, independent branding, lint, release contents, native instrumentation on API 36, and signed installation on API 29.

- [Source commit](https://github.com/{repository}/commit/{commit})
- [Build and verification]({run_url})
- [Versioned release](https://github.com/{repository}/releases/tag/{tag}) with checksums, build metadata and verification.json
- APK SHA-256: `{sha}`
- Version code: `{code}`

This is a development prerelease, not a store publication.
<!-- version-code: {code} -->
'''
(apk.parent / 'RELEASE_NOTES.md').write_text(notes)
print(f'Prepared release {tag}: {name}, version code {code}')
