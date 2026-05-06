ALTER TABLE audit_event ADD severity varchar2(255);
UPDATE audit_event SET severity = 'ERROR' WHERE severity IS NULL AND event_type = 'INCIDENT_CREATED';
