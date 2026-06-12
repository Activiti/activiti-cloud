-- Creates the closure table that pre-computes every ancestor-descendant pair in
-- the process-instance hierarchy (subprocesses and linked processes).
CREATE TABLE process_instance_hierarchy
(
    ancestor_id   VARCHAR2(255) NOT NULL,
    descendant_id VARCHAR2(255) NOT NULL,
    depth         NUMBER(10)    DEFAULT 0 NOT NULL,
    relation_type VARCHAR2(20)  NOT NULL,
    CONSTRAINT pk_pih PRIMARY KEY (ancestor_id, descendant_id)
);

CREATE INDEX idx_pih_ancestor      ON process_instance_hierarchy (ancestor_id);
CREATE INDEX idx_pih_descendant    ON process_instance_hierarchy (descendant_id);
CREATE INDEX idx_pih_relation_type ON process_instance_hierarchy (relation_type);

-- 1. Self-references for every process instance
MERGE INTO process_instance_hierarchy t
USING (SELECT id, 'self' AS rel FROM process_instance) s
ON (t.ancestor_id = s.id AND t.descendant_id = s.id)
WHEN NOT MATCHED THEN
    INSERT (ancestor_id, descendant_id, depth, relation_type)
    VALUES (s.id, s.id, 0, s.rel);

-- 2. Direct subprocess relationships (parent -> child, depth 1)
MERGE INTO process_instance_hierarchy t
USING (
    SELECT parent_id, id
    FROM process_instance
    WHERE parent_id IS NOT NULL
      AND parent_id <> id
) s
ON (t.ancestor_id = s.parent_id AND t.descendant_id = s.id)
WHEN NOT MATCHED THEN
    INSERT (ancestor_id, descendant_id, depth, relation_type)
    VALUES (s.parent_id, s.id, 1, 'subprocess');

-- 3. Direct linked-process relationships (linked -> process, depth 1)
MERGE INTO process_instance_hierarchy t
USING (
    SELECT linked_process_instance_id, id
    FROM process_instance
    WHERE linked_process_instance_id IS NOT NULL
) s
ON (t.ancestor_id = s.linked_process_instance_id AND t.descendant_id = s.id)
WHEN NOT MATCHED THEN
    INSERT (ancestor_id, descendant_id, depth, relation_type)
    VALUES (s.linked_process_instance_id, s.id, 1, 'linked');

-- 4. Propagate transitive closure iteratively
DECLARE
    v_inserted NUMBER := 1;
BEGIN
    WHILE v_inserted > 0 LOOP
        MERGE INTO process_instance_hierarchy t
        USING (
            SELECT DISTINCT h1.ancestor_id,
                            h2.descendant_id,
                            h1.depth + h2.depth AS combined_depth,
                            h2.relation_type
            FROM process_instance_hierarchy h1
                     JOIN process_instance_hierarchy h2 ON h1.descendant_id = h2.ancestor_id
            WHERE h1.depth > 0
              AND h2.depth > 0
        ) s
        ON (t.ancestor_id = s.ancestor_id AND t.descendant_id = s.descendant_id)
        WHEN NOT MATCHED THEN
            INSERT (ancestor_id, descendant_id, depth, relation_type)
            VALUES (s.ancestor_id, s.descendant_id, s.combined_depth, s.relation_type);

        v_inserted := SQL%ROWCOUNT;
    END LOOP;
END;
