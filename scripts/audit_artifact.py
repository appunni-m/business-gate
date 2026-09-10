#!/usr/bin/env python3
"""Inspect the built release APK, its actual DEX classes and merged manifest."""
import json
import os
import re
import struct
import subprocess
import sys
import zipfile
from pathlib import Path
from compatibility import validate

apk = Path(sys.argv[1])
mapping_file = Path('app/build/outputs/mapping/release/mapping.txt')
allowed_originals = ('io.github.appunnim.businessgate.', 'com.android.tools.r8.')
allowed_classes = set()
for line in mapping_file.read_text().splitlines():
    match = re.match(r'^([^ #\s].*) -> (.*):$', line)
    if match:
        original, mapped = match.groups()
        assert original.startswith(allowed_originals) or original == 'java.lang.Record', f'Unexpected source/runtime class: {original}'
        allowed_classes.add('L' + mapped.replace('.', '/') + ';')
class_count = 0
with zipfile.ZipFile(apk) as archive:
    names = archive.namelist()
    assert not any(n.endswith('.so') or n.startswith('lib/') for n in names), 'Unexpected native library'
    compatibility = validate(lambda name: archive.read('assets/' + name))
    for name in names:
        if not name.endswith('.dex'):
            continue
        data = archive.read(name)
        sn, so = struct.unpack_from('<II', data, 0x38)
        tn, to = struct.unpack_from('<II', data, 0x40)
        cn, co = struct.unpack_from('<II', data, 0x60)
        strings = []
        for i in range(sn):
            offset = struct.unpack_from('<I', data, so+i*4)[0]
            while data[offset] & 128:
                offset += 1
            offset += 1
            strings.append(data[offset:data.index(b'\0', offset)].decode('utf-8', errors='replace'))
        types = [strings[struct.unpack_from('<I', data, to+i*4)[0]] for i in range(tn)]
        for i in range(cn):
            descriptor = types[struct.unpack_from('<I', data, co+i*32)[0]]
            assert descriptor in allowed_classes, f'Unmapped runtime class: {descriptor}'
            class_count += 1
        assert not any('GateInstrumentation' in s or 'ReleaseProbe' in s for s in strings), 'Test harness in release DEX'
sdk = Path(os.environ['ANDROID_HOME'])
aapt = sdk / 'build-tools/35.0.0/aapt2'
permissions = subprocess.check_output([str(aapt), 'dump', 'permissions', str(apk)], text=True)
actual = set(re.findall(r"uses-permission: name='([^']+)'", permissions))
assert actual == {'android.permission.POST_NOTIFICATIONS'}, actual
manifest = subprocess.check_output([str(aapt), 'dump', 'xmltree', '--file', 'AndroidManifest.xml', str(apk)], text=True)
assert re.search(r'android:allowBackup.*0x0', manifest), 'Backup not disabled'
assert not re.search(r'android:debuggable.*0xffffffff', manifest), 'Release is debuggable'
assert 'android.permission.BIND_ACCESSIBILITY_SERVICE' in manifest
assert 'android.permission.BIND_NOTIFICATION_LISTENER_SERVICE' in manifest
assert 'backup_rules' in manifest or 'android:fullBackupContent' in manifest
print(f'PASS release artifact audit: {class_count} mapped app/compiler classes; no runtime SDK/native library; minimal permissions; backup off; no test harness; validated compatibility={compatibility["connectedAppActionsEnabled"]}')
