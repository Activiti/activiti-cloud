CREATE TABLE task_assignment_history (
    id                VARCHAR(255) NOT NULL,
    task_id           VARCHAR(255) NOT NULL,
    actor             VARCHAR(255),
    previous_assignee VARCHAR(255),
    new_assignee      VARCHAR(255),
    action            VARCHAR(32) NOT NULL,
    timestamp         BIGINT NOT NULL,
    reason            VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE INDEX idx_task_assignment_history_task_time ON task_assignment_history (task_id, timestamp DESC, id DESC);

CREATE TABLE task_assignment_event_receipt (
    id      VARCHAR(255) NOT NULL,
    task_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_task_assignment_event_task ON task_assignment_event_receipt (task_id);
