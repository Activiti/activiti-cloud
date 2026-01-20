ALTER TABLE integration_context
  DROP CONSTRAINT integration_context_bpmn_activity_idx;

-- Purge orphaned integration_context records that don't have a matching bpmn_activity entity
DELETE FROM integration_context ic
WHERE NOT EXISTS (
  SELECT 1 FROM bpmn_activity ba
  WHERE ba.process_instance_id = ic.process_instance_id
    AND ba.element_id = ic.client_id
    AND ba.execution_id = ic.execution_id
);

ALTER TABLE integration_context
  ADD CONSTRAINT fk_integration_context_bpmn_activity
  FOREIGN KEY (process_instance_id, client_id, execution_id)
  REFERENCES bpmn_activity (process_instance_id, element_id, execution_id)
  ON DELETE CASCADE;
