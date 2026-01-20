ALTER TABLE integration_context
  DROP CONSTRAINT integration_context_bpmn_activity_idx;

ALTER TABLE integration_context
  ADD CONSTRAINT fk_integration_context_bpmn_activity
  FOREIGN KEY (process_instance_id, client_id, execution_id)
  REFERENCES bpmn_activity (process_instance_id, element_id, execution_id)
  ON DELETE CASCADE;

CREATE INDEX integration_context_processInstance_idx on integration_context (process_instance_id);

ALTER TABLE bpmn_activity
  ADD COLUMN IF NOT EXISTS integration_context_counter INTEGER DEFAULT 0;

-- Update existing service tasks with the correct count
-- This is a one-time migration to populate the counter for existing data
UPDATE bpmn_activity ba
SET integration_context_counter = (
  SELECT COUNT(*)
  FROM integration_context ic
  WHERE ic.process_instance_id = ba.process_instance_id
    AND ic.client_id = ba.element_id
    AND ic.execution_id = ba.execution_id
)
WHERE ba.activity_type = 'serviceTask';
