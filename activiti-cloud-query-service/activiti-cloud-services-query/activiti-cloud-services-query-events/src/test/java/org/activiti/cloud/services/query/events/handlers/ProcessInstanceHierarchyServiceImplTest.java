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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import java.util.List;
import org.activiti.cloud.services.query.model.ProcessInstanceHierarchyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessInstanceHierarchyServiceImplTest {

    @Mock
    private EntityManager entityManager;

    private ProcessInstanceHierarchyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProcessInstanceHierarchyServiceImpl(entityManager);
        given(entityManager.find(eq(ProcessInstanceHierarchyEntity.class), any())).willReturn(null);
    }

    /** Builds a stub for findByDescendantId to return the given rows. */
    @SuppressWarnings("unchecked")
    private void stubFindByDescendantId(List<ProcessInstanceHierarchyEntity> rows) {
        CriteriaBuilder cb = Mockito.mock(CriteriaBuilder.class);
        CriteriaQuery<ProcessInstanceHierarchyEntity> cq = Mockito.mock(CriteriaQuery.class);
        TypedQuery<ProcessInstanceHierarchyEntity> tq = Mockito.mock(TypedQuery.class);
        given(entityManager.getCriteriaBuilder()).willReturn(cb);
        given(cb.createQuery(ProcessInstanceHierarchyEntity.class)).willReturn(cq);
        given(cq.from(ProcessInstanceHierarchyEntity.class))
            .willReturn(Mockito.mock(jakarta.persistence.criteria.Root.class));
        given(entityManager.createQuery(cq)).willReturn(tq);
        given(tq.getResultList()).willReturn(rows);
    }

    @Test
    void registerProcess_shouldInsertSelfRow() {
        service.registerProcess("A");

        ArgumentCaptor<ProcessInstanceHierarchyEntity> captor = ArgumentCaptor.forClass(
            ProcessInstanceHierarchyEntity.class
        );
        verify(entityManager).persist(captor.capture());

        ProcessInstanceHierarchyEntity row = captor.getValue();
        assertThat(row.getAncestorId()).isEqualTo("A");
        assertThat(row.getDescendantId()).isEqualTo("A");
        assertThat(row.getDepth()).isZero();
        assertThat(row.getRelationType()).isEqualTo(ProcessInstanceHierarchyEntity.RELATION_SELF);
    }

    @Test
    void registerSubprocess_shouldInsertSelfRowAndSubprocessEdge() {
        ProcessInstanceHierarchyEntity parentSelfRow = new ProcessInstanceHierarchyEntity(
            "parent",
            "parent",
            0,
            ProcessInstanceHierarchyEntity.RELATION_SELF
        );
        stubFindByDescendantId(List.of(parentSelfRow));

        service.registerSubprocess("child", "parent");

        ArgumentCaptor<ProcessInstanceHierarchyEntity> captor = ArgumentCaptor.forClass(
            ProcessInstanceHierarchyEntity.class
        );
        verify(entityManager, Mockito.times(2)).persist(captor.capture());

        List<ProcessInstanceHierarchyEntity> persisted = captor.getAllValues();

        ProcessInstanceHierarchyEntity selfRow = persisted
            .stream()
            .filter(r -> r.getAncestorId().equals("child") && r.getDescendantId().equals("child"))
            .findFirst()
            .orElseThrow();
        assertThat(selfRow.getDepth()).isZero();
        assertThat(selfRow.getRelationType()).isEqualTo(ProcessInstanceHierarchyEntity.RELATION_SELF);

        ProcessInstanceHierarchyEntity edge = persisted
            .stream()
            .filter(r -> r.getAncestorId().equals("parent") && r.getDescendantId().equals("child"))
            .findFirst()
            .orElseThrow();
        assertThat(edge.getDepth()).isEqualTo(1);
        // Regression guard: must be SUBPROCESS, not "self"
        assertThat(edge.getRelationType()).isEqualTo(ProcessInstanceHierarchyEntity.RELATION_SUBPROCESS);
    }

    @Test
    void registerSubprocess_transitiveAncestorEdgeMustAlsoBeSubprocess() {
        // Chain: grandparent → parent → child
        // grandparent has a direct subprocess edge to parent already
        ProcessInstanceHierarchyEntity grandparentSelf = new ProcessInstanceHierarchyEntity(
            "grandparent",
            "grandparent",
            0,
            ProcessInstanceHierarchyEntity.RELATION_SELF
        );
        ProcessInstanceHierarchyEntity grandparentToParent = new ProcessInstanceHierarchyEntity(
            "grandparent",
            "parent",
            1,
            ProcessInstanceHierarchyEntity.RELATION_SUBPROCESS
        );
        ProcessInstanceHierarchyEntity parentSelf = new ProcessInstanceHierarchyEntity(
            "parent",
            "parent",
            0,
            ProcessInstanceHierarchyEntity.RELATION_SELF
        );
        stubFindByDescendantId(List.of(grandparentSelf, grandparentToParent, parentSelf));

        service.registerSubprocess("child", "parent");

        ArgumentCaptor<ProcessInstanceHierarchyEntity> captor = ArgumentCaptor.forClass(
            ProcessInstanceHierarchyEntity.class
        );
        verify(entityManager, Mockito.times(4)).persist(captor.capture());

        captor
            .getAllValues()
            .stream()
            .filter(r -> !r.getAncestorId().equals(r.getDescendantId())) // exclude self-rows
            .forEach(r ->
                assertThat(r.getRelationType())
                    .as("Edge (%s→%s) must be SUBPROCESS", r.getAncestorId(), r.getDescendantId())
                    .isEqualTo(ProcessInstanceHierarchyEntity.RELATION_SUBPROCESS)
            );
    }

    @Test
    void registerLinkedProcess_shouldInsertSelfRowAndLinkedEdge() {
        ProcessInstanceHierarchyEntity mainSelfRow = new ProcessInstanceHierarchyEntity(
            "main",
            "main",
            0,
            ProcessInstanceHierarchyEntity.RELATION_SELF
        );
        stubFindByDescendantId(List.of(mainSelfRow));

        service.registerLinkedProcess("linked", "main");

        ArgumentCaptor<ProcessInstanceHierarchyEntity> captor = ArgumentCaptor.forClass(
            ProcessInstanceHierarchyEntity.class
        );
        verify(entityManager, Mockito.times(2)).persist(captor.capture());

        ProcessInstanceHierarchyEntity edge = captor
            .getAllValues()
            .stream()
            .filter(r -> r.getAncestorId().equals("main") && r.getDescendantId().equals("linked"))
            .findFirst()
            .orElseThrow();
        assertThat(edge.getDepth()).isEqualTo(1);
        assertThat(edge.getRelationType()).isEqualTo(ProcessInstanceHierarchyEntity.RELATION_LINKED);
    }
}
