#!/usr/bin/env python3
"""Source gate; the artifact audit repeats it against bytes inside the distributable."""
from pathlib import Path
from compatibility import validate

assets = Path('app/src/main/assets')
result = validate(lambda name: (assets / name).read_bytes())
print(f"PASS compatibility: {len(result['qualifiedAdapters'])} qualified adapters; live capability={result['connectedAppActionsEnabled']}")
