create table process_variable_history
(
    id                  NUMBER(19,0) GENERATED ALWAYS AS IDENTITY NOT NULL,
    process_instance_id varchar(255) not null,
    variable_name       varchar(255) not null,
    type                varchar(255),
    value               json,
    deleted             NUMBER(1,0) default 0 not null,
    create_time         timestamp not null,
    message_id          varchar(255),
    command_id          varchar(255),
    sequence_number     NUMBER(10,0),
    primary key (id)
);

create index idx_pvh_process_var on process_variable_history (process_instance_id, variable_name, create_time);
