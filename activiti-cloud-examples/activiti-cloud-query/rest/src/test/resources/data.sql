INSERT INTO process_instance (id, last_modified, last_modified_from, last_modified_to, process_definition_id, status, initiator, start_date, business_key, process_definition_key) VALUES
  ('0', CURRENT_TIMESTAMP, null, null, 'process_definition_id', 'RUNNING', 'initiator',null, 'bus_key','def_key'),
  ('1', CURRENT_TIMESTAMP, null, null, 'process_definition_id', 'RUNNING', 'initiator',null, 'bus_key','def_key');

INSERT INTO task (id, assignee, business_key, created_date, description, due_date, last_modified, last_modified_from, last_modified_to, name, priority, process_definition_id, process_instance_id, status, owner, claimed_date) VALUES
  ('1', 'testuser', 'bk1', CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task1', 5, 'process_definition_id', 0, 'COMPLETED'  , 'owner', null),
  ('2', 'hruser', null, CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task2', 10, 'process_definition_id', 1, 'CREATED'  , 'owner', null),
  ('3', 'hruser', null, CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task3', 5, 'process_definition_id', 1, 'CREATED'  , 'owner', null),
  ('4', 'hruser', null, CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task4', 10, 'process_definition_id', 1, 'CREATED'  , 'owner', null),
  ('5', 'hruser', null, CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task5', 10, 'process_definition_id', 1, 'COMPLETED'  , 'owner', null),
  ('6', 'hruser', 'bk6', CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task6', 10, 'process_definition_id', 1, 'ASSIGNED'  , 'owner', null);

INSERT INTO PROCESS_VARIABLE (id, create_time, execution_id, last_updated_time, name, process_instance_id, type, ephemeral, "value") VALUES
  (1, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'initiator', 1, 'map', false, JSON '{"value": { "key" : ["1","2","3","4","5"]}}');

INSERT INTO TASK_VARIABLE (id, create_time, execution_id, last_updated_time, name, process_instance_id, task_id, type, "value", ephemeral) VALUES
  (2, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable1', 0, '1', 'String', JSON '{"value": "10"}', false),
  (3, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable2', 0, '1', 'String', JSON '{"value": true}', false),
  (4, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable3', 0, '2', 'String', JSON '{"value": null}', false),
  (5, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable4', 0, '2', 'map', JSON '{"value": { "key" : "data" }}', false),
  (6, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable5', 1, '4', 'String', JSON '{"value": 1.0}', false),
  (7, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable6', 1, '4', 'list', JSON '{"value": [1,2,3,4,5]}', false);

INSERT INTO TASK_PROCESS_VARIABLE (task_id, process_variable_id) VALUES
  (4,1),
  (5,1);
