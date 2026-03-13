CREATE INDEX idx_task_var_name_value_btree
ON task_variable (name, JSON_SERIALIZE(value RETURNING VARCHAR2(4000)));
