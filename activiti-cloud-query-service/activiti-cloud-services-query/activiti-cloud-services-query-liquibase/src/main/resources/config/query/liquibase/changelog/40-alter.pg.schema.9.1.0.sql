create sequence process_variable_history_sequence start with 1 increment by 50;

create table process_variable_history
(
    id                  bigint not null default nextval('process_variable_history_sequence'),
    process_instance_id varchar(255) not null,
    variable_name       varchar(255) not null,
    type                varchar(255),
    "value"             jsonb,
    create_time         timestamp not null,
    message_id          varchar(255),
    command_id          varchar(255),
    sequence_number     integer,
    primary key (id)
);

create index idx_pvh_process_var on process_variable_history (process_instance_id, variable_name, create_time);
