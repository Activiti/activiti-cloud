alter table process_instance
    add completed_date timestamp;
    add completed_from timestamp;
    add completed_to   timestamp;

create index pi_completedDate_idx on process_instance (completed_date);
create index pi_completedFrom_idx on process_instance (completed_from);
create index pi_completedTo_idx on process_instance (completed_to);
