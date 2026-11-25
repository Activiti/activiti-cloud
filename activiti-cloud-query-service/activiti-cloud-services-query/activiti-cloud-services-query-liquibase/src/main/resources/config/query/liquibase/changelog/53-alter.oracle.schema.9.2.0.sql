CREATE TABLE task_assignment_history (
    id                VARCHAR2(255 CHAR) NOT NULL,
    task_id           VARCHAR2(255 CHAR) NOT NULL,
    actor             VARCHAR2(255 CHAR),
    previous_assignee VARCHAR2(255 CHAR),
    new_assignee      VARCHAR2(255 CHAR),
    action            VARCHAR2(32 CHAR) NOT NULL,
    timestamp         NUMBER(19) NOT NULL,
    reason            VARCHAR2(255 CHAR),
    PRIMARY KEY (id)
);

CREATE INDEX idx_task_assignment_history_task_time ON task_assignment_history (task_id, timestamp DESC, id DESC);

CREATE TABLE task_assignment_event_receipt (
    id      VARCHAR2(255 CHAR) NOT NULL,
    task_id VARCHAR2(255 CHAR) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_task_assignment_event_task ON task_assignment_event_receipt (task_id);
