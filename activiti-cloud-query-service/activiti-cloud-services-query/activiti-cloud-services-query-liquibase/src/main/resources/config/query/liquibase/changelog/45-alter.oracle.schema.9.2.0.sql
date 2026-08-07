declare
  procedure drop_index_if_exists(p_index_name varchar2) is
    v_count number;
  begin
    select count(*) into v_count from user_indexes where index_name = upper(p_index_name);
    if v_count > 0 then
      execute immediate 'drop index ' || p_index_name;
    end if;
  end;
begin
  drop_index_if_exists('pi_processdefinitionid_idx');
  drop_index_if_exists('pi_name_idx');
  drop_index_if_exists('pi_suspendeddate_idx');
  drop_index_if_exists('idx_process_instance_initiator');
end;
