alter table task
    modify (allow_self_service number(1,0) default null null);
alter table task
    drop column allow_self_service_declared;
