alter table process_instance
    add column completed_date timestamp;

create index pi_completedDate_idx on process_instance (completed_date);

update process_instance as pi
    set completed_date = (select p_i.last_modified
                             from process_instance as p_i
                             where p_i.id = pi.id);
