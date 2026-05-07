CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_process_instance_status_pdk_lastmodified
ON process_instance (process_definition_key, status, last_modified, id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_task_status_pdk_lastmodified
ON task (process_definition_key, status, last_modified, id);
