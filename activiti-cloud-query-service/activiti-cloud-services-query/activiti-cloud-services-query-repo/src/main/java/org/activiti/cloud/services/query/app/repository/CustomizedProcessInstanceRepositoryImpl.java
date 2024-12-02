/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;

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
        List<String> parentIds = getParentIds(processInstances);

        Page<ProcessInstanceEntity> subprocesses = findSubprocessesByParentIds(parentIds, pageable);

        Map<String, Set<QueryCloudSubprocessInstance>> subprocessMap = groupSubprocesses(subprocesses);

        setSubprocesses(processInstances, subprocessMap);

        return processInstances;
    }

    @Override
    public ProcessInstanceEntity mapSubprocesses(ProcessInstanceEntity processInstance) {
        List<ProcessInstanceEntity> subprocesses = findSubprocessesByParentId(processInstance.getId());

        if (subprocesses == null || subprocesses.isEmpty()) {
            processInstance.setSubprocesses(new HashSet<>());
            return processInstance;
        }

        Set<QueryCloudSubprocessInstance> subprocessSet = subprocesses
            .stream()
            .map(this::getQueryCloudSubprocessInstance)
            .collect(Collectors.toSet());

        processInstance.setSubprocesses(subprocessSet);

        return processInstance;
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

    public Map<String, Set<QueryCloudSubprocessInstance>> groupSubprocesses(Page<ProcessInstanceEntity> subprocesses) {
        return subprocesses
            .getContent()
            .stream()
            .collect(
                Collectors.groupingBy(
                    ProcessInstanceEntity::getParentId,
                    Collectors.mapping(this::getQueryCloudSubprocessInstance, Collectors.toSet())
                )
            );
    }

    public void setSubprocesses(
        Page<ProcessInstanceEntity> processInstances,
        Map<String, Set<QueryCloudSubprocessInstance>> subprocessMap
    ) {
        processInstances
            .getContent()
            .forEach(processInstance -> {
                Set<QueryCloudSubprocessInstance> subprocessSet = subprocessMap.getOrDefault(
                    processInstance.getId(),
                    Set.of()
                );
                processInstance.setSubprocesses(subprocessSet);
            });
    }

    public Page<ProcessInstanceEntity> findSubprocessesByParentIds(List<String> parentIds, Pageable pageable) {
        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;

        Querydsl querydsl = getQuerydsl();

        JPQLQuery<ProcessInstanceEntity> subprocessQuery = queryFactory
            .selectFrom(processInstanceEntity)
            .where(processInstanceEntity.parentId.in(parentIds));

        long totalElements = subprocessQuery.fetchCount();

        assert querydsl != null;
        List<ProcessInstanceEntity> subprocesses = querydsl.applyPagination(pageable, subprocessQuery).fetch();

        return PageableExecutionUtils.getPage(subprocesses, pageable, () -> totalElements);
    }

    public List<ProcessInstanceEntity> findSubprocessesByParentId(String parentId) {
        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;

        return queryFactory
            .selectFrom(processInstanceEntity)
            .where(processInstanceEntity.parentId.eq(parentId))
            .fetch();
    }
}
