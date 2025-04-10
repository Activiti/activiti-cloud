UPDATE process_variable
SET ephemeral = false
WHERE ephemeral IS NULL;
