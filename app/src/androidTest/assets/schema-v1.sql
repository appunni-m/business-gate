PRAGMA foreign_keys = ON;
CREATE TABLE namespace (
 id INTEGER PRIMARY KEY, installation TEXT NOT NULL, receiver_binding TEXT NOT NULL DEFAULT '',
 qualification_id TEXT NOT NULL DEFAULT '', global_revision INTEGER NOT NULL DEFAULT 0 CHECK(global_revision>=0),
 enabled INTEGER NOT NULL DEFAULT 0 CHECK(enabled IN (0,1)), paused INTEGER NOT NULL DEFAULT 1 CHECK(paused IN (0,1)),
 consent_version TEXT NOT NULL DEFAULT '', consent_at INTEGER NOT NULL DEFAULT 0,
 setup TEXT NOT NULL DEFAULT 'WELCOME', discovery INTEGER NOT NULL DEFAULT 0 CHECK(discovery IN (0,1)),
 sales_hints INTEGER NOT NULL DEFAULT 0 CHECK(sales_hints IN (0,1)), digest INTEGER NOT NULL DEFAULT 0 CHECK(digest IN (0,1))
);
CREATE TABLE account (
 id INTEGER PRIMARY KEY, namespace_id INTEGER NOT NULL REFERENCES namespace(id),
 phone TEXT NOT NULL CHECK(length(phone) BETWEEN 8 AND 16 AND substr(phone,1,1)='+' AND substr(phone,2,1) BETWEEN '1' AND '9' AND substr(phone,2) NOT GLOB '*[^0-9]*'),
 name TEXT NOT NULL DEFAULT '', search_key TEXT NOT NULL DEFAULT '',
 kind TEXT NOT NULL DEFAULT 'UNKNOWN' CHECK(kind IN ('UNKNOWN','BUSINESS_CONFIRMED','REGULAR_PROFILE_OBSERVED','AMBIGUOUS','NON_DIRECT')),
 choice TEXT NOT NULL DEFAULT 'DEFAULT' CHECK(choice IN ('DEFAULT','ALLOW','DENY_MANUAL')),
 observed_state TEXT NOT NULL DEFAULT 'UNKNOWN' CHECK(observed_state IN ('UNKNOWN','BLOCKED','UNBLOCKED')),
 gate_owned INTEGER NOT NULL DEFAULT 0 CHECK(gate_owned IN (0,1)), revision INTEGER NOT NULL DEFAULT 0 CHECK(revision>=0),
 ever_business INTEGER NOT NULL DEFAULT 0 CHECK(ever_business IN (0,1)),
 review TEXT NOT NULL DEFAULT 'NONE' CHECK(review IN ('NONE','NEW_SENDER','POSSIBLE_COMMERCIAL','TYPE_CHANGED')),
 hint_bits INTEGER NOT NULL DEFAULT 0 CHECK(hint_bits BETWEEN 0 AND 255),
 dismissed_until INTEGER NOT NULL DEFAULT 0, checked_at INTEGER NOT NULL DEFAULT 0,
 first_seen INTEGER NOT NULL, last_seen INTEGER NOT NULL,
 UNIQUE(namespace_id,phone)
);
CREATE INDEX account_search ON account(namespace_id,search_key,id);
CREATE INDEX account_retention ON account(last_seen,choice,ever_business);
CREATE TABLE action_job (
 account_id INTEGER PRIMARY KEY REFERENCES account(id) ON DELETE CASCADE,
 action TEXT NOT NULL CHECK(action IN ('BLOCK','UNBLOCK')),
 state TEXT NOT NULL CHECK(state IN ('PENDING','WAITING','ACTION_INTENT','VERIFYING','REINSPECT','DONE','CANCELED','FAILED')),
 global_revision INTEGER NOT NULL, account_revision INTEGER NOT NULL,
 generation INTEGER NOT NULL DEFAULT 0, nonce TEXT, grant_created_at INTEGER NOT NULL DEFAULT 0,
 attempts INTEGER NOT NULL DEFAULT 0 CHECK(attempts>=0), reason TEXT NOT NULL DEFAULT '',
 created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
 CHECK(action='UNBLOCK' OR nonce IS NULL),
 CHECK(action<>'UNBLOCK' OR state IN ('DONE','CANCELED','FAILED') OR nonce IS NOT NULL)
);
CREATE TABLE action_event (
 id INTEGER PRIMARY KEY, account_id INTEGER REFERENCES account(id) ON DELETE SET NULL,
 type TEXT NOT NULL CHECK(type IN ('POLICY','OBSERVED','INTENT','VERIFIED','STOPPED','RESET','RECOVERY')),
 reason TEXT NOT NULL CHECK(reason IN ('USER_CHOICE','LIVE_EVIDENCE','ACTION_INTENT','VERIFIED','USER_STOP','PROCESS_RESTART','INSTALLATION_CHANGED','CLEAR_LOCAL','HINT_DISMISSED')),
 at_ms INTEGER NOT NULL
);
CREATE INDEX event_retention ON action_event(at_ms,id);
CREATE TABLE app_meta (key TEXT PRIMARY KEY, value TEXT NOT NULL);
CREATE TABLE attention_daily (
 day_utc TEXT PRIMARY KEY, management_ms INTEGER NOT NULL DEFAULT 0 CHECK(management_ms>=0),
 occupancy_ms INTEGER NOT NULL DEFAULT 0 CHECK(occupancy_ms>=0), union_ms INTEGER NOT NULL DEFAULT 0 CHECK(union_ms>=0),
 CHECK(union_ms<=management_ms+occupancy_ms)
);
PRAGMA user_version=1;
