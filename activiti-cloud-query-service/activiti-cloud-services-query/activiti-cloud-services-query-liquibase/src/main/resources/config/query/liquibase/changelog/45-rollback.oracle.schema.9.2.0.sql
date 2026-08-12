create index pi_processdefinitionid_idx on process_instance(process_definition_id);
create index pi_name_idx on process_instance(name);
create index pi_suspendeddate_idx on process_instance(suspended_date);
create index idx_process_instance_initiator on process_instance(initiator);
