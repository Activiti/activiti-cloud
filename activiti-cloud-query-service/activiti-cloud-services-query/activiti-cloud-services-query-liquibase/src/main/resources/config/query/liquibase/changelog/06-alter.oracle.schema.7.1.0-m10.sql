alter table task
    add completed_by varchar(255);

create index task_completedBy_idx on task (completed_by);

commit;
