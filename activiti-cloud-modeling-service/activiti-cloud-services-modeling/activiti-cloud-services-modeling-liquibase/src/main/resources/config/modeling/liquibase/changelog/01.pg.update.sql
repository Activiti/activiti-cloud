ALTER TABLE model_version
ALTER COLUMN content TYPE oid USING content::oid;