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

    public CustomizedProcessInstanceRepositoryImpl() {
        super(ProcessInstanceEntity.class);
    }

    @Override
    public Page<ProcessInstanceEntity> mapSubprocesses(
        Page<ProcessInstanceEntity> processInstances,
        Pageable pageable
    ) {
        List<String> parentIds = processInstances
            .getContent()
            .stream()
            .map(ProcessInstanceEntity::getId)
            .collect(Collectors.toList());

        Page<ProcessInstanceEntity> subprocesses = findSubprocessesByParentIds(parentIds, pageable);

        Map<String, Set<QueryCloudSubprocessInstance>> subprocessMap = subprocesses
            .getContent()
            .stream()
            .collect(
                Collectors.groupingBy(
                    ProcessInstanceEntity::getParentId,
                    Collectors.mapping(
                        subprocess -> {
                            QueryCloudSubprocessInstance subProcessInstance = new QueryCloudSubprocessInstance();
                            subProcessInstance.setId(subprocess.getId());
                            subProcessInstance.setProcessDefinitionName(subprocess.getProcessDefinitionName());
                            return subProcessInstance;
                        },
                        Collectors.toSet()
                    )
                )
            );

        processInstances
            .getContent()
            .forEach(processInstance -> {
                Set<QueryCloudSubprocessInstance> subprocessSet = subprocessMap.getOrDefault(
                    processInstance.getId(),
                    Set.of()
                );
                processInstance.setSubprocesses(subprocessSet);
            });

        return processInstances;
    }

    @Override
    public ProcessInstanceEntity mapSubprocesses(ProcessInstanceEntity processInstance) {
        List<ProcessInstanceEntity> subprocesses = findSubprocessesByParentId(processInstance.getId());

        Set<QueryCloudSubprocessInstance> subprocessSet = subprocesses
            .stream()
            .map(subprocess -> {
                QueryCloudSubprocessInstance subProcessInstance = new QueryCloudSubprocessInstance();
                subProcessInstance.setId(subprocess.getId());
                subProcessInstance.setProcessDefinitionName(subprocess.getProcessDefinitionName());
                return subProcessInstance;
            })
            .collect(Collectors.toSet());

        processInstance.setSubprocesses(subprocessSet);

        return processInstance;
    }

    private Page<ProcessInstanceEntity> findSubprocessesByParentIds(List<String> parentIds, Pageable pageable) {
        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;

        EntityManager entityManager = getEntityManager();
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        Querydsl querydsl = getQuerydsl();

        JPQLQuery<ProcessInstanceEntity> subprocessQuery = queryFactory
            .selectFrom(processInstanceEntity)
            .where(processInstanceEntity.parentId.in(parentIds));

        long totalElements = subprocessQuery.fetchCount();

        List<ProcessInstanceEntity> subprocesses = querydsl.applyPagination(pageable, subprocessQuery).fetch();

        return PageableExecutionUtils.getPage(subprocesses, pageable, () -> totalElements);
    }

    private List<ProcessInstanceEntity> findSubprocessesByParentId(String parentId) {
        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;

        EntityManager entityManager = getEntityManager();
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        return queryFactory
            .selectFrom(processInstanceEntity)
            .where(processInstanceEntity.parentId.eq(parentId))
            .fetch();
    }
}
