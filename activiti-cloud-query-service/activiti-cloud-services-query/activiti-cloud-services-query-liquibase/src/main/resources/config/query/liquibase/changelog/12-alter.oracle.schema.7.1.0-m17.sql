declare max_task_variable number;
begin
    select nvl(max(id), 0) + 1 into max_task_variable from task_variable;
    execute immediate 'create sequence task_variable_sequence start with ' || max_task_variable || ' increment by 50';
end;
/
