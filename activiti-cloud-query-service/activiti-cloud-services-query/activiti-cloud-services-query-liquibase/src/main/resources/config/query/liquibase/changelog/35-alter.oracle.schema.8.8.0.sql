ALTER TABLE integration_context
  ADD warning_date TIMESTAMP;

ALTER TABLE integration_context
  ADD warning_code VARCHAR2(255);

ALTER TABLE integration_context
  ADD warning_message VARCHAR2(255);

ALTER TABLE integration_context
  ADD warning_class_name VARCHAR2(255);
