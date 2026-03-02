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
        List<String> parentIds = getParentIds(processInstances);

        List<ProcessInstanceEntity> subprocesses = findSubprocessesByParentIds(parentIds);

        Map<String, Set<QueryCloudSubprocessInstance>> subprocessMap = groupSubprocesses(subprocesses);

        setSubprocesses(processInstances.getContent(), subprocessMap);

        return processInstances;
    }

    @Override
    public ProcessInstanceEntity mapSubprocesses(ProcessInstanceEntity processInstance) {
        List<ProcessInstanceEntity> subprocesses = findSubprocessesByParentId(processInstance.getId());

        Map<String, Set<QueryCloudSubprocessInstance>> subprocessMap = groupSubprocesses(subprocesses);

        setSubprocesses(List.of(processInstance), subprocessMap);

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
}
