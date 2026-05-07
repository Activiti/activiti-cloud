create index if not exists idx_process_instance_status_pdk_lastmodified
on process_instance (process_definition_key, status, last_modified, id);

create index if not exists idx_task_status_pdk_lastmodified
on task (process_definition_key, status, last_modified, id);
