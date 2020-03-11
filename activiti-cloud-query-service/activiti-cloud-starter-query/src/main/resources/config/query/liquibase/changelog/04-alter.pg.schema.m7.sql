alter table bpmn_activity
    add column error_message varchar(255);
alter table bpmn_activity
    add column error_class_name varchar(255);
alter table bpmn_activity
    add column error_date timestamp;
alter table bpmn_activity
    add column stack_trace_elements text;
    