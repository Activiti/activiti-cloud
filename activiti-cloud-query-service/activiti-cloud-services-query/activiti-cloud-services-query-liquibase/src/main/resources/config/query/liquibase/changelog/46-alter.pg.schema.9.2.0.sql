CREATE INDEX CONCURRENTLY IF NOT EXISTS bpmn_activity_activitytype_starteddate_idx ON bpmn_activity (activity_type, started_date DESC NULLS LAST);
