CREATE TABLE project_configuration
(
    project_id VARCHAR(255) NOT NULL,
    enable_candidate_starters boolean NOT NULL DEFAULT FALSE,
    PRIMARY KEY (project_id),
    FOREIGN KEY (project_id) REFERENCES project (id)
);

INSERT INTO project_configuration (project_id, enable_candidate_starters)
SELECT id, false
FROM project;
