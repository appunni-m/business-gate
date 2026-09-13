#!/usr/bin/env python3
"""Source gate; the artifact audit repeats it against bytes inside the distributable."""
from pathlib import Path
from compatibility import validate
import json, hashlib

assets = Path('app/src/main/assets')
result = validate(lambda name: (assets / name).read_bytes())
print(f"PASS compatibility: {len(result['qualifiedAdapters'])} qualified adapters; live capability={result['connectedAppActionsEnabled']}")

registry=json.loads((assets/'adapters/compatibility.json').read_text())
for row in registry.get('measuredRoutes', []):
    for field, filename in [('businessRouteSha256','BusinessProfileRoute.java'),('receiverRouteSha256','ReceivingAccountRoute.java'),('actionEchoSha256','ActionEcho.java')]:
        source=Path('connected-route/src/main/java/io/github/appunnim/businessgate/connected')/filename
        assert hashlib.sha256(source.read_bytes()).hexdigest()==row[field], 'Measured route changed: rerun live qualification'
