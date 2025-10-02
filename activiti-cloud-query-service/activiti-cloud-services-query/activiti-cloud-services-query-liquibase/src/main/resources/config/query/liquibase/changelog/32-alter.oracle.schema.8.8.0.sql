UPDATE process_variable
SET ephemeral = 0
WHERE ephemeral IS NULL;
