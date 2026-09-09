#!/usr/bin/env python3
"""Exercise the exact bundled schema, including constraints and crash recovery."""
import sqlite3
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
print(f'PASS {checks} schema constraints and recovery assertions')
