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

import static org.activiti.cloud.services.query.app.repository.utils.ProcessInstanceHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.activiti.cloud.api.process.model.QueryCloudSubprocessInstance;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QProcessInstanceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CustomizedProcessInstanceRepositoryImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private JPAQueryFactory queryFactory;

    @Mock
    private JPAQuery<ProcessInstanceEntity> jpaQuery;

    @Mock
    private Querydsl querydsl;

    private CustomizedProcessInstanceRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new CustomizedProcessInstanceRepositoryImpl(entityManager);
        ReflectionTestUtils.setField(repository, "queryFactory", queryFactory);
        ReflectionTestUtils.setField(repository, "querydsl", querydsl);
    }

    @Test
    void testGetQueryCloudSubprocessInstance() {
        ProcessInstanceEntity subprocess = createProcessInstance(UUID.randomUUID().toString());
        QueryCloudSubprocessInstance result = repository.getQueryCloudSubprocessInstance(subprocess);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getProcessDefinitionName()).isNotNull();
    }

    @Test
    void testGetParentIds() {
        List<ProcessInstanceEntity> processInstancesList = createParentProcessInstances(3);
        Page<ProcessInstanceEntity> processInstances = new PageImpl<>(processInstancesList);

        List<String> result = repository.getParentIds(processInstances);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
    }

    @Test
    void testGroupSubprocesses() {
        List<ProcessInstanceEntity> processInstancesList = createParentProcessInstances(4);
        String parentIdOne = processInstancesList.getFirst().getId();
        String parentIdTwo = processInstancesList.getLast().getId();
        List<ProcessInstanceEntity> subprocessesList = createSubprocessInstances(2, parentIdOne);
        subprocessesList.addAll(createSubprocessInstances(3, parentIdTwo));

        Page<ProcessInstanceEntity> subprocesses = new PageImpl<>(subprocessesList);

        Map<String, Set<QueryCloudSubprocessInstance>> result = repository.groupSubprocesses(subprocesses);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsKey(parentIdOne);
        assertThat(result).containsKey(parentIdTwo);
        assertThat(result.get(parentIdOne)).hasSize(2);
        assertThat(result.get(parentIdTwo)).hasSize(3);
    }

    @Test
    void testFindSubprocessesByParentId() {
        String parentId = UUID.randomUUID().toString();
        List<ProcessInstanceEntity> expectedSubprocesses = createSubprocessInstances(2, parentId);

        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;

        when(queryFactory.selectFrom(processInstanceEntity)).thenReturn(jpaQuery);
        when(jpaQuery.where(processInstanceEntity.parentId.eq(parentId))).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(expectedSubprocesses);

        List<ProcessInstanceEntity> result = repository.findSubprocessesByParentId(parentId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isNotNull();
        assertThat(result.get(1).getId()).isNotNull();

        verify(queryFactory).selectFrom(processInstanceEntity);
        verify(jpaQuery).where(processInstanceEntity.parentId.eq(parentId));
        verify(jpaQuery).fetch();
    }

    @Test
    void testFindSubprocessesByParentIds() {
        List<String> parentIds = Arrays.asList("parent1", "parent2");
        Pageable pageable = PageRequest.of(0, 10);

        List<ProcessInstanceEntity> expectedSubprocesses = createSubprocessInstances(2, "parent1");
        expectedSubprocesses.addAll(createSubprocessInstances(3, "parent2"));

        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;

        when(queryFactory.selectFrom(processInstanceEntity)).thenReturn(jpaQuery);
        when(jpaQuery.where(processInstanceEntity.parentId.in(parentIds))).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(expectedSubprocesses);
        when(querydsl.applyPagination(pageable, jpaQuery)).thenReturn(jpaQuery);

        Page<ProcessInstanceEntity> result = repository.findSubprocessesByParentIds(parentIds, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getContent().get(0).getId()).isNotNull();
        assertThat(result.getContent().get(1).getId()).isNotNull();

        verify(queryFactory).selectFrom(processInstanceEntity);
        verify(jpaQuery).where(processInstanceEntity.parentId.in(parentIds));
        verify(jpaQuery).fetch();
    }

    @Test
    void testMapSubprocesses() {
        List<ProcessInstanceEntity> processInstancesList = createParentProcessInstances(2);
        List<String> parentIds = Arrays.asList(
            processInstancesList.getFirst().getId(),
            processInstancesList.getLast().getId()
        );
        Page<ProcessInstanceEntity> processInstances = new PageImpl<>(processInstancesList);
        Pageable pageable = PageRequest.of(0, 10);

        List<ProcessInstanceEntity> subprocessesList = createSubprocessInstances(
            2,
            processInstancesList.get(0).getId()
        );
        subprocessesList.addAll(createSubprocessInstances(3, processInstancesList.get(1).getId()));

        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;

        when(queryFactory.selectFrom(processInstanceEntity)).thenReturn(jpaQuery);
        when(jpaQuery.where(processInstanceEntity.parentId.in(parentIds))).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(subprocessesList);
        when(querydsl.applyPagination(pageable, jpaQuery)).thenReturn(jpaQuery);

        Page<ProcessInstanceEntity> result = repository.mapSubprocesses(processInstances, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getSubprocesses()).isNotNull();
        assertThat(result.getContent().get(1).getSubprocesses()).isNotNull();
    }

    @Test
    void testMapSubprocessesForProcessInstance() {
        ProcessInstanceEntity entity = createProcessInstance("1");
        String parentId = entity.getId();
        List<ProcessInstanceEntity> expectedSubprocesses = createSubprocessInstances(2, parentId);

        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;

        when(queryFactory.selectFrom(processInstanceEntity)).thenReturn(jpaQuery);
        when(jpaQuery.where(processInstanceEntity.parentId.eq(parentId))).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(expectedSubprocesses);

        ProcessInstanceEntity result = repository.mapSubprocesses(entity);

        assertThat(result).isNotNull();
        assertThat(result.getSubprocesses()).hasSize(2);
    }
}
