CREATE TABLE business_name_choice (
 namespace_id INTEGER NOT NULL REFERENCES namespace(id) ON DELETE CASCADE,
 name_key TEXT NOT NULL CHECK(length(name_key) BETWEEN 1 AND 120),
 enabled INTEGER NOT NULL CHECK(enabled IN (0,1)),
 revision INTEGER NOT NULL CHECK(revision>=1),
 PRIMARY KEY(namespace_id,name_key)
);
UPDATE namespace SET paused=1,global_revision=global_revision+1;
