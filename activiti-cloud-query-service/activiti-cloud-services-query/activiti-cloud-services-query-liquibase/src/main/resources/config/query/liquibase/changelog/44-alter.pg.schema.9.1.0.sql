ALTER TABLE integration_context
  ADD COLUMN IF NOT EXISTS externalized_data_provider_type varchar(255);

ALTER TABLE integration_context
  ADD COLUMN IF NOT EXISTS externalized_data_url varchar(255);
