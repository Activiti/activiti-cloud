ALTER TABLE task_variable
  ADD COLUMN fts tsvector
    GENERATED ALWAYS AS
      (
      jsonb_to_tsvector('simple', value, '["string", "numeric"]')
      ) STORED

CREATE INDEX table_col_fts_idx ON task_variable USING gin (fts)
