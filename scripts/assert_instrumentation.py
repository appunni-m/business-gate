#!/usr/bin/env python3
"""Keep owned synthetic harness evidence and expose actionable CI failures without a login."""
import os
from pathlib import Path
import re
import sys

mode = sys.argv[1]
assert re.fullmatch(r'[a-z-]{1,64}', mode)
report = sys.stdin.read(64_001)
directory = Path('output/device-tests')
directory.mkdir(parents=True, exist_ok=True)
(directory / f'{mode}.txt').write_text(report)
print(report, end='')
passed = len(report)<=64_000 and re.search(r'(?m)^(?:INSTRUMENTATION_RESULT: stream=)?PASS [0-9]+ [^\n]+; mode='+re.escape(mode)+r'\s*$',report) is not None and 'FAIL' not in report
if not passed:
    failure = next((line for line in report.splitlines() if 'FAIL' in line), 'No passing instrumentation result')
    if os.environ.get('GITHUB_ACTIONS') == 'true':
        escaped = failure[:2000].replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
        print(f'::error title=Android {mode}::{escaped}')
    raise SystemExit(1)
