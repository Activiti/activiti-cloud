DROP INDEX CONCURRENTLY IF EXISTS idx_task_status_unassigned;
CREATE INDEX CONCURRENTLY idx_task_status_unassigned ON task (status) INCLUDE (id)
  WHERE assignee IS NULL;
