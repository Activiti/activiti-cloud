alter table process_instance
    add completed_date timestamp;

create index pi_completedDate_idx on process_instance (completed_date);

commit;
