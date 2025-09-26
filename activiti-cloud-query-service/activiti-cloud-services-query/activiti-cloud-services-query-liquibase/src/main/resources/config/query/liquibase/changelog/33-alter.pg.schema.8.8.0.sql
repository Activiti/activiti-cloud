ALTER TABLE task_variable
  ADD COLUMN fts tsvector
    GENERATED ALWAYS AS
      (
      jsonb_to_tsvector('simple', value, '["string", "numeric"]')
      ) STORED
