ALTER TABLE namespace ADD COLUMN package_digest TEXT NOT NULL DEFAULT '';
ALTER TABLE namespace ADD COLUMN profile_key TEXT NOT NULL DEFAULT '';
ALTER TABLE namespace ADD COLUMN active INTEGER NOT NULL DEFAULT 0 CHECK(active IN (0,1));
CREATE UNIQUE INDEX one_active_namespace ON namespace(active) WHERE active=1;
CREATE UNIQUE INDEX receiver_namespace ON namespace(installation,package_digest,profile_key,receiver_binding) WHERE receiver_binding<>'';
UPDATE namespace SET active=CASE WHEN id=1 THEN 1 ELSE 0 END,enabled=0,paused=1,receiver_binding='',qualification_id='',global_revision=global_revision+1;
UPDATE action_job SET state=CASE WHEN state IN ('ACTION_INTENT','VERIFYING') THEN 'REINSPECT' ELSE 'CANCELED' END,nonce=NULL WHERE action='BLOCK';
UPDATE action_job SET state='CANCELED',nonce=NULL WHERE action='UNBLOCK';
