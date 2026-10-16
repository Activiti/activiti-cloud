alter table task
    alter column allow_self_service drop not null;
alter table task
    alter column allow_self_service drop default;
