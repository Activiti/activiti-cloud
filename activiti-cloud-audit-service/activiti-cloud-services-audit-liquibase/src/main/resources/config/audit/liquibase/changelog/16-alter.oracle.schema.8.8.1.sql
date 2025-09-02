CREATE INDEX IF NOT EXISTS audit_event_entity_id_idx ON audit_event USING btree (entity_id)
