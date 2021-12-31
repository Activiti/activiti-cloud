alter sequence audit_sequence increment by 50;
select setval('audit_sequence', (select max(id) from audit_event));
