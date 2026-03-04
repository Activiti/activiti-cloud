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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CustomizedProcessInstanceRepositoryImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private JPAQueryFactory queryFactory;

    @Mock
    private JPAQuery<ProcessInstanceEntity> jpaQuery;

    private CustomizedProcessInstanceRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new CustomizedProcessInstanceRepositoryImpl(entityManager);
        ReflectionTestUtils.setField(repository, "queryFactory", queryFactory);
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

        Map<String, Set<QueryCloudSubprocessInstance>> result = repository.groupSubprocesses(subprocessesList);

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

        verify(queryFactory).selectFrom(processInstanceEntity);
        verify(jpaQuery).where(processInstanceEntity.parentId.eq(parentId));
        verify(jpaQuery).fetch();
    }

    @Test
    void testFindSubprocessesByParentIds() {
        List<String> parentIds = Arrays.asList("parent1", "parent2");

        List<ProcessInstanceEntity> expectedSubprocesses = createSubprocessInstances(2, "parent1");
        expectedSubprocesses.addAll(createSubprocessInstances(3, "parent2"));

        QProcessInstanceEntity processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;

        when(queryFactory.selectFrom(processInstanceEntity)).thenReturn(jpaQuery);
        when(jpaQuery.where(processInstanceEntity.parentId.in(parentIds))).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(expectedSubprocesses);

        List<ProcessInstanceEntity> result = repository.findSubprocessesByParentIds(parentIds);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(5);

        verify(queryFactory).selectFrom(processInstanceEntity);
        verify(jpaQuery).where(processInstanceEntity.parentId.in(parentIds));
        verify(jpaQuery).fetch();
    }

    @Test
    void testBuildChildrenByParentIdMap_groupsDirectChildrenCorrectly() {
        String parentId = UUID.randomUUID().toString();
        List<ProcessInstanceEntity> children = createSubprocessInstances(3, parentId);

        Map<String, Set<QueryCloudSubprocessInstance>> result = repository.buildChildrenByParentIdMap(children);

        assertThat(result).hasSize(1);
        assertThat(result.get(parentId)).hasSize(3);
    }

    @Test
    void testBuildChildrenByParentIdMap_ignoresEntriesWithNullParentId() {
        ProcessInstanceEntity noParent = createProcessInstance("1"); // parentId set to null by helper
        ProcessInstanceEntity withParent = createProcessInstance(UUID.randomUUID().toString());

        Map<String, Set<QueryCloudSubprocessInstance>> result = repository.buildChildrenByParentIdMap(
            List.of(noParent, withParent)
        );

        assertThat(result).doesNotContainKey(null);
    }

    @Test
    void testCollectAllSubprocesses_returnsEmptyWhenNoChildren() {
        Set<QueryCloudSubprocessInstance> result = repository.collectAllSubprocesses("root", Map.of());
        assertThat(result).isEmpty();
    }

    @Test
    void testCollectAllSubprocesses_returnsDeepDescendants() {
        String rootId = "root";
        String child1Id = "child1";
        String child2Id = "child2";
        String grandchild1Id = "gc1";
        String grandchild2Id = "gc2";

        QueryCloudSubprocessInstance child1 = subprocessRef(child1Id);
        QueryCloudSubprocessInstance child2 = subprocessRef(child2Id);
        QueryCloudSubprocessInstance gc1 = subprocessRef(grandchild1Id);
        QueryCloudSubprocessInstance gc2 = subprocessRef(grandchild2Id);

        Map<String, Set<QueryCloudSubprocessInstance>> childrenMap = Map.of(
            rootId, Set.of(child1, child2),
            child1Id, Set.of(gc1, gc2)
        );

        Set<QueryCloudSubprocessInstance> result = repository.collectAllSubprocesses(rootId, childrenMap);

        assertThat(result).hasSize(4)
            .extracting(QueryCloudSubprocessInstance::getId)
            .containsExactlyInAnyOrder(child1Id, child2Id, grandchild1Id, grandchild2Id);
    }


    @Test
    void testMapSubprocesses_fetchesAllLevelsInOneBulkQuery() {
        String rootId = UUID.randomUUID().toString();

        // Two top-level process instances sharing the same root
        ProcessInstanceEntity parent1 = createParentWithRoot(rootId);
        ProcessInstanceEntity parent2 = createParentWithRoot(rootId);

        // Direct children of parent1
        ProcessInstanceEntity child1 = createSubprocessOf(parent1.getId(), rootId);
        ProcessInstanceEntity child2 = createSubprocessOf(parent1.getId(), rootId);
        // Grandchild (child of child1)
        ProcessInstanceEntity grandchild = createSubprocessOf(child1.getId(), rootId);
        // Direct child of parent2
        ProcessInstanceEntity child3 = createSubprocessOf(parent2.getId(), rootId);

        Page<ProcessInstanceEntity> processInstances = new PageImpl<>(List.of(parent1, parent2));
        Set<String> pageIds = Set.of(parent1.getId(), parent2.getId());

        QProcessInstanceEntity q = QProcessInstanceEntity.processInstanceEntity;
        when(queryFactory.selectFrom(q)).thenReturn(jpaQuery);
        when(jpaQuery.where(q.rootProcessInstanceId.in(List.of(rootId)).and(q.id.notIn(pageIds))))
            .thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(List.of(child1, child2, grandchild, child3));

        Page<ProcessInstanceEntity> result = repository.mapSubprocesses(processInstances);

        // parent1 should have child1, child2, and grandchild (all 3 levels)
        assertThat(result.getContent().get(0).getSubprocesses())
            .hasSize(3)
            .extracting(QueryCloudSubprocessInstance::getId)
            .containsExactlyInAnyOrder(child1.getId(), child2.getId(), grandchild.getId());

        // parent2 should have only child3
        assertThat(result.getContent().get(1).getSubprocesses())
            .hasSize(1)
            .extracting(QueryCloudSubprocessInstance::getId)
            .containsExactly(child3.getId());

        // Only ONE database query should have been issued
        verify(queryFactory, times(1)).selectFrom(q);
    }

    @Test
    void testMapSubprocessesForProcessInstance_returnsDeepSubprocesses() {
        String rootId = UUID.randomUUID().toString();
        ProcessInstanceEntity entity = createParentWithRoot(rootId);

        ProcessInstanceEntity child = createSubprocessOf(entity.getId(), rootId);
        ProcessInstanceEntity grandchild = createSubprocessOf(child.getId(), rootId);

        QProcessInstanceEntity q = QProcessInstanceEntity.processInstanceEntity;
        when(queryFactory.selectFrom(q)).thenReturn(jpaQuery);
        when(jpaQuery.where(q.rootProcessInstanceId.in(List.of(rootId)).and(q.id.notIn(Set.of(entity.getId())))))
            .thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(List.of(child, grandchild));

        ProcessInstanceEntity result = repository.mapSubprocesses(entity);

        assertThat(result.getSubprocesses())
            .hasSize(2)
            .extracting(QueryCloudSubprocessInstance::getId)
            .containsExactlyInAnyOrder(child.getId(), grandchild.getId());
    }

    @Test
    void testMapSubprocessesForProcessInstance_nullRootId_returnsEmpty() {
        ProcessInstanceEntity entity = new ProcessInstanceEntity();
        entity.setId(UUID.randomUUID().toString());

        ProcessInstanceEntity result = repository.mapSubprocesses(entity);

        assertThat(result.getSubprocesses()).isEmpty();
        verifyNoInteractions(queryFactory);
    }


    @Test
    void testMapAllLinkedProcesses() {
        List<ProcessInstanceEntity> parents = createParentProcessInstances(2);
        String parentId1 = parents.get(0).getId();
        String parentId2 = parents.get(1).getId();
        Page<ProcessInstanceEntity> processInstances = new PageImpl<>(parents);

        ProcessInstanceEntity linked1 = createProcessInstance(UUID.randomUUID().toString());
        linked1.setLinkedProcessInstanceId(parentId1);
        ProcessInstanceEntity linked2 = createProcessInstance(UUID.randomUUID().toString());
        linked2.setLinkedProcessInstanceId(parentId1);
        ProcessInstanceEntity linked3 = createProcessInstance(UUID.randomUUID().toString());
        linked3.setLinkedProcessInstanceId(parentId2);

        QProcessInstanceEntity q = QProcessInstanceEntity.processInstanceEntity;
        when(queryFactory.selectFrom(q)).thenReturn(jpaQuery);
        when(jpaQuery.where(q.linkedProcessInstanceId.in(List.of(parentId1, parentId2)))).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(List.of(linked1, linked2, linked3));

        Page<ProcessInstanceEntity> result = repository.mapAllLinkedProcesses(processInstances);

        assertThat(result.getContent().get(0).getLinkedProcesses()).hasSize(2);
        assertThat(result.getContent().get(1).getLinkedProcesses()).hasSize(1);
    }

    private static QueryCloudSubprocessInstance subprocessRef(String id) {
        QueryCloudSubprocessInstance ref = new QueryCloudSubprocessInstance();
        ref.setId(id);
        return ref;
    }

    private static ProcessInstanceEntity createParentWithRoot(String rootId) {
        ProcessInstanceEntity e = new ProcessInstanceEntity();
        e.setId(UUID.randomUUID().toString());
        e.setProcessDefinitionName("process-definition");
        e.setRootProcessInstanceId(rootId);
        return e;
    }

    private static ProcessInstanceEntity createSubprocessOf(String parentId, String rootId) {
        ProcessInstanceEntity e = new ProcessInstanceEntity();
        e.setId(UUID.randomUUID().toString());
        e.setProcessDefinitionName("subprocess-definition");
        e.setParentId(parentId);
        e.setRootProcessInstanceId(rootId);
        return e;
    }
}
