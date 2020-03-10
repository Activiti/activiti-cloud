alter table audit_event
    add column integration_context text;
    add column error_message varchar(255);
    add column error_class_name varchar(255);
    add column stack_trace_elements text;
    