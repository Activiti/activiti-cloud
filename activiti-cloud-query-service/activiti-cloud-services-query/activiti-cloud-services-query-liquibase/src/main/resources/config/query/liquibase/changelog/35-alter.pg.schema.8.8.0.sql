ALTER TABLE integration_context
  ADD COLUMN IF NOT EXISTS warning_date TIMESTAMP;

ALTER TABLE integration_context
  ADD COLUMN IF NOT EXISTS warning_code VARCHAR(255);

ALTER TABLE integration_context
  ADD COLUMN IF NOT EXISTS warning_message VARCHAR(255);

ALTER TABLE integration_context
  ADD COLUMN IF NOT EXISTS warning_class_name VARCHAR(255);
