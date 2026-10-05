DROP INDEX CONCURRENTLY IF EXISTS pi_root_unlinked_startdate_idx;
CREATE INDEX CONCURRENTLY pi_root_unlinked_startdate_idx ON process_instance (start_date DESC NULLS LAST)
  WHERE parent_id IS NULL AND (linked_process_instance_id IS NULL OR linked_process_instance_type IS NULL);
