create index if not exists pi_processdefinitionid_idx on process_instance(process_definition_id);
create index if not exists pi_name_idx on process_instance(name);
create index if not exists pi_suspendeddate_idx on process_instance(suspended_date);
create index if not exists idx_process_instance_initiator on process_instance(initiator);
