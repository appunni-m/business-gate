#!/usr/bin/env python3
"""Independently verify anonymous release downloads after publication."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import time
import urllib.request
import zipfile

from compatibility import validate
from sign_apk import signing_fingerprint

PACKAGE = 'io.github.appunnim.businessgate'


def download(url):
    request = urllib.request.Request(url, headers={'User-Agent': 'Business-Gate-release-verifier'})
    with urllib.request.urlopen(request, timeout=20) as response:
        value = response.read(32 * 1024 * 1024 + 1)
    if len(value) > 32 * 1024 * 1024:
        raise ValueError('Public asset exceeds the verification size limit')
    return value


def inspect_apk(data, info, pin):
    sdk = Path(os.environ['ANDROID_HOME']) / 'build-tools/35.0.0'
    with tempfile.TemporaryDirectory(prefix='business-gate-public-') as directory:
        apk = Path(directory) / 'business-gate.apk'
        apk.write_bytes(data)
        if signing_fingerprint(sdk, apk) != pin:
            raise ValueError('Public APK publisher certificate differs')
        badging = subprocess.check_output([str(sdk / 'aapt2'), 'dump', 'badging', str(apk)], text=True)
        package = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging)
        minimum = re.search(r"^(?:minSdkVersion|sdkVersion):'(\d+)'", badging, re.M)
        target = re.search(r"^targetSdkVersion:'(\d+)'", badging, re.M)
        if not package or not minimum or not target or (
                package[1], int(package[2]), package[3], int(minimum[1]), int(target[1])) != (
                PACKAGE, info['versionCode'], info['versionName'], 29, 36):
            raise ValueError('Public APK package, version or SDK baseline differs')
        with zipfile.ZipFile(apk) as archive:
            capability = validate(lambda name: archive.read('assets/' + name))
        if any(info.get(key) != value for key, value in capability.items()):
            raise ValueError('Public APK capability metadata differs')


def verify_version(repository, tag, pin, fetch, inspect, expected=None):
    if not re.fullmatch(r'build-[1-9][0-9]{0,9}', tag):
        raise ValueError('Invalid versioned release tag')
    base = f'https://github.com/{repository}/releases/download/{tag}/'
    files = {name: fetch(base + name) for name in ('business-gate.apk', 'build-info.json', 'SHA256SUMS')}
    entries = files['SHA256SUMS'].decode('ascii').splitlines()
    checksums = {}
    for entry in entries:
        match = re.fullmatch(r'([0-9a-f]{64})  (business-gate\.apk|build-info\.json|verification\.json)', entry)
        if not match or match[2] in checksums:
            raise ValueError('Invalid or duplicate public checksum entry')
        checksums[match[2]] = match[1]
    if set(checksums) not in ({'business-gate.apk', 'build-info.json'}, {'business-gate.apk', 'build-info.json', 'verification.json'}):
        raise ValueError('Missing public checksum entry')
    if 'verification.json' in checksums:
        files['verification.json'] = fetch(base + 'verification.json')
    for name, digest in checksums.items():
        if hashlib.sha256(files[name]).hexdigest() != digest:
            raise ValueError('Public asset checksum differs: ' + name)
    info = json.loads(files['build-info.json'])
    if expected is not None and info != expected:
        raise ValueError('Public metadata differs from the verified build')
    if ('verificationSha256' in info) != ('verification.json' in checksums):
        raise ValueError('Public verification manifest is missing or unbound')
    if 'verificationSha256' in info:
        evidence = json.loads(files['verification.json'])
        if (info['verificationSha256'] != checksums['verification.json'] or evidence.get('complete') is not True
                or evidence.get('environment', {}).get('sourceCommit') != info.get('commit')
                or evidence.get('signedUpgrade', {}).get('currentApkSha256') != info.get('sha256')):
            raise ValueError('Public verification evidence differs from the APK')
    if (info.get('applicationId') != PACKAGE or info.get('releaseTag') != tag
            or type(info.get('versionCode')) is not int or not 1 <= info['versionCode'] <= 2_100_000_000
            or tag != f"build-{info['versionCode']}" or info.get('minimumAndroidApi') != 29
            or info.get('targetAndroidApi') != 36 or info.get('signingCertificateSha256') != pin
            or info.get('sha256') != checksums['business-gate.apk']
            or not re.fullmatch(r'[0-9a-f]{40}', info.get('commit', ''))
            or not re.fullmatch(re.escape(f'https://github.com/{repository}/actions/runs/') + r'[0-9]+', info.get('workflowRun', ''))):
        raise ValueError('Public release identity or provenance differs')
    reference = json.loads(fetch(f'https://api.github.com/repos/{repository}/git/ref/tags/{tag}'))
    if reference.get('object', {}).get('type') != 'commit' or reference['object'].get('sha') != info['commit']:
        raise ValueError('Versioned tag differs from public metadata')
    inspect(files['business-gate.apk'], info, pin)
    return info, files['business-gate.apk']


def verify(repository, expected, pin, fetch=download, inspect=inspect_apk):
    if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', repository):
        raise ValueError('Invalid repository')
    info, apk = verify_version(repository, expected['releaseTag'], pin, fetch, inspect, expected)
    rolling = json.loads(fetch(f'https://api.github.com/repos/{repository}/releases/tags/development'))
    notes = rolling.get('body') or ''
    markers = re.findall(r'<!-- version-code: ([0-9]+) -->', notes)
    if len(markers) != 1 or int(markers[0]) < info['versionCode']:
        raise ValueError('Rolling version is missing, ambiguous or stale')
    rolling_code = int(markers[0])
    rolling_info, rolling_apk = info, apk
    if rolling_code > info['versionCode']:
        rolling_info, rolling_apk = verify_version(repository, f'build-{rolling_code}', pin, fetch, inspect)
    if rolling_info['sha256'] not in notes:
        raise ValueError('Rolling notes checksum differs')
    reference = json.loads(fetch(f'https://api.github.com/repos/{repository}/git/ref/tags/development'))
    if reference.get('object', {}).get('type') != 'commit' or reference['object'].get('sha') != rolling_info['commit']:
        raise ValueError('Rolling tag differs')
    if fetch(f'https://github.com/{repository}/releases/download/development/business-gate.apk') != rolling_apk:
        raise ValueError('Rolling APK differs from its versioned release')
    return {'schemaVersion': 1, 'commit': info['commit'], 'versionCode': info['versionCode'],
            'sha256': info['sha256'], 'publisherCertificateSha256': pin,
            'versionedUrl': f'https://github.com/{repository}/releases/tag/{info["releaseTag"]}',
            'rollingVersionCode': rolling_code, 'anonymousDownloadVerified': True,
            'connectedAppActionsEnabled': info['connectedAppActionsEnabled']}


def verify_with_retries(operation, attempts=4, sleep=time.sleep):
    for attempt in range(attempts):
        try:
            return operation()
        except (ValueError, KeyError, TypeError, UnicodeError, OSError, subprocess.SubprocessError, zipfile.BadZipFile, AssertionError):
            if attempt + 1 == attempts:
                raise
            print(f'Public release not consistent yet; retry {attempt + 2}/{attempts}.')
            sleep(3 * (attempt + 1))


def main():
    directory = Path('output/delivery')
    expected = json.loads((directory / 'build-info.json').read_text())
    repository = os.environ['GITHUB_REPOSITORY']
    pin = Path('config/signing-certificate.sha256').read_text().strip()
    versioned = f'https://github.com/{repository}/releases/tag/{expected["releaseTag"]}'
    summary = os.environ.get('GITHUB_STEP_SUMMARY')
    try:
        report = verify_with_retries(lambda: verify(repository, expected, pin))
        (directory / 'public-verification.json').write_text(json.dumps(report, indent=2) + '\n')
    except Exception:
        if summary:
            with open(summary, 'a') as stream:
                stream.write(f'\n### Public verification failed\n\nCheck the [versioned release]({versioned}); the rolling download was not confirmed. Published versioned assets were not replaced.\n')
        raise
    print('PASS anonymous public APK, checksums, metadata, certificate and rolling channel')
    if summary:
        with open(summary, 'a') as stream:
            stream.write(f'\n### Public download verified\n\nBuild `{report["versionCode"]}`: [versioned release]({versioned}). '
                         f'Continuous channel: `{report["rollingVersionCode"]}`. Certificate, checksums and source match.\n')


if __name__ == '__main__':
    main()
