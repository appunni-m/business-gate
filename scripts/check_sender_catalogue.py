#!/usr/bin/env python3
"""Validate offline name provenance without network lookups or brand aliases."""
import datetime
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
