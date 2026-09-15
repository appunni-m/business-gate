#!/usr/bin/env python3
"""Validate offline name provenance without network lookups or brand aliases."""
import datetime
import base64
import json
import re
import unicodedata
from pathlib import Path
from urllib.parse import urlparse
path=Path('app/src/main/assets/indian-sender-names.json')
data=json.loads(path.read_text())
assert set(data)=={'schema','checkedOn','coverage','entries'}
assert data['schema']==1 and datetime.date.fromisoformat(data['checkedOn'])<=datetime.date.today()
assert 1<=len(data['entries'])<=500
names=set()
for row in data['entries']:
    assert set(row)=={'name','country','profileSource','contactSource','evidence'}
    name=row['name']
    assert name==unicodedata.normalize('NFC',name).strip() and 1<=len(name)<=120 and name not in names
    assert all(unicodedata.category(c) not in {'Cc','Cf','Cs'} for c in name)
    assert row['country']=='IN' and row['evidence']=='public-profile'
    assert re.fullmatch(r'https://wa[.]me/91[0-9]{10}',row['profileSource'])
    url=urlparse(row['contactSource']);assert url.scheme=='https' and url.hostname and not url.username and not url.password
    names.add(name)
print(f'PASS {len(names)} exact starter names with public-profile and business-contact provenance')

supplied_path=Path('app/src/main/assets/supplied-sender-rules.json')
assert supplied_path.stat().st_size<=128_000
supplied=json.loads(supplied_path.read_text())
assert set(supplied)=={'schemaVersion','source','addedOn','encoding','payload'}
assert supplied['schemaVersion']==1 and supplied['source']=='owner-provided'
assert datetime.date.fromisoformat(supplied['addedOn'])<=datetime.date.today()
assert supplied['encoding']=='base64-utf8-json'
decoded=base64.b64decode(supplied['payload'],validate=True)
assert base64.b64encode(decoded).decode('ascii')==supplied['payload']
rows=json.loads(decoded.decode('utf-8'))
assert isinstance(rows,list) and 1<=len(rows)<=500
assert all(isinstance(name,str) for name in rows) and rows==sorted(set(rows))
for name in rows:
    assert name==unicodedata.normalize('NFC',name).strip() and 1<=len(name)<=120 and name not in names
    assert all(unicodedata.category(c) not in {'Cc','Cf','Cs'} for c in name)
print(f'PASS {len(rows)} unique Base64-encoded supplied rules; no business identity asserted')
