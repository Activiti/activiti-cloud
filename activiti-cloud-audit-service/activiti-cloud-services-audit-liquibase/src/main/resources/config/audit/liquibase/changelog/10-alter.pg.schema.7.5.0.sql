alter table audit_event
  add column candidate_starter_user text;
alter table audit_event
  add column candidate_starter_group text;
