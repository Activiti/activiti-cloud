ALTER TABLE process_instance
  ADD COLUMN IF NOT EXISTS type VARCHAR(255) NOT NULL DEFAULT 'main-process';

UPDATE process_instance
SET type =
  CASE
      WHEN linked_process_instance_type IS NOT NULL THEN linked_process_instance_type
      WHEN parent_id IS NOT NULL AND parent_id != id THEN 'call-activity'
      ELSE type
  END;

CREATE INDEX idx_linked_process_instance_id_type
  ON process_instance(linked_process_instance_id, type);

CREATE INDEX idx_root_process_instance_id_type
  ON process_instance(root_process_instance_id, type);
