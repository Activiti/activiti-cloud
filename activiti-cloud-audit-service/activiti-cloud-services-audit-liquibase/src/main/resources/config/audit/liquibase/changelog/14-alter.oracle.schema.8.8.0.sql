UPDATE audit_event
SET ephemeral_variable = 0
WHERE ephemeral_variable IS NULL;
