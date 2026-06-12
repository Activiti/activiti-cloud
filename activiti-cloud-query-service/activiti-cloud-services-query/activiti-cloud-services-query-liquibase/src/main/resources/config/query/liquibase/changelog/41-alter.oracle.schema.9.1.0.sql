create table process_variable_history
(
    id                  NUMBER(19,0) GENERATED ALWAYS AS IDENTITY NOT NULL,
    process_instance_id VARCHAR2(255) not null,
    variable_name       VARCHAR2(255) not null,
    type                VARCHAR2(255),
    value               json,
    deleted             NUMBER(1,0) default 0 not null,
    event_time          timestamp not null,
    record_create_time  timestamp default CURRENT_TIMESTAMP not null,
    message_id          VARCHAR2(255),
    command_id          VARCHAR2(255),
    sequence_number     NUMBER(10,0),
    primary key (id)
);

create index idx_pvh_process_var on process_variable_history (process_instance_id, variable_name, event_time);
create index idx_pvh_record_create_time on process_variable_history (record_create_time);
