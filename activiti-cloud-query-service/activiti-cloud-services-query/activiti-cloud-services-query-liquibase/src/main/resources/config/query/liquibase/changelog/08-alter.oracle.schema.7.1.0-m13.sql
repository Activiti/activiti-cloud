alter table process_instance
    add suspended_date timestamp;

create index pi_suspendedDate_idx on process_instance (suspended_date);

update process_instance pi
    set suspended_date = (select p_i.last_modified
                             from process_instance p_i
                             where (p_i.id = pi.id AND pi.status = 'SUSPENDED'));

commit;
