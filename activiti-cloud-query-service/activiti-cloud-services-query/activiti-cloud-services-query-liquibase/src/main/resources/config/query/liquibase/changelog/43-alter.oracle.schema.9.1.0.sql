DROP INDEX idx_pvh_record_create_time;
DROP INDEX idx_pvh_process_var;
DROP TABLE process_variable_history;
DROP SEQUENCE process_variable_history_sequence;

ALTER TABLE integration_context
  ADD externalized_data_provider_type VARCHAR2(255);

ALTER TABLE integration_context
  ADD externalized_data_url VARCHAR2(255);
