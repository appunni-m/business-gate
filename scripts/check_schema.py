#!/usr/bin/env python3
"""Exercise the exact bundled schema, including constraints and crash recovery."""
import contextlib
import json
import sqlite3
import subprocess
import sys
import tempfile
from pathlib import Path

conn = sqlite3.connect(':memory:')
conn.executescript(Path('app/src/main/assets/schema.sql').read_text())
conn.execute("INSERT INTO namespace(id,installation) VALUES(1,'synthetic-installation')")
checks = 0

def reject(sql, args=()):
    global checks
    try:
        conn.execute(sql, args)
    except sqlite3.IntegrityError:
        checks += 1
    else:
        raise AssertionError(f'Constraint accepted invalid data: {sql}')

for phone in ['12025550101', '+01234567', '+123', '+1234567890123456', '+1202555abc1', '+12025550101\u202e']:
    reject('INSERT INTO account(namespace_id,phone,first_seen,last_seen) VALUES(1,?,0,0)', (phone,))
conn.execute("INSERT INTO account(id,namespace_id,phone,name,first_seen,last_seen) VALUES(1,1,'+12025550101','Harbor Clinic',0,0)")
reject("INSERT INTO account(namespace_id,phone,first_seen,last_seen) VALUES(1,'+12025550101',0,0)")
reject("UPDATE account SET choice='INFERRED_ALLOW' WHERE id=1")
reject("UPDATE account SET kind='VERIFIED_HUMAN' WHERE id=1")
reject("UPDATE account SET revision=-1 WHERE id=1")
reject("INSERT INTO account(namespace_id,phone,first_seen,last_seen) VALUES(2,'+12025550102',0,0)")
reject("INSERT INTO action_job(account_id,action,state,global_revision,account_revision,created_at,updated_at) VALUES(1,'UNBLOCK','PENDING',0,0,0,0)")
conn.execute("INSERT INTO action_job(account_id,action,state,global_revision,account_revision,created_at,updated_at) VALUES(1,'BLOCK','ACTION_INTENT',0,0,0,0)")
conn.execute("UPDATE action_job SET state='REINSPECT',reason='PROCESS_RESTART' WHERE state IN ('ACTION_INTENT','VERIFYING')")
assert conn.execute('SELECT state FROM action_job').fetchone()[0] == 'REINSPECT'
checks += 1
reject("UPDATE action_job SET nonce='wrong-action-grant'")
reject("INSERT INTO action_event(account_id,type,reason,at_ms) VALUES(1,'OBSERVED','raw screen body',0)")
reject("INSERT INTO attention_daily VALUES('2026-09-09',1,1,3)")
conn.execute("INSERT INTO attention_daily VALUES('2026-09-09',30,30,40)")
conn.execute("DELETE FROM account WHERE id=1")
assert conn.execute('SELECT count(*) FROM action_job').fetchone()[0] == 0
checks += 1
assert conn.execute('PRAGMA foreign_key_check').fetchall() == []
assert conn.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
checks += 2
legacy=sqlite3.connect(':memory:')
legacy.executescript(Path('app/src/androidTest/assets/schema-v1.sql').read_text())
legacy.execute("INSERT INTO namespace(id,installation,enabled,paused) VALUES(1,'actual-v1-fixture',1,0)")
legacy.execute("INSERT INTO account(namespace_id,phone,choice,first_seen,last_seen) VALUES(1,'+12025550101','ALLOW',0,0)")
legacy.executescript(Path('app/src/main/assets/migrations/1-2.sql').read_text())
assert legacy.execute('SELECT choice FROM account').fetchone()[0]=='ALLOW'
assert legacy.execute('SELECT active,enabled,paused,receiver_binding FROM namespace').fetchone()==(1,0,1,'')
checks+=2

migration = [sql.strip() for sql in Path('app/src/main/assets/migrations/1-2.sql').read_text().split(';') if sql.strip()]
v1 = Path('app/src/androidTest/assets/schema-v1.sql').read_text()
def old_database(path):
    db=sqlite3.connect(path)
    db.executescript(v1)
    db.execute("INSERT INTO namespace(id,installation,enabled,paused) VALUES(1,'interruption-fixture',1,0)")
    db.execute("INSERT INTO account(namespace_id,phone,choice,first_seen,last_seen) VALUES(1,'+12025550101','ALLOW',0,0)")
    db.commit()
    return db

def preserved(db):
    assert db.execute('PRAGMA user_version').fetchone()[0]==1
    assert 'active' not in {row[1] for row in db.execute('PRAGMA table_info(namespace)')}
    assert db.execute('SELECT choice FROM account').fetchone()[0]=='ALLOW'
    assert db.execute('PRAGMA integrity_check').fetchone()[0]=='ok'

# Actual worker death at each migration statement, followed by reopening the original file.
with tempfile.TemporaryDirectory(prefix='business-gate-migration-') as directory:
    for cut in range(1,len(migration)+1):
        path=Path(directory)/f'legacy-{cut}.db'
        old_database(path).close()
        worker='''import json,os,sqlite3,sys
db=sqlite3.connect(sys.argv[1]);db.execute('BEGIN IMMEDIATE')
for statement in json.loads(sys.stdin.read()): db.execute(statement)
os._exit(73)
'''
        result=subprocess.run([sys.executable,'-c',worker,str(path)],input=json.dumps(migration[:cut]),text=True)
        assert result.returncode==73
        with contextlib.closing(sqlite3.connect(path)) as reopened: preserved(reopened)
        checks+=1

# A real SQLite page quota raises SQLITE_FULL without consuming the host's remaining disk.
limited=old_database(':memory:')
pages=limited.execute('PRAGMA page_count').fetchone()[0]
limited.execute(f'PRAGMA max_page_count={pages}')
try:
    limited.execute('BEGIN IMMEDIATE')
    for statement in migration: limited.execute(statement)
