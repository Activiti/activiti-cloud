ALTER TABLE bpmn_activity
  DROP CONSTRAINT bpmn_activity_processInstance_elementId_idx;

ALTER TABLE integration_context
  DROP CONSTRAINT integration_context_bpmn_activity_idx;

