UPDATE audit_event
SET ephemeral_variable = false
WHERE ephemeral_variable IS NULL;