except sqlite3.OperationalError as error:
    assert 'full' in str(error).lower()
    limited.rollback()
else:
    raise AssertionError('Expected the migration to exceed its page quota')
preserved(limited)
checks+=1
limited.execute('PRAGMA max_page_count=1000000')
limited.execute('BEGIN IMMEDIATE')
for statement in migration: limited.execute(statement)
limited.execute('PRAGMA user_version=2');limited.commit()
assert limited.execute('SELECT choice FROM account').fetchone()[0]=='ALLOW'
assert limited.execute('SELECT paused,enabled FROM namespace').fetchone()==(1,0)
checks+=1
# Actual shipped v2 schema: no optional label becomes a name permission.
v2 = Path('app/src/androidTest/assets/schema-v2.sql').read_text()
name_migration = [sql.strip() for sql in Path('app/src/main/assets/migrations/2-3.sql').read_text().split(';') if sql.strip()]
def version_two(path):
    db=sqlite3.connect(path)
    db.executescript(v2)
    db.execute("INSERT INTO namespace(id,installation,active,enabled,paused) VALUES(1,'v2-fixture',1,1,0)")
    for phone, choice in (('+12025550101','ALLOW'),('+12025550102','DENY_MANUAL')):
        db.execute("INSERT INTO account(namespace_id,phone,name,choice,first_seen,last_seen) VALUES(1,?,'Harbor Clinic',?,0,0)",(phone,choice))
    db.commit()
    return db
with tempfile.TemporaryDirectory(prefix='business-gate-name-migration-') as directory:
    for cut in range(1,len(name_migration)+1):
        path=Path(directory)/f'v2-{cut}.db'
        version_two(path).close()
        result=subprocess.run([sys.executable,'-c',worker,str(path)],input=json.dumps(name_migration[:cut]),text=True)
        assert result.returncode==73
        with contextlib.closing(sqlite3.connect(path)) as reopened:
            assert reopened.execute('PRAGMA user_version').fetchone()[0]==2
            assert reopened.execute("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='business_name_choice'").fetchone()[0]==0
            assert reopened.execute('SELECT choice FROM account ORDER BY phone').fetchall()==[('ALLOW',),('DENY_MANUAL',)]
        checks+=1
migrated=version_two(':memory:')
migrated.execute('BEGIN IMMEDIATE')
for statement in name_migration:migrated.execute(statement)
migrated.execute('PRAGMA user_version=3');migrated.commit()
assert migrated.execute('SELECT count(*) FROM business_name_choice').fetchone()[0]==0
assert migrated.execute('SELECT choice FROM account ORDER BY phone').fetchall()==[('ALLOW',),('DENY_MANUAL',)]
assert migrated.execute('SELECT enabled,paused FROM namespace').fetchone()==(1,1)
checks+=3
for name, enabled, revision in [('',1,1),('x'*121,1,1),('Harbor Clinic',2,1),('Harbor Clinic',1,0)]:
    reject('INSERT INTO business_name_choice VALUES(1,?,?,?)',(name,enabled,revision))
reject("INSERT INTO business_name_choice VALUES(999,'Harbor Clinic',1,1)")
conn.execute("INSERT INTO business_name_choice VALUES(1,'Harbor Clinic',1,1)")
reject("INSERT INTO business_name_choice VALUES(1,'Harbor Clinic',0,2)")
conn.execute('DELETE FROM namespace WHERE id=1')
assert conn.execute('SELECT count(*) FROM business_name_choice').fetchone()[0]==0
checks+=1
# Shipped v3 choices survive an interrupted observed-name upgrade; aliases remain unverified.
v3 = Path('app/src/androidTest/assets/schema-v3.sql').read_text()
observed_migration = [sql.strip() for sql in Path('app/src/main/assets/migrations/3-4.sql').read_text().split(';') if sql.strip()]
with tempfile.TemporaryDirectory(prefix='business-gate-observed-name-') as directory:
    for cut in range(1,len(observed_migration)+1):
        path=Path(directory)/f'v3-{cut}.db'
        db=sqlite3.connect(path);db.executescript(v3)
        db.execute("INSERT INTO namespace(id,installation,active,enabled,paused) VALUES(1,'v3-fixture',1,1,0)")
        db.execute("INSERT INTO account(namespace_id,phone,name,choice,first_seen,last_seen) VALUES(1,'+12025550101','Harbor Clinic','ALLOW',0,0)")
        db.execute("INSERT INTO business_name_choice VALUES(1,'Harbor Clinic',1,2)");db.commit();db.close()
        result=subprocess.run([sys.executable,'-c',worker,str(path)],input=json.dumps(observed_migration[:cut]),text=True)
        assert result.returncode==73
        with contextlib.closing(sqlite3.connect(path)) as reopened:
            assert reopened.execute('PRAGMA user_version').fetchone()[0]==3
            assert 'business_name' not in [r[1] for r in reopened.execute('PRAGMA table_info(account)')]
            assert reopened.execute('SELECT choice FROM account').fetchone()==('ALLOW',)
            reopened.execute('BEGIN IMMEDIATE')
            for statement in observed_migration: reopened.execute(statement)
            reopened.execute('PRAGMA user_version=4');reopened.commit()
            assert reopened.execute('SELECT name,business_name,choice FROM account').fetchone()==('Harbor Clinic','','ALLOW')
            assert reopened.execute('SELECT enabled,revision FROM business_name_choice').fetchone()==(1,2)
            assert reopened.execute('SELECT paused FROM namespace').fetchone()==(1,)
        checks+=6
print(f'PASS {checks} schema, migration interruption, quota failure and recovery checks')
