/* Using "JSONB" data type for value column in PROCESS_VARIABLE and TASK_VARIABLE tables,
the automatic creation of tables is not working anymore with H2 database which only supports "JSON".
So, these two tables have to be created manually */
CREATE TABLE IF NOT EXISTS PROCESS_VARIABLE (
  marked_as_deleted BOOLEAN,
  create_time TIMESTAMP(6),
  id BIGINT NOT NULL,
  last_updated_time TIMESTAMP(6),
  app_name VARCHAR(255),
  app_version VARCHAR(255),
  execution_id VARCHAR(255),
  name VARCHAR(255),
  process_definition_key VARCHAR(255),
  process_instance_id VARCHAR(255),
  service_full_name VARCHAR(255),
  service_name VARCHAR(255),
  service_type VARCHAR(255),
  service_version VARCHAR(255),
  type VARCHAR(255),
  variable_definition_id VARCHAR(255),
  "value" JSON,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS TASK_VARIABLE (
  marked_as_deleted BOOLEAN,
  create_time TIMESTAMP(6),
  id BIGINT NOT NULL,
  last_updated_time TIMESTAMP(6),
  app_name VARCHAR(255),
  app_version VARCHAR(255),
  execution_id VARCHAR(255),
  name VARCHAR(255),
  process_instance_id VARCHAR(255),
  service_full_name VARCHAR(255),
  service_name VARCHAR(255),
  service_type VARCHAR(255),
  service_version VARCHAR(255),
  task_id VARCHAR(255),
  type VARCHAR(255),
  "value" JSON,
  PRIMARY KEY (id)
);

INSERT INTO process_instance (id, last_modified, last_modified_from, last_modified_to, process_definition_id, status, initiator, start_date, business_key, process_definition_key) VALUES
  ('0', CURRENT_TIMESTAMP, null, null, 'process_definition_id', 'RUNNING', 'initiator',null, 'bus_key','def_key'),
  ('1', CURRENT_TIMESTAMP, null, null, 'process_definition_id', 'RUNNING', 'initiator',null, 'bus_key','def_key');

INSERT INTO task (id, assignee, business_key, created_date, description, due_date, last_modified, last_modified_from, last_modified_to, name, priority, process_definition_id, process_instance_id, status, owner, claimed_date) VALUES
  ('1', 'assignee', 'bk1', CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task1', 5, 'process_definition_id', 0, 'COMPLETED'  , 'owner', null),
  ('2', 'assignee', null, CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task2', 10, 'process_definition_id', 0, 'CREATED'  , 'owner', null),
  ('3', 'assignee', null, CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task3', 5, 'process_definition_id', 0, 'CREATED'  , 'owner', null),
  ('4', 'assignee', null, CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task4', 10, 'process_definition_id', 1, 'CREATED'  , 'owner', null),
  ('5', 'assignee', null, CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task5', 10, 'process_definition_id', 1, 'COMPLETED'  , 'owner', null),
  ('6', 'assignee', 'bk6', CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task6', 10, 'process_definition_id', 0, 'ASSIGNED'  , 'owner', null);

INSERT INTO PROCESS_VARIABLE (id, create_time, execution_id, last_updated_time, name, process_instance_id, type, "value") VALUES
  (1, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'initiator', 1, 'map', JSON '{"value": { "key" : ["1","2","3","4","5"]}}');

INSERT INTO TASK_VARIABLE (id, create_time, execution_id, last_updated_time, name, process_instance_id, task_id, type, "value") VALUES
  (2, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable1', 0, '1', 'String', JSON '{"value": "10"}'),
  (3, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable2', 0, '1', 'String', JSON '{"value": true}'),
  (4, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable3', 0, '2', 'String', JSON '{"value": null}'),
  (5, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable4', 0, '2', 'map', JSON '{"value": { "key" : "data" }}'),
  (6, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable5', 1, '4', 'String', JSON '{"value": 1.0}'),
  (7, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable6', 1, '4', 'list', JSON '{"value": [1,2,3,4,5]}');

INSERT INTO TASK_PROCESS_VARIABLE (task_id, process_variable_id) VALUES
  (4,1),
  (5,1);
