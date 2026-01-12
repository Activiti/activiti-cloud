ALTER TABLE integration_context
  DROP CONSTRAINT integration_context_bpmn_activity_idx;

ALTER TABLE integration_context
  ADD CONSTRAINT integration_context_bpmn_activity_idx UNIQUE (id, process_instance_id, client_id, execution_id);
