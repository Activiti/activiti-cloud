-- ─── Closure Table: process_instance_hierarchy ───────────────────────────────
--
-- Pre-computes every ancestor–descendant relationship in the process hierarchy
-- so that finding all subprocesses, linked processes, and their nested children
-- at any depth requires a single JOIN instead of recursive queries.
--
-- Each row means: "process X is an ancestor of process Y at N levels of depth".
--
-- Rows stored for a chain A → B → C:
--   (A, A, 0, self)         -- A is ancestor of itself
--   (A, B, 1, subprocess)   -- A is direct parent of B
--   (A, C, 2, subprocess)   -- A is grandparent of C
--   (B, B, 0, self)
--   (B, C, 1, subprocess)   -- B is direct parent of C
--   (C, C, 0, self)
--
-- The self-row (depth = 0) is needed so that when a new child D is added under
-- C, a single INSERT ... SELECT h.ancestor_id, 'D', h.depth + 1 can pick up C
-- itself (depth 0 → 1) along with all of C's ancestors, without special cases.
--
-- Query example — all descendants of process X:
--   SELECT * FROM process_instance pi
--   JOIN process_instance_hierarchy h ON h.descendant_id = pi.id
--   WHERE h.ancestor_id = :processId AND h.depth > 0;
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS process_instance_hierarchy
(
    ancestor_id   VARCHAR(255) NOT NULL,
    descendant_id VARCHAR(255) NOT NULL,
    depth         INT          NOT NULL DEFAULT 0,
    relation_type VARCHAR(20)  NOT NULL,
    PRIMARY KEY (ancestor_id, descendant_id)
);

CREATE INDEX IF NOT EXISTS idx_pih_ancestor      ON process_instance_hierarchy (ancestor_id);
CREATE INDEX IF NOT EXISTS idx_pih_descendant    ON process_instance_hierarchy (descendant_id);
CREATE INDEX IF NOT EXISTS idx_pih_relation_type ON process_instance_hierarchy (relation_type);

-- ─── Migrate existing data ────────────────────────────────────────────────────
-- The steps below populate the closure table from data already present in
-- process_instance. After this migration every relationship (direct or indirect)
-- is represented as a row in process_instance_hierarchy.
--
-- Step 1 – Create a self-row for each process (depth 0).
--          This lets future inserts treat "the process itself" and "its
--          ancestors" uniformly in a single query (see header comment).
--
-- Step 2 – Create a row for each direct parent → child subprocess (depth 1).
--          Reads the existing parent_id column from process_instance.
--
-- Step 3 – Create a row for each direct linked process relationship (depth 1).
--          Reads the existing linked_process_instance_id column.
--
-- Step 4 – Discover indirect (multi-level) relationships by repeatedly joining
--          the table with itself. For example, if we already know A→B and B→C,
--          this step produces A→C at depth 2. The loop keeps running until no
--          new rows are found, covering any depth.

-- Step 1: Self-rows
INSERT INTO process_instance_hierarchy (ancestor_id, descendant_id, depth, relation_type)
SELECT id, id, 0, 'self'
FROM process_instance
ON CONFLICT DO NOTHING;

-- Step 2: Direct subprocess relationships (parent → child, depth 1)
INSERT INTO process_instance_hierarchy (ancestor_id, descendant_id, depth, relation_type)
SELECT parent_id, id, 1, 'subprocess'
FROM process_instance
WHERE parent_id IS NOT NULL
  AND parent_id <> id
ON CONFLICT DO NOTHING;

-- Step 3: Direct linked-process relationships (depth 1)
INSERT INTO process_instance_hierarchy (ancestor_id, descendant_id, depth, relation_type)
SELECT linked_process_instance_id, id, 1, 'linked'
FROM process_instance
WHERE linked_process_instance_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- Step 4: Propagate indirect relationships (depth 2+).
--         Loops until no new rows are produced. Cycles are impossible in a
--         process hierarchy so this loop always terminates.
DO
$$
    DECLARE
        v_inserted INT := 1;
    BEGIN
        WHILE v_inserted > 0
            LOOP
                INSERT INTO process_instance_hierarchy (ancestor_id, descendant_id, depth, relation_type)
                SELECT DISTINCT h1.ancestor_id,
                                h2.descendant_id,
                                h1.depth + h2.depth,
                                h2.relation_type
                FROM process_instance_hierarchy h1
                         JOIN process_instance_hierarchy h2 ON h1.descendant_id = h2.ancestor_id
                WHERE h1.depth > 0
                  AND h2.depth > 0
                ON CONFLICT DO NOTHING;

                GET DIAGNOSTICS v_inserted = ROW_COUNT;
            END LOOP;
    END
$$;
