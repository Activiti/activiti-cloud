insert into task_process_variable(task_id, process_variable_id)
select t.id, pv.id
from process_variable pv
       join task t on pv.process_instance_id = t.process_instance_id
where not exists (
  select * from task_process_variable tpv
  where  tpv.task_id = t.id and tpv.process_variable_id = pv.id
  );
