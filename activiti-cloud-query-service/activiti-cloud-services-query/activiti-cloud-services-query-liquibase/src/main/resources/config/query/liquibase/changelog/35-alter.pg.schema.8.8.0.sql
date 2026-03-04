ALTER TABLE process_instance
  ADD COLUMN IF NOT EXISTS type VARCHAR(255) NOT NULL DEFAULT (
    CASE
      WHEN parent_id IS NOT NULL OR parent_id != id THEN 'call-activity'
      WHEN linked_process_instance_type IS NOT NULL THEN linked_process_instance_type
      ELSE 'main-process'
      END
    );
