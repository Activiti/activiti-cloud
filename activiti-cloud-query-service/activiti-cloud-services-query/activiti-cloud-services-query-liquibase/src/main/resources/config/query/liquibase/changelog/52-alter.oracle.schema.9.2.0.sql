ALTER TABLE bpmn_activity
  ADD COLUMN IF NOT EXISTS service_task_type VARCHAR(255);
