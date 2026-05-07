DROP INDEX CONCURRENTLY IF EXISTS idx_audit_event_timestamp;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_event_timestamp_desc_nulls_last ON audit_event (timestamp DESC NULLS LAST);
