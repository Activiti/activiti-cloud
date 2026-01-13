ALTER TABLE integration_context
  DROP CONSTRAINT integration_context_bpmn_activity_idx;

ALTER TABLE integration_context
  ADD CONSTRAINT fk_integration_context_bpmn_activity
  FOREIGN KEY (process_instance_id, client_id, execution_id)
  REFERENCES bpmn_activity (process_instance_id, element_id, execution_id)
  ON DELETE CASCADE;
