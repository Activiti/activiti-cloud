CREATE INDEX IF NOT EXISTS audit_event_timestamp_idx ON audit_event USING btree (timestamp)
