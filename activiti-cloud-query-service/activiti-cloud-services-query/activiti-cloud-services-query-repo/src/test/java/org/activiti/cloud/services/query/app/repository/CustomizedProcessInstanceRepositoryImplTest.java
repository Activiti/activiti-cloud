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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.activiti.cloud.api.process.model.QueryCloudSubprocessInstance;
import org.activiti.cloud.services.query.app.repository.utils.ProcessInstanceHelper;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QProcessInstanceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.test.util.ReflectionTestUtils;

class CustomizedProcessInstanceRepositoryImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private JPAQueryFactory queryFactory;

    @Mock
    private JPAQuery<ProcessInstanceEntity> jpaQuery;

    @Mock
    private JPQLQuery<ProcessInstanceEntity> jpqlQuery;

    @Mock
    private Querydsl querydsl;

    private CustomizedProcessInstanceRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new CustomizedProcessInstanceRepositoryImpl(entityManager);
        ReflectionTestUtils.setField(repository, "queryFactory", queryFactory);
        ReflectionTestUtils.setField(repository, "querydsl", querydsl);
    }

    @Test
    void testGetQueryCloudSubprocessInstance() {
        ProcessInstanceEntity subprocess = new ProcessInstanceHelper()
            .createProcessInstance(UUID.randomUUID().toString());
        QueryCloudSubprocessInstance result = repository.getQueryCloudSubprocessInstance(subprocess);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertNotNull(result.getProcessDefinitionName());
    }

    @Test
    void testGetParentIds() {
        List<ProcessInstanceEntity> processInstancesList = new ProcessInstanceHelper().createParentProcessInstances(3);
        Page<ProcessInstanceEntity> processInstances = new PageImpl<>(processInstancesList);

        List<String> result = repository.getParentIds(processInstances);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void testGroupSubprocesses() {
        List<ProcessInstanceEntity> processInstancesList = new ProcessInstanceHelper().createParentProcessInstances(4);
        String parentIdOne = processInstancesList.getFirst().getId();
        String parentIdTwo = processInstancesList.getLast().getId();
        List<ProcessInstanceEntity> subprocessesList = new ProcessInstanceHelper()
            .createSubprocessInstances(2, parentIdOne);
        subprocessesList.addAll(new ProcessInstanceHelper().createSubprocessInstances(3, parentIdTwo));

        Page<ProcessInstanceEntity> subprocesses = new PageImpl<>(subprocessesList);

        Map<String, Set<QueryCloudSubprocessInstance>> result = repository.groupSubprocesses(subprocesses);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey(parentIdOne));
        assertTrue(result.containsKey(parentIdTwo));
        assertEquals(2, result.get(parentIdOne).size());
        assertEquals(3, result.get(parentIdTwo).size());
    }

    @Test
    void testFindSubprocessesByParentId() {
        String parentId = UUID.randomUUID().toString();
        List<ProcessInstanceEntity> expectedSubprocesses = new ProcessInstanceHelper()
            .createSubprocessInstances(2, parentId);

        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;

        when(queryFactory.selectFrom(processInstanceEntity)).thenReturn(jpaQuery);
        when(jpaQuery.where(processInstanceEntity.parentId.eq(parentId))).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(expectedSubprocesses);

        List<ProcessInstanceEntity> result = repository.findSubprocessesByParentId(parentId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertNotNull(result.get(0).getId());
        assertNotNull(result.get(1).getId());

        verify(queryFactory).selectFrom(processInstanceEntity);
        verify(jpaQuery).where(processInstanceEntity.parentId.eq(parentId));
        verify(jpaQuery).fetch();
    }

    @Test
    void testFindSubprocessesByParentIds() {
        List<String> parentIds = Arrays.asList("parent1", "parent2");
        Pageable pageable = PageRequest.of(0, 10);

        List<ProcessInstanceEntity> expectedSubprocesses = new ProcessInstanceHelper()
            .createSubprocessInstances(2, "parent1");
        expectedSubprocesses.addAll(new ProcessInstanceHelper().createSubprocessInstances(3, "parent2"));

        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;

        when(queryFactory.selectFrom(processInstanceEntity)).thenReturn(jpaQuery);
        when(jpaQuery.where(processInstanceEntity.parentId.in(parentIds))).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(expectedSubprocesses);
        when(querydsl.applyPagination(pageable, jpaQuery)).thenReturn(jpaQuery);

        Page<ProcessInstanceEntity> result = repository.findSubprocessesByParentIds(parentIds, pageable);

        assertNotNull(result);
        assertEquals(5, result.getTotalElements());
        assertEquals(5, result.getContent().size());
        assertNotNull(result.getContent().get(0).getId());
        assertNotNull(result.getContent().get(1).getId());

        verify(queryFactory).selectFrom(processInstanceEntity);
        verify(jpaQuery).where(processInstanceEntity.parentId.in(parentIds));
        verify(jpaQuery).fetch();
    }

    @Test
    void testMapSubprocesses() {
        List<ProcessInstanceEntity> processInstancesList = new ProcessInstanceHelper().createParentProcessInstances(2);
        List<String> parentIds = Arrays.asList(
            processInstancesList.getFirst().getId(),
            processInstancesList.getLast().getId()
        );
        Page<ProcessInstanceEntity> processInstances = new PageImpl<>(processInstancesList);
        Pageable pageable = PageRequest.of(0, 10);

        List<ProcessInstanceEntity> subprocessesList = new ProcessInstanceHelper()
            .createSubprocessInstances(2, processInstancesList.get(0).getId());
        subprocessesList.addAll(
            new ProcessInstanceHelper().createSubprocessInstances(3, processInstancesList.get(1).getId())
        );

        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;

        when(queryFactory.selectFrom(processInstanceEntity)).thenReturn(jpaQuery);
        when(jpaQuery.where(processInstanceEntity.parentId.in(parentIds))).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(subprocessesList);
        when(querydsl.applyPagination(pageable, jpaQuery)).thenReturn(jpaQuery);

        Page<ProcessInstanceEntity> result = repository.mapSubprocesses(processInstances, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertNotNull(result.getContent().get(0).getSubprocesses());
        assertNotNull(result.getContent().get(1).getSubprocesses());
    }

    @Test
    void testMapSubprocessesForProcessInstance() {
        ProcessInstanceEntity entity = new ProcessInstanceHelper().createProcessInstance("1");
        String parentId = entity.getId();
        List<ProcessInstanceEntity> expectedSubprocesses = new ProcessInstanceHelper()
            .createSubprocessInstances(2, parentId);

        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;

        when(queryFactory.selectFrom(processInstanceEntity)).thenReturn(jpaQuery);
        when(jpaQuery.where(processInstanceEntity.parentId.eq(parentId))).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(expectedSubprocesses);

        ProcessInstanceEntity result = repository.mapSubprocesses(entity);

        assertNotNull(result);
        assertEquals(2, result.getSubprocesses().size());
    }
}
