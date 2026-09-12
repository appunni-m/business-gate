"""Validate bundled compatibility and evidence before a build can claim live capability."""
import hashlib
import json
import re
from datetime import date


def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':')).encode()


def digest(value):
    if not isinstance(value, str) or not re.fullmatch(r'[a-f0-9]{64}', value):
        raise ValueError('Invalid digest')


def resource(value, ancestor=False):
    namespace = value.get('resourceNamespace', 'selected')
    assert namespace in ('selected', 'android', 'none')
    if namespace == 'none':
        assert ancestor and value['resourceSuffix'] == ''
    else:
        assert re.fullmatch(r'[a-zA-Z0-9_]{1,160}', value['resourceSuffix'])


def path(value):
    children = value['children']
    assert len(children) <= 12 and all(type(i) is int and 0 <= i <= 63 for i in children)
    resource(value)
    assert re.fullmatch(r'[a-zA-Z0-9_.$]{1,160}', value['className'])
    assert len(value.get('expectedText', '')) <= 160
    assert len(value['ancestors']) == len(children)
    for index, ancestor in zip(children, value['ancestors']):
        resource(ancestor, ancestor=True)
        assert re.fullmatch(r'[a-zA-Z0-9_.$]{1,160}', ancestor['className'])
        assert type(ancestor['childCount']) is int and index < ancestor['childCount'] <= 64


def validate(read_asset):
    def read(name, limit):
        content = read_asset(name)
        assert len(content) <= limit, 'Unbounded compatibility asset'
        return content

    registry = json.loads(read('adapters/compatibility.json', 512_000))
    assert registry['schemaVersion'] == 1
    rows = registry['adapters']
    assert isinstance(rows, list) and len(rows) <= 32
    ids, environments = set(), set()
    for row in rows:
        identity = row['id']
        assert re.fullmatch(r'[a-z0-9][a-z0-9-]{0,63}', identity) and identity not in ids
        ids.add(identity)
        digest(row['packageSha256'])
        digest(row['evidenceSha256'])
        assert type(row['versionCode']) is int and row['versionCode'] > 0
        assert type(row['api']) is int and row['api'] >= 29 and row['locale'] == 'en'
        assert all(row[key] is True for key in ('physicallyQualified', 'interruptionSafe', 'readStatePreserving'))
        assert row['reviewer'].strip()
        for field in ('signingSha256', 'signingHistorySha256'):
            assert 1 <= len(row[field]) <= 8 and len(set(row[field])) == len(row[field])
            for value in row[field]:
                digest(value)
        env = row['environment']
        assert all(1 <= len(env[key]) <= 160 for key in ('manufacturer', 'model', 'deviceLocale'))
        assert 50 <= env['fontScalePercent'] <= 300 and 72 <= env['densityDpi'] <= 1000
        assert env['orientation'] in (1, 2)
        key = (row['packageSha256'], row['versionCode'], row['api'], canonical(env))
        assert key not in environments
        environments.add(key)
        assert len(row['screens']) == 3
        assert {screen['role'] for screen in row['screens']} == {'PROFILE', 'BLOCK_DIALOG', 'UNBLOCK_DIALOG'}
        for screen in row['screens']:
            for field in ('signature', 'phone', 'receiver', 'business', 'regular', 'blocked', 'unblocked', 'blockControl', 'unblockControl'):
                path(screen[field])
            assert 1 <= len(screen['forbiddenControls']) <= 8
            for excluded in screen['forbiddenControls']:
                path(excluded)
        evidence_bytes = read(f'adapters/evidence/{identity}.json', 128_000)
        assert hashlib.sha256(evidence_bytes).hexdigest() == row['evidenceSha256']
        evidence = json.loads(evidence_bytes)
        contract = {key: value for key, value in row.items() if key != 'evidenceSha256'}
        assert evidence['schemaVersion'] == 1
        assert evidence['contractSha256'] == hashlib.sha256(canonical(contract)).hexdigest()
        assert evidence['reviewer'] == row['reviewer']
        assert all(evidence[key] is True for key in ('physicalDevice', 'readReceiptsPreserved', 'noUnauthorizedSideEffects'))
        assert date.fromisoformat(evidence['capturedOn']) <= date.today()
        assert all(evidence['cases'].get(f'full:T{i:03d}') == 'PASS' for i in range(1, 97))
        assert 1 <= len(evidence['artifacts']) <= 16
        for artifact in evidence['artifacts']:
            assert re.fullmatch(r'adapters/evidence/[a-z0-9-]+\.(json|txt)', artifact['path'])
            assert hashlib.sha256(read(artifact['path'], 256_000)).hexdigest() == artifact['sha256']
    return {'connectedAppActionsEnabled': bool(rows), 'qualifiedAdapters': sorted(ids),
            'compatibilitySha256': hashlib.sha256(canonical(registry)).hexdigest()}
