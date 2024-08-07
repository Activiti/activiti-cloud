create table if not exists PROCESS_VARIABLE (
  marked_as_deleted boolean,
  create_time timestamp(6),
  id bigint not null,
  last_updated_time timestamp(6),
  app_name varchar(255),
  app_version varchar(255),
  execution_id varchar(255),
  name varchar(255),
  process_definition_key varchar(255),
  process_instance_id varchar(255),
  service_full_name varchar(255),
  service_name varchar(255),
  service_type varchar(255),
  service_version varchar(255),
  type varchar(255),
  variable_definition_id varchar(255),
  "value" json,
  primary key (id)
);

create table if not exists TASK_VARIABLE (
  marked_as_deleted boolean,
  create_time timestamp(6),
  id bigint not null,
  last_updated_time timestamp(6),
  app_name varchar(255),
  app_version varchar(255),
  execution_id varchar(255),
  name varchar(255),
  process_instance_id varchar(255),
  service_full_name varchar(255),
  service_name varchar(255),
  service_type varchar(255),
  service_version varchar(255),
  task_id varchar(255),
  type varchar(255),
  "value" json,
  primary key (id)
);

insert into process_instance (id, last_modified, last_modified_from, last_modified_to, process_definition_id, status, initiator, start_date, business_key, process_definition_key) values
  ('0', CURRENT_TIMESTAMP, null, null, 'process_definition_id', 'RUNNING', 'initiator',null, 'bus_key','def_key'),
  ('1', CURRENT_TIMESTAMP, null, null, 'process_definition_id', 'RUNNING', 'initiator',null, 'bus_key','def_key');

insert into task (id, assignee, business_key, created_date, description, due_date, last_modified, last_modified_from, last_modified_to, name, priority, process_definition_id, process_instance_id, status, owner, claimed_date) values
  ('1', 'assignee', 'bk1', CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task1', 5, 'process_definition_id', 0, 'COMPLETED'  , 'owner', null),
  ('2', 'assignee', null, CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task2', 10, 'process_definition_id', 0, 'CREATED'  , 'owner', null),
  ('3', 'assignee', null, CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task3', 5, 'process_definition_id', 0, 'CREATED'  , 'owner', null),
  ('4', 'assignee', null, CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task4', 10, 'process_definition_id', 1, 'CREATED'  , 'owner', null),
  ('5', 'assignee', null, CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task5', 10, 'process_definition_id', 1, 'COMPLETED'  , 'owner', null),
  ('6', 'assignee', 'bk6', CURRENT_TIMESTAMP, 'description', null, null, null, null, 'task6', 10, 'process_definition_id', 0, 'ASSIGNED'  , 'owner', null);

insert into PROCESS_VARIABLE (id, create_time, execution_id, last_updated_time, name, process_instance_id, type, "value") values
  (1, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'initiator', 1, 'map', JSON '{"value": { "key" : ["1","2","3","4","5"]}}');

insert into TASK_VARIABLE (id, create_time, execution_id, last_updated_time, name, process_instance_id, task_id, type, "value") values
  (2, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable1', 0, '1', 'String', JSON '{"value": "10"}'),
  (3, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable2', 0, '1', 'String', JSON '{"value": true}'),
  (4, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable3', 0, '2', 'String', JSON '{"value": null}'),
  (5, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable4', 0, '2', 'map', JSON '{"value": { "key" : "data" }}'),
  (6, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable5', 1, '4', 'String', JSON '{"value": 1.0}'),
  (7, CURRENT_TIMESTAMP, 'execution_id', CURRENT_TIMESTAMP, 'variable6', 1, '4', 'list', JSON '{"value": [1,2,3,4,5]}');

insert into TASK_PROCESS_VARIABLE (task_id, process_variable_id) values
  (4,1),
  (5,1);
