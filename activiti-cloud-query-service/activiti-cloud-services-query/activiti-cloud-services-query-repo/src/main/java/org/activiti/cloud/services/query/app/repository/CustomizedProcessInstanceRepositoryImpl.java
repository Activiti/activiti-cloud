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
package org.activiti.cloud.services.query.app.repository;

import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.cloud.api.process.model.QueryCloudSubprocessInstance;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QProcessInstanceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

public class CustomizedProcessInstanceRepositoryImpl
    extends QuerydslRepositorySupport
    implements CustomizedProcessInstanceRepository {

    private final JPAQueryFactory queryFactory;

    public CustomizedProcessInstanceRepositoryImpl(EntityManager entityManager) {
        super(ProcessInstanceEntity.class);
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public Page<ProcessInstanceEntity> mapSubprocesses(
        Page<ProcessInstanceEntity> processInstances,
        Pageable pageable
    ) {
        return mapSubprocesses(processInstances);
    }

    @Override
    public Page<ProcessInstanceEntity> mapSubprocesses(Page<ProcessInstanceEntity> processInstances) {
        List<ProcessInstanceEntity> content = processInstances.getContent();
        if (content.isEmpty()) {
            return processInstances;
        }

        Set<String> pageIds = content.stream().map(ProcessInstanceEntity::getId).collect(Collectors.toSet());
        List<String> rootIds = content.stream()
            .map(ProcessInstanceEntity::getRootProcessInstanceId)
            .filter(id -> id != null)
            .distinct()
            .toList();

        if (rootIds.isEmpty()) {
            content.forEach(pi -> pi.setSubprocesses(Set.of()));
            return processInstances;
        }

        // Single query: all descendants at every level belonging to the same process trees
        List<ProcessInstanceEntity> allDescendants = findAllDescendantsByRootIds(rootIds, pageIds);

        // parentId → children map (covers all levels)
        Map<String, Set<QueryCloudSubprocessInstance>> childrenByParentId = buildChildrenByParentIdMap(allDescendants);

        // For each page entity, recursively collect its full subtree
        content.forEach(pi -> pi.setSubprocesses(collectAllSubprocesses(pi.getId(), childrenByParentId)));

        return processInstances;
    }

    @Override
    public ProcessInstanceEntity mapSubprocesses(ProcessInstanceEntity processInstance) {
        String rootId = processInstance.getRootProcessInstanceId();
        if (rootId == null) {
            processInstance.setSubprocesses(Set.of());
            return processInstance;
        }

        List<ProcessInstanceEntity> allDescendants = findAllDescendantsByRootIds(
            List.of(rootId),
            Set.of(processInstance.getId())
        );

        Map<String, Set<QueryCloudSubprocessInstance>> childrenByParentId = buildChildrenByParentIdMap(allDescendants);

        processInstance.setSubprocesses(collectAllSubprocesses(processInstance.getId(), childrenByParentId));

        return processInstance;
    }

    /**
     * Fetches all process instances that share any of the given root process instance ids,
     * excluding the page-level entries themselves (they are not their own subprocesses).
     */
    public List<ProcessInstanceEntity> findAllDescendantsByRootIds(List<String> rootIds, Set<String> excludeIds) {
        QProcessInstanceEntity pi = QProcessInstanceEntity.processInstanceEntity;

        JPQLQuery<ProcessInstanceEntity> query = queryFactory
            .selectFrom(pi)
            .where(pi.rootProcessInstanceId.in(rootIds).and(pi.id.notIn(excludeIds)));

        return query.fetch();
    }

    /**
     * Builds a map of parentId -> direct children (as QueryCloudSubprocessInstance).
     * Used to traverse the tree in memory.
     */
    public Map<String, Set<QueryCloudSubprocessInstance>> buildChildrenByParentIdMap(
        List<ProcessInstanceEntity> descendants
    ) {
        return descendants.stream()
            .filter(d -> d.getParentId() != null)
            .collect(
                Collectors.groupingBy(
                    ProcessInstanceEntity::getParentId,
                    Collectors.mapping(this::getQueryCloudSubprocessInstance, Collectors.toSet())
                )
            );
    }

    /**
     * Recursively collects all subprocess ids (direct and indirect) for a given process instance id.
     */
    public Set<QueryCloudSubprocessInstance> collectAllSubprocesses(
        String processId,
        Map<String, Set<QueryCloudSubprocessInstance>> childrenByParentId
    ) {
        Set<QueryCloudSubprocessInstance> directChildren = childrenByParentId.getOrDefault(processId, Set.of());
        if (directChildren.isEmpty()) {
            return Set.of();
        }

        Set<QueryCloudSubprocessInstance> all = new HashSet<>(directChildren);
        for (QueryCloudSubprocessInstance child : directChildren) {
            all.addAll(collectAllSubprocesses(child.getId(), childrenByParentId));
        }
        return all;
    }

    public QueryCloudSubprocessInstance getQueryCloudSubprocessInstance(ProcessInstanceEntity subprocess) {
        QueryCloudSubprocessInstance subProcessInstance = new QueryCloudSubprocessInstance();
        subProcessInstance.setId(subprocess.getId());
        subProcessInstance.setProcessDefinitionName(subprocess.getProcessDefinitionName());
        return subProcessInstance;
    }

    public List<String> getParentIds(Page<ProcessInstanceEntity> processInstances) {
        return processInstances.getContent().stream().map(ProcessInstanceEntity::getId).toList();
    }

    public Map<String, Set<QueryCloudSubprocessInstance>> groupSubprocesses(List<ProcessInstanceEntity> subprocesses) {
        return subprocesses
            .stream()
            .collect(
                Collectors.groupingBy(
                    ProcessInstanceEntity::getParentId,
                    Collectors.mapping(this::getQueryCloudSubprocessInstance, Collectors.toSet())
                )
            );
    }

    public void setSubprocesses(
        List<ProcessInstanceEntity> processInstances,
        Map<String, Set<QueryCloudSubprocessInstance>> subprocessMap
    ) {
        processInstances.forEach(processInstance -> {
            Set<QueryCloudSubprocessInstance> subprocessSet = subprocessMap.getOrDefault(
                processInstance.getId(),
                Set.of()
            );
            processInstance.setSubprocesses(subprocessSet);
        });
    }

    public List<ProcessInstanceEntity> findSubprocessesByParentIds(List<String> parentIds) {
        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;

        JPQLQuery<ProcessInstanceEntity> subprocessQuery = queryFactory
            .selectFrom(processInstanceEntity)
            .where(processInstanceEntity.parentId.in(parentIds));

        return subprocessQuery.fetch();
    }

    public List<ProcessInstanceEntity> findSubprocessesByParentId(String parentId) {
        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;

        return queryFactory
            .selectFrom(processInstanceEntity)
            .where(processInstanceEntity.parentId.eq(parentId))
            .fetch();
    }

    @Override
    public Page<ProcessInstanceEntity> mapAllLinkedProcesses(Page<ProcessInstanceEntity> processInstances) {
        List<String> ids = processInstances.getContent().stream().map(ProcessInstanceEntity::getId).toList();

        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;
        List<ProcessInstanceEntity> allLinked = queryFactory
            .selectFrom(processInstanceEntity)
            .where(processInstanceEntity.linkedProcessInstanceId.in(ids))
            .fetch();

        Map<String, Set<QueryCloudSubprocessInstance>> linkedMap = allLinked
            .stream()
            .collect(
                Collectors.groupingBy(
                    ProcessInstanceEntity::getLinkedProcessInstanceId,
                    Collectors.mapping(this::getQueryCloudSubprocessInstance, Collectors.toSet())
                )
            );

        processInstances.getContent().forEach(pi ->
            pi.setLinkedProcesses(linkedMap.getOrDefault(pi.getId(), Set.of()))
        );

        return processInstances;
    }
}
