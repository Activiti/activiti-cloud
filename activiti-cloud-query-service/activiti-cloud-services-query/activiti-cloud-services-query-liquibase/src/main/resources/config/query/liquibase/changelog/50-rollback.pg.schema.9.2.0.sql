update task set allow_self_service = false where allow_self_service is null;
alter table task
    alter column allow_self_service set default false;
alter table task
    alter column allow_self_service set not null;
