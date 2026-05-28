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
package org.activiti.cloud.services.query.events.handlers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.activiti.cloud.services.query.model.ProcessInstanceHierarchyEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceHierarchyEntity_;
import org.activiti.cloud.services.query.model.ProcessInstanceHierarchyId;

/**
 * Exposes methods to handle process instance hierarchy relationships (parent-child and linked)
 * by manipulating the {@code process_instance_hierarchy} closure table.
 *
 * <p>Uses {@link EntityManager} directly so that all writes share the same
 * transaction as the surrounding event handler.
 */
public class ProcessInstanceHierarchyServiceImpl implements ProcessInstanceHierarchyService {

    private final EntityManager entityManager;

    public ProcessInstanceHierarchyServiceImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void registerProcess(String processId) {
        insertIfAbsent(processId, processId, 0, ProcessInstanceHierarchyEntity.RELATION_SELF);
    }

    @Override
    public void registerSubprocess(String processId, String parentId) {
        insertIfAbsent(processId, processId, 0, ProcessInstanceHierarchyEntity.RELATION_SELF);

        // Propagate: for every ancestor of parentId (including self-ref), create a new row pointing to processId
        for (ProcessInstanceHierarchyEntity ancestor : findByDescendantId(parentId)) {
            insertIfAbsent(ancestor.getAncestorId(), processId, ancestor.getDepth() + 1, ancestor.getRelationType());
        }
    }

    @Override
    public void registerLinkedProcess(String processId, String linkedProcessInstanceId) {
        // Propagate: for every ancestor of linkedProcessInstanceId (including self-ref), create a linked row
        for (ProcessInstanceHierarchyEntity ancestor : findByDescendantId(linkedProcessInstanceId)) {
            insertIfAbsent(
                ancestor.getAncestorId(),
                processId,
                ancestor.getDepth() + 1,
                ProcessInstanceHierarchyEntity.RELATION_LINKED
            );
        }
    }

    @Override
    public void removeProcess(String processId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<ProcessInstanceHierarchyEntity> delete = cb.createCriteriaDelete(
            ProcessInstanceHierarchyEntity.class
        );
        Root<ProcessInstanceHierarchyEntity> root = delete.from(ProcessInstanceHierarchyEntity.class);
        Predicate isAncestor = cb.equal(root.get(ProcessInstanceHierarchyEntity_.ancestorId), processId);
        Predicate isDescendant = cb.equal(root.get(ProcessInstanceHierarchyEntity_.descendantId), processId);
        delete.where(cb.or(isAncestor, isDescendant));
        entityManager.createQuery(delete).executeUpdate();
    }

    private List<ProcessInstanceHierarchyEntity> findByDescendantId(String descendantId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ProcessInstanceHierarchyEntity> query = cb.createQuery(ProcessInstanceHierarchyEntity.class);
        Root<ProcessInstanceHierarchyEntity> root = query.from(ProcessInstanceHierarchyEntity.class);
        query.where(cb.equal(root.get(ProcessInstanceHierarchyEntity_.descendantId), descendantId));
        return entityManager.createQuery(query).getResultList();
    }

    private void insertIfAbsent(String ancestorId, String descendantId, int depth, String relationType) {
        if (
            entityManager.find(
                ProcessInstanceHierarchyEntity.class,
                new ProcessInstanceHierarchyId(ancestorId, descendantId)
            ) ==
            null
        ) {
            entityManager.persist(new ProcessInstanceHierarchyEntity(ancestorId, descendantId, depth, relationType));
        }
    }
}
