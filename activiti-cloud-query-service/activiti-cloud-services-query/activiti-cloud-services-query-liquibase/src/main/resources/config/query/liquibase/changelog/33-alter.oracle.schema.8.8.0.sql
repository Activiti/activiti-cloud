ALTER TABLE bpmn_activity
  DROP CONSTRAINT bpmn_activity_processInstance_elementId_idx;

ALTER TABLE bpmn_activity
  ADD CONSTRAINT bpmn_activity_processInstance_elementId_idx UNIQUE (id, process_instance_id, element_id, execution_id);

ALTER TABLE integration_context
  DROP CONSTRAINT integration_context_bpmn_activity_idx;

ALTER TABLE integration_context
  ADD CONSTRAINT integration_context_bpmn_activity_idx UNIQUE (id, process_instance_id, client_id, execution_id);
