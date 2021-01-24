create sequence audit_sequence start 1 increment 1;

alter table audit_event_entity
    rename to audit_event;

alter table audit_event
    add column message text;
alter table audit_event
    add column timer text;
alter table audit_event
    add column error text;

