alter table task
    add process_definition_name varchar(255);

create index task_processDefinitionName_idx on task (process_definition_name);
    
update task ts
  set process_definition_name = (select pd.name 
                                    from process_definition pd 
                                    where pd.id = ts.process_definition_id);

commit;