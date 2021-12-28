alter table process_definition
    add category varchar(255);

declare max_process_variable number
begin
    select nvl(max(id), 0) + 1 into max_process_variable from process_variable
    execute immediate 'create sequence process_variable_sequence start with ' || max_process_variable || ' increment by 50'
end;

declare max_task_variable number
begin
    select nvl(max(id), 0) + 1 into max_task_variable from task_variable
    execute immediate 'create sequence task_variable_sequence start with ' || max_task_variable || ' increment by 50'
end;
