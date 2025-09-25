
CREATE INDEX task_variable_value_trgm_idx ON task_variable USING GIN ((value -> 'value') gin_trgm_ops) WHERE type = 'string';
