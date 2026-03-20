BEGIN
  BEGIN
    EXECUTE IMMEDIATE 'DROP INDEX idx_task_var_name_value_btree';
  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE != -1418 THEN RAISE; END IF;
  END;
  EXECUTE IMMEDIATE 'CREATE INDEX idx_task_var_name_value_btree ON task_variable (name, STANDARD_HASH(JSON_SERIALIZE(value), ''MD5''))';
END;
