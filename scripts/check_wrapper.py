#!/usr/bin/env python3
"""Pinned against the official Gradle 8.13 wrapper and distribution checksums."""
import hashlib
from pathlib import Path

expected_jar = '81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f'
expected_distribution = '20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78'
assert hashlib.sha256(Path('gradle/wrapper/gradle-wrapper.jar').read_bytes()).hexdigest() == expected_jar
properties = Path('gradle/wrapper/gradle-wrapper.properties').read_text()
assert f'distributionSha256Sum={expected_distribution}' in properties
assert 'gradle-8.13-bin.zip' in properties
print('PASS official wrapper JAR and pinned distribution checksum')
