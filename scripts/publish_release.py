#!/usr/bin/env python3
"""Publish a versioned prerelease, then advance the continuous download channel."""
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile

directory = Path('output/delivery')
info = json.loads((directory / 'build-info.json').read_text())
repository = os.environ['GITHUB_REPOSITORY']
tag = info['releaseTag']
title = 'Business Gate ' + info['versionName']
notes = str(directory / 'RELEASE_NOTES.md')


def gh(*args, **kwargs):
    return subprocess.run(['gh', *args], check=True, text=True, **kwargs)


pages = json.loads(gh('api', '--paginate', '--slurp', f'repos/{repository}/releases?per_page=100', capture_output=True).stdout)
releases = [release for page in pages for release in page]
existing = next((release for release in releases if release['tag_name'] == tag), None)
assets = ['business-gate.apk', 'SHA256SUMS', 'build-info.json']
# A retry can resume an interrupted draft, but cannot change a published binary.
if existing and not existing['draft']:
    with tempfile.TemporaryDirectory(prefix='business-gate-release-') as temporary:
        gh('release', 'download', tag, '--repo', repository, '--dir', temporary)
        for name in assets:
            if (Path(temporary) / name).read_bytes() != (directory / name).read_bytes():
                raise SystemExit('Published version differs; start a new workflow run instead of replacing it.')
elif existing:
    gh('release', 'upload', tag, *(str(directory / name) for name in assets),
       '--repo', repository, '--clobber')
    gh('release', 'edit', tag, '--repo', repository, '--notes-file', notes, '--draft=false')
else:
    # Draft creation keeps partial asset uploads out of the public release list.
    gh('release', 'create', tag, *(str(directory / name) for name in assets),
       '--repo', repository, '--target', info['commit'], '--title', title,
       '--notes-file', notes, '--prerelease', '--draft')
    gh('release', 'edit', tag, '--repo', repository, '--draft=false')

# Only advance the rolling channel. An older queued run must never replace a newer APK.
rolling = next((release for release in releases if release['tag_name'] == 'development'), None)
if rolling:
    match = re.search(r'<!-- version-code: (\d+) -->', rolling['body'] or '')
    if not match:
        raise SystemExit('Existing development release has no version marker; refusing to overwrite it.')
    if int(match[1]) > info['versionCode']:
        print('Versioned APK published; a newer build already owns the continuous channel.')
        raise SystemExit(0)
    gh('release', 'upload', 'development', str(directory / 'business-gate.apk'),
       '--repo', repository, '--clobber')
    gh('api', '--method', 'PATCH', f'repos/{repository}/git/refs/tags/development',
       '-f', 'sha=' + info['commit'], '-F', 'force=true')
    gh('release', 'edit', 'development', '--repo', repository,
       '--title', 'Latest development APK', '--notes-file', notes, '--prerelease', '--draft=false')
else:
    gh('release', 'create', 'development', str(directory / 'business-gate.apk'),
       '--repo', repository, '--target', info['commit'], '--title', 'Latest development APK',
       '--notes-file', notes, '--prerelease', '--draft')
    gh('release', 'edit', 'development', '--repo', repository, '--draft=false')
url = f'https://github.com/{repository}/releases/download/development/business-gate.apk'
print('Published installable APK: ' + url)
summary = os.environ.get('GITHUB_STEP_SUMMARY')
if summary:
    with open(summary, 'a') as stream:
        stream.write(f'### APK ready\n\n[Download Business Gate]({url}) · '
                     f'[Versioned release](https://github.com/{repository}/releases/tag/{tag})\n\n'
                     'Android 10 or newer. Connected-app blocking remains disabled.\n')
