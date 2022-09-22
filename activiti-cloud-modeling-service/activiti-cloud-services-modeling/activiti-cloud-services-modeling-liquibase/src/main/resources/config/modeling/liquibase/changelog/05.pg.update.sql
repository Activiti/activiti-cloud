create table project_configuration
(
    id varchar(255) not null,
    enable_candidate_starters boolean,
    primary key (id),
    foreign key (id) references project (id)
);
