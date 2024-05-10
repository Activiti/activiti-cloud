CREATE INDEX CONCURRENTLY IF NOT EXISTS audit_event_process_instance_id_idx ON audit_event (process_instance_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS audit_event_app_name_idx ON audit_event (app_name);
CREATE INDEX CONCURRENTLY IF NOT EXISTS audit_event_event_type_idx ON audit_event (event_type);
