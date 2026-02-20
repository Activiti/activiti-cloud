CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_event_entity_ts_desc ON audit_event (entity_id, timestamp DESC);
