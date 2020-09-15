alter table process_instance
    add completed_date timestamp;

create index pi_completedDate_idx on process_instance (completed_date);

update process_instance pi
    set completed_date = (select p_i.last_modified
                             from process_instance p_i
                             where p_i.id = pi.id);

commit;
