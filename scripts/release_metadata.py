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
}
if info['minimumAndroidApi'] != 29 or info['targetAndroidApi'] != 36:
    raise SystemExit('Distribution SDK baseline changed')
(apk.parent / 'build-info.json').write_text(json.dumps(info, indent=2) + '\n')
files = [apk, apk.parent / 'build-info.json']
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
- [Versioned release](https://github.com/{repository}/releases/tag/{tag}) with checksums and build metadata
- APK SHA-256: `{sha}`
- Version code: `{code}`

This is a development prerelease, not a store publication.
<!-- version-code: {code} -->
'''
(apk.parent / 'RELEASE_NOTES.md').write_text(notes)
print(f'Prepared release {tag}: {name}, version code {code}')
