BEGIN
  BEGIN
    EXECUTE IMMEDIATE 'DROP INDEX idx_audit_event_timestamp';
  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE != -1418 THEN RAISE; END IF;
  END;
  EXECUTE IMMEDIATE 'CREATE INDEX idx_audit_event_timestamp_desc_nulls_last ON audit_event (timestamp DESC NULLS LAST)';
END;
