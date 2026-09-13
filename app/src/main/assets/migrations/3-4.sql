ALTER TABLE account ADD COLUMN business_name TEXT NOT NULL DEFAULT '' CHECK(length(business_name)<=120);
CREATE INDEX account_business_name ON account(namespace_id,business_name,id);
UPDATE namespace SET paused=1,global_revision=global_revision+1;
