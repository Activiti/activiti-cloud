insert into process_instance (id, last_modified, last_modified_from, last_modified_to, process_definition_id, status, initiator, start_date, business_key, process_definition_key) values
  ('0', CURRENT_TIMESTAMP, null, null, 'process_definition_id', 'Running', 'initiator',null, 'bus_key','def_key'),
  ('1', CURRENT_TIMESTAMP, null, null, 'process_definition_id', 'Running', 'initiator',null, 'bus_key','def_key');

insert into task (id, assignee, category, create_time, description, due_date, last_modified, last_modified_from, last_modified_to, name, priority, process_definition_id, process_instance_id, status, owner, claim_date) values
  ('1', 'assignee', 'category', CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task1', 'Normal', 'process_definition_id', '0', 'Completed' , 'owner', null),
  ('2', 'assignee', 'category', CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task2', 'High', 'process_definition_id', '0', 'Running' , 'owner', null),
  ('3', 'assignee', 'category', CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task3', 'Normal', 'process_definition_id', '0', 'Running' , 'owner', null),
  ('4', 'assignee', 'category', CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task4', 'High', 'process_definition_id', '1', 'Running', 'owner', null ),
  ('5', 'assignee', 'category', CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task5', 'Normal', 'process_definition_id', '1', 'Completed', 'owner', null ),
  ('6', 'assignee', 'category', CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task6', 'Normal', 'process_definition_id', '1', 'Suspended' , 'owner', null),
  ('7', '', 'category', CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task7', 'Normal', 'process_definition_id', '1', 'Suspended', 'owner', null ),
  ('8', 'assignee', 'category', CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task8', 'Normal', null, null, 'ASSIGNED', 'owner', null);

insert into variable (create_time, execution_id, last_updated_time, name, process_instance_id, task_id, type, value) values
  (CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable1', '0', '1', 'String', 'value1'),
  (CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable2', '0', '1', 'String', 'value2'),
  (CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable3', '0', '2', 'String', 'value3'),
  (CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable4', '0', '2', 'String', 'value4'),
  (CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable5', '1', '4', 'String', 'value5'),
  (CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable6', '1', '4', 'String', 'value6'),
  (CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable7', '1', '5', 'String', 'value7'),
  (CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'initiator', '1', null, 'String', 'admin');

