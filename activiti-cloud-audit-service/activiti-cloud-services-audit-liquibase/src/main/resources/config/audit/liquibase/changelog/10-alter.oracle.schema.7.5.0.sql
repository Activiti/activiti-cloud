alter table audit_event
  add column candidate_starter_user CLOB;
alter table audit_event
  add column candidate_starter_group CLOB;
