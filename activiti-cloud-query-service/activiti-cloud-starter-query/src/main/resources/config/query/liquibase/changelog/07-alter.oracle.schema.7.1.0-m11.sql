create table application
(
    id                         varchar(255) not null,
    name                       varchar(255) not null,
    version                    varchar(255),
    primary key (id)
);

INSERT INTO application
SELECT DISTINCT CONCAT(app_name, app_version), app_name, app_version
FROM process_instance;

commit;