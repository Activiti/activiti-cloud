alter table process_definition
    add column category varchar(255);

create sequence process_variable_sequence start with 1 increment by 50;

select setval('process_variable_sequence', (select max(id) from process_variable));

create sequence task_variable_sequence start with 1 increment by 50;

select setval('task_variable_sequence', (select max(id) from task_variable));
