alter table process_instance
    add process_definition_name varchar(255);

create index pi_processDefinitionName_idx on process_instance (process_definition_name);

update process_instance pi
  set process_definition_name = (select pd.name
                                    from process_definition pd
                                    where pd.id = pi.process_definition_id);

commit;
