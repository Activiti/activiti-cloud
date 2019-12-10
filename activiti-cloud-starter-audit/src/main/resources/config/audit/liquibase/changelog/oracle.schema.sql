create table process_variable
(
    id                  NUMBER(19,0) GENERATED ALWAYS AS IDENTITY MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE  NOKEEP  NOSCALE  NOT NULL ENABLE,
    app_name            varchar(255),
    app_version         varchar(255),
    service_full_name   varchar(255),
    service_name        varchar(255),
    service_type        varchar(255),
    service_version     varchar(255),
    create_time         timestamp,
    execution_id        varchar(255),
    last_updated_time   timestamp,
    marked_as_deleted   NUMBER(1,0),
    name                varchar(255),
    process_instance_id varchar(255),
    type                varchar(255),
    value               LONG,
    primary key (id)
)