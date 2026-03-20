DROP INDEX IF EXISTS idx_task_var_name_value_btree;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_task_var_name_value_btree
ON task_variable (name, md5(value::text));
