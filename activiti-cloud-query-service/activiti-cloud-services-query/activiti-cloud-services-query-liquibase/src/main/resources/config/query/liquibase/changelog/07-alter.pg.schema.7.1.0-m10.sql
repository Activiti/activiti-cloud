alter table task
    add column process_definition_name varchar(255);
    
create index task_processDefinitionName_idx on task (process_definition_name);

update task as ts
  set process_definition_name = (select pd.name 
                                    from process_definition as pd 
                                    where pd.id = ts.process_definition_id);