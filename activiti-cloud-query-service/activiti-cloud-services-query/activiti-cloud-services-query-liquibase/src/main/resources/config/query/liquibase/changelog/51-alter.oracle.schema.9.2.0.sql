alter table task
    add allow_self_service_declared number(1,0);
update task set allow_self_service = 0 where allow_self_service is null;
alter table task
    modify (allow_self_service number(1,0) default 0 not null);
