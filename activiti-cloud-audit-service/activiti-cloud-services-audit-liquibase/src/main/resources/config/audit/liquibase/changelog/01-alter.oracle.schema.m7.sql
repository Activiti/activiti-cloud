alter table audit_event
    add integration_context CLOB;
alter table audit_event
    add error_message varchar(255);
alter table audit_event
    add error_class_name varchar(255);
alter table audit_event
    add stack_trace_elements CLOB;
