CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_proc_var_defKey_name
  ON process_variable (process_definition_key, name);
