create sequence audit_sequence start with 1 increment by 50;
create table audit_event
(
    type                       varchar(31) not null,
    id                         bigint      not null,
    app_name                   varchar(255),
    app_version                varchar(255),
    business_key               varchar(255),
    entity_id                  varchar(255),
    event_id                   varchar(255),
    event_type                 varchar(255),
    message_id                 varchar(255),
    parent_process_instance_id varchar(255),
    process_definition_id      varchar(255),
    process_definition_key     varchar(255),
    process_instance_id        varchar(255),
    sequence_number            integer     not null,
    service_full_name          varchar(255),
    service_name               varchar(255),
    service_type               varchar(255),
    service_version            varchar(255),
    timestamp                  bigint,
    bpmn_activity              text,
    cause                      varchar(255),
    error                      text,
    flow_node_id               varchar(255),
    integration_context_id     varchar(255),
    message                    text,
    process_instance           text,
    process_definition         text,
    sequence_flow              text,
    signal                     text,
    task                       text,
    task_id                    varchar(255),
    task_name                  varchar(255),
    candidate_group            text,
    candidate_user             text,
    timer                      text,
    variable_instance          text,
    variable_name              varchar(255),
    variable_type              varchar(255),
    variable_previous_value    text,
    message_subscription	   text,
    error_code				   varchar(255),
    error_message			   varchar(255),
    error_class_name		   varchar(255),
    integration_context		   text,
    stack_trace_elements	   text,
    deployment                 text,
    primary key (id)
);

CREATE INDEX audit_event_event_id_idx ON audit_event(event_id);

