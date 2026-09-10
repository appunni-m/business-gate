#!/usr/bin/env python3
"""Keep owned synthetic harness evidence and expose actionable CI failures without a login."""
import os
from pathlib import Path
import re
import sys

mode = sys.argv[1]
assert re.fullmatch(r'[a-z-]{1,64}', mode)
report = sys.stdin.read(64_001)
folder = os.environ.get('GATE_TEST_REPORTS', 'device-tests')
assert re.fullmatch(r'[a-z0-9-]{1,64}', folder)
directory = Path('output') / folder
directory.mkdir(parents=True, exist_ok=True)
(directory / f'{mode}.txt').write_text(report)
print(report, end='')
success = re.search(r'(?m)^(?:INSTRUMENTATION_RESULT: stream=)?PASS [1-9][0-9]* [^\n]+; mode='+re.escape(mode)+r'\s*$',report)
if os.environ.get('GITHUB_ACTIONS') == 'true':
    for metric in re.findall(r'(?m)^(?:INSTRUMENTATION_RESULT: stream=)?(METRIC(?: [a-z][a-z0-9_]{0,39}=[0-9]{1,20}){1,12})$', report)[:8]:
        print(f'::notice title=Android {mode} metrics::{metric}')
passed = len(report)<=64_000 and success is not None and 'FAIL' not in report
if not passed:
    failure = next((line for line in report.splitlines() if 'FAIL' in line), 'No passing instrumentation result')
    if os.environ.get('GITHUB_ACTIONS') == 'true':
        escaped = failure[:2000].replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
        print(f'::error title=Android {mode}::{escaped}')
    raise SystemExit(1)
if os.environ.get('GITHUB_ACTIONS') == 'true':
    print(f'::notice title=Android {mode}::{success.group(0).strip()}')
