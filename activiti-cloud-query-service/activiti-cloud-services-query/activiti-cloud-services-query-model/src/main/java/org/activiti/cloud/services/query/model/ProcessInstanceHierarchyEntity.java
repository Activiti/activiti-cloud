/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.query.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Pre-computes every ancestor–descendant relationship in the process hierarchy
 * so that finding all subprocesses, linked processes, and their nested children
 * at any depth requires a single JOIN instead of recursive queries.
 *
 * <p>Each row means: "process {@code X} is an ancestor of process {@code Y}
 * at {@code N} levels of depth".
 *
 * <p>Rows stored for a chain A → B → C:
 * <pre>
 *   (A, A, 0, self)         — A is ancestor of itself
 *   (A, B, 1, subprocess)   — A is direct parent of B
 *   (A, C, 2, subprocess)   — A is grandparent of C
 *   (B, B, 0, self)
 *   (B, C, 1, subprocess)   — B is direct parent of C
 *   (C, C, 0, self)
 * </pre>
 *
 * <p>The self-row (depth&nbsp;=&nbsp;0) is needed so that when a new child D is added
 * under C, a single {@code INSERT ... SELECT h.ancestor_id, 'D', h.depth + 1}
 * can pick up C itself (depth 0&nbsp;→&nbsp;1) along with all of C's ancestors,
 * without special cases.
 *
 * <p>Query example — all descendants of process X:
 * <pre>{@code
 *   SELECT pi.*
 *   FROM process_instance pi
 *   JOIN process_instance_hierarchy h
 *     ON h.ancestor_id = :rootId AND h.descendant_id = pi.id AND h.depth > 0
 * }</pre>
 */
@Entity(name = "ProcessInstanceHierarchy")
@IdClass(ProcessInstanceHierarchyId.class)
@Table(
    name = "process_instance_hierarchy",
    indexes = {
        @Index(name = "idx_pih_ancestor", columnList = "ancestor_id"),
        @Index(name = "idx_pih_descendant", columnList = "descendant_id"),
        @Index(name = "idx_pih_relation_type", columnList = "relation_type"),
    }
)
public class ProcessInstanceHierarchyEntity {

    public static final String RELATION_SELF = "self";
    public static final String RELATION_SUBPROCESS = "subprocess";
    public static final String RELATION_LINKED = "linked";

    @Id
    private String ancestorId;

    @Id
    private String descendantId;

    private int depth;

    private String relationType;

    public ProcessInstanceHierarchyEntity() {}

    public ProcessInstanceHierarchyEntity(String ancestorId, String descendantId, int depth, String relationType) {
        this.ancestorId = ancestorId;
        this.descendantId = descendantId;
        this.depth = depth;
        this.relationType = relationType;
    }

    public String getAncestorId() {
        return ancestorId;
    }

    public void setAncestorId(String ancestorId) {
        this.ancestorId = ancestorId;
    }

    public String getDescendantId() {
        return descendantId;
    }

    public void setDescendantId(String descendantId) {
        this.descendantId = descendantId;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }
}
