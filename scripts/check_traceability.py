#!/usr/bin/env python3
"""Check complete, document-qualified acceptance inventory and resolvable evidence references."""
import json
import re
from pathlib import Path

ledger = json.loads(Path('docs/acceptance-ledger.json').read_text())
assert ledger['schemaVersion'] == 1
expected = {f'full:T{i:03d}' for i in range(1, 97)} | {f'companion:T{i:03d}' for i in range(1, 98)}
rows = ledger['cases']
assert len(rows) == len(expected) and {row['id'] for row in rows} == expected
states = {'unrun', 'implemented', 'synthetic-only', 'emulator-verified', 'physically-qualified', 'failed'}
plan = Path('docs/implementation-plan.md').read_text()
for row in rows:
    source = Path(row['source'])
    assert source.is_file() and row['status'] in states and row['requirements'] and row['remaining']
    assert row['id'].split(':')[1] in source.read_text().splitlines()[row['sourceLine'] - 1]
    assert row['fixes'] and all(re.search(r'^### ' + fix + r' ', plan, re.M) for fix in row['fixes'])
    for test in row['tests']:
        path = Path(test['path'])
        assert path.is_file()
        assert not test['method'] or test['method'] + '(' in path.read_text()
    if row['status'] == 'physically-qualified':
        assert row.get('physicalEvidence') and Path(row['physicalEvidence']).is_file(), 'Physical claims require evidence'
print(f'PASS traceability: {len(rows)} distinct full/companion cases; all references resolve')
