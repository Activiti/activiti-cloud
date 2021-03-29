alter table process_instance
    add column suspended_date timestamp;

create index pi_suspendedDate_idx on process_instance (suspended_date);

update process_instance as pi
    set suspended_date = (select p_i.last_modified
                             from process_instance as p_i
                             where (p_i.id = pi.id AND pi.status = 'SUSPENDED'));
