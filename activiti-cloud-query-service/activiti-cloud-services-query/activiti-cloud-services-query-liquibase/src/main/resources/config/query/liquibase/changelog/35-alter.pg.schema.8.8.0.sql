ALTER TABLE process_instance
  ADD COLUMN IF NOT EXISTS type VARCHAR(255) DEFAULT 'main-process';

UPDATE process_instance
SET type =
  CASE
      WHEN linked_process_instance_type IS NOT NULL THEN linked_process_instance_type
      WHEN parent_id IS NOT NULL AND parent_id != id THEN 'call-activity'
      ELSE type
  END;
