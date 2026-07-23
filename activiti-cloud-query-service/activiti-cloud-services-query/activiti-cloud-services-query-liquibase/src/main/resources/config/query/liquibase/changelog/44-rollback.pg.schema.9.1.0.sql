ALTER TABLE integration_context
  DROP COLUMN IF EXISTS externalized_data_provider_type;

ALTER TABLE integration_context
  DROP COLUMN IF EXISTS externalized_data_url;
