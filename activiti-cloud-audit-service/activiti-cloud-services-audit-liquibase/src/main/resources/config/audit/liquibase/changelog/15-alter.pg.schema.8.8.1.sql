CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_event_timestamp ON audit_event(timestamp DESC);
