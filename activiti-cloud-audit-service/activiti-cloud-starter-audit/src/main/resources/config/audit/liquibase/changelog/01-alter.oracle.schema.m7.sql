alter table audit_event
    add column integration_context CLOB;
alter table audit_event
    add column error_message varchar(255);
alter table audit_event
    add column error_class_name varchar(255);
alter table audit_event
    add column stack_trace_elements CLOB;
    