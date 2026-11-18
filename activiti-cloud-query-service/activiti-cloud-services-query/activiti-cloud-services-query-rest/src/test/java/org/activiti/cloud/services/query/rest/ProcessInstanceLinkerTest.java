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
package org.activiti.cloud.services.query.rest;

import static org.activiti.cloud.services.query.util.ProcessInstanceTestUtils.buildProcessInstanceEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceHierarchyRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.QueryEntityNotFoundException;
import org.activiti.cloud.services.query.app.specification.ProcessInstanceSpecification;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceHierarchyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessInstanceLinkerTest {

    private static final String TEST_USER = "testuser";
    private static final String MAIN_PROCESS_ID = "main-123";
    private static final String LINK_TYPE = "form-type";

    private ProcessInstanceLinker processInstanceLinker;

    @Mock
    private ProcessInstanceRepository processInstanceRepository;

    @Mock
    private ProcessInstanceHierarchyRepository processInstanceHierarchyRepository;

    @Mock
    private EntityFinder entityFinder;

    @Captor
    private ArgumentCaptor<List<ProcessInstanceHierarchyEntity>> savedHierarchyRows;

    @BeforeEach
    void setUp() {
        processInstanceLinker = new ProcessInstanceLinker(
            processInstanceRepository,
            processInstanceHierarchyRepository,
            entityFinder
        );
    }

    @Test
    void should_linkProcess_when_initiatorsMatch() {
        ProcessInstanceEntity mainProcess = mainProcess(TEST_USER);
        ProcessInstanceEntity orphan = orphan("orphan-1", TEST_USER);

        given(entityFinder.findById(any(), anyString(), anyString())).willReturn(mainProcess);
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(List.of(orphan));

        processInstanceLinker.linkProcessInstances(MAIN_PROCESS_ID, List.of("orphan-1"), LINK_TYPE, TEST_USER);

        assertThat(orphan.getLinkedProcessInstanceId()).isEqualTo(MAIN_PROCESS_ID);
        verify(processInstanceRepository).save(orphan);
    }

    @Test
    void should_linkProcess_when_only_matchingInitiators() {
        ProcessInstanceEntity mainProcess = mainProcess(TEST_USER);
        ProcessInstanceEntity orphan1 = orphan("orphan-1", TEST_USER);
        ProcessInstanceEntity orphan2 = orphan("orphan-2", "anotherUser");
        ProcessInstanceEntity orphan3 = orphan("orphan-3", TEST_USER);

        given(entityFinder.findById(any(), anyString(), anyString())).willReturn(mainProcess);
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(
            List.of(orphan1, orphan2, orphan3)
        );

        processInstanceLinker.linkProcessInstances(
            MAIN_PROCESS_ID,
            List.of("orphan-1", "orphan-2", "orphan-3"),
            LINK_TYPE,
            TEST_USER
        );

        verify(processInstanceRepository).save(orphan1);
        verify(processInstanceRepository).save(orphan3);
        verify(processInstanceRepository, never()).save(orphan2);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidProcessInstanceIds")
    void should_not_linkProcess_when_processInstanceIds_isInvalid(List<String> processInstanceIds) {
        processInstanceLinker.linkProcessInstances(MAIN_PROCESS_ID, processInstanceIds, LINK_TYPE, TEST_USER);

        verify(processInstanceRepository, never()).findAll(any(ProcessInstanceSpecification.class));
        verify(processInstanceRepository, never()).save(any(ProcessInstanceEntity.class));
        verifyNoInteractions(processInstanceHierarchyRepository);
    }

    @Test
    void should_not_linkProcess_when_orphanProcesses_notFound() {
        given(entityFinder.findById(any(), anyString(), anyString())).willReturn(mainProcess(TEST_USER));
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(
            Collections.emptyList()
        );

        processInstanceLinker.linkProcessInstances(
            MAIN_PROCESS_ID,
            List.of("orphan-1", "orphan-2"),
            LINK_TYPE,
            TEST_USER
        );

        verify(processInstanceRepository, never()).save(any(ProcessInstanceEntity.class));
        verifyNoInteractions(processInstanceHierarchyRepository);
    }

    @Test
    void should_not_linkProcess_when_initiators_dontMatch() {
        ProcessInstanceEntity orphan = orphan("orphan-1", TEST_USER);

        given(entityFinder.findById(any(), anyString(), anyString())).willReturn(mainProcess("anotherUser"));
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(List.of(orphan));

        processInstanceLinker.linkProcessInstances(MAIN_PROCESS_ID, List.of("orphan-1"), LINK_TYPE, TEST_USER);

        verify(processInstanceRepository, never()).save(orphan);
        verifyNoInteractions(processInstanceHierarchyRepository);
    }

    @Test
    void should_propagateException_when_mainProcess_notFound() {
        given(entityFinder.findById(any(), anyString(), anyString())).willThrow(
            new QueryEntityNotFoundException("not found")
        );
        List<String> processInstanceIds = List.of("orphan-1");

        assertThatThrownBy(() ->
            processInstanceLinker.linkProcessInstances(MAIN_PROCESS_ID, processInstanceIds, LINK_TYPE, TEST_USER)
        ).isInstanceOf(QueryEntityNotFoundException.class);

        verify(processInstanceRepository, never()).findAll(any(ProcessInstanceSpecification.class));
    }

    @Test
    void should_registerLinkedProcessAndItsSubtree_when_linked() {
        ProcessInstanceEntity orphan = orphan("orphan-1", TEST_USER);

        given(entityFinder.findById(any(), anyString(), anyString())).willReturn(mainProcess(TEST_USER));
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(List.of(orphan));
        given(processInstanceHierarchyRepository.findByDescendantId(MAIN_PROCESS_ID)).willReturn(
            List.of(
                new ProcessInstanceHierarchyEntity(MAIN_PROCESS_ID, MAIN_PROCESS_ID, 0, "self"),
                new ProcessInstanceHierarchyEntity("root", MAIN_PROCESS_ID, 1, "subprocess")
            )
        );
        given(
            processInstanceHierarchyRepository.findByAncestorIdInAndDepthGreaterThan(List.of("orphan-1"), -1)
        ).willReturn(
            List.of(
                new ProcessInstanceHierarchyEntity("orphan-1", "orphan-1", 0, "self"),
                new ProcessInstanceHierarchyEntity("orphan-1", "child-1", 1, "subprocess")
            )
        );

        processInstanceLinker.linkProcessInstances(MAIN_PROCESS_ID, List.of("orphan-1"), LINK_TYPE, TEST_USER);

        verify(processInstanceHierarchyRepository).saveAll(savedHierarchyRows.capture());
        assertThat(savedHierarchyRows.getValue())
            .extracting(
                ProcessInstanceHierarchyEntity::getAncestorId,
                ProcessInstanceHierarchyEntity::getDescendantId,
                ProcessInstanceHierarchyEntity::getDepth,
                ProcessInstanceHierarchyEntity::getRelationType
            )
            .containsExactlyInAnyOrder(
                tuple(MAIN_PROCESS_ID, "orphan-1", 1, "linked"),
                tuple(MAIN_PROCESS_ID, "child-1", 2, "subprocess"),
                tuple("root", "orphan-1", 2, "linked"),
                tuple("root", "child-1", 3, "subprocess")
            );
    }

    @Test
    void should_notRegisterAgain_when_hierarchyRowsAlreadyExist() {
        ProcessInstanceEntity orphan = orphan("orphan-1", TEST_USER);
        ProcessInstanceHierarchyEntity existing = new ProcessInstanceHierarchyEntity(
            MAIN_PROCESS_ID,
            "orphan-1",
            1,
            "linked"
        );

        given(entityFinder.findById(any(), anyString(), anyString())).willReturn(mainProcess(TEST_USER));
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(List.of(orphan));
        given(processInstanceHierarchyRepository.findByDescendantId(MAIN_PROCESS_ID)).willReturn(
            List.of(new ProcessInstanceHierarchyEntity(MAIN_PROCESS_ID, MAIN_PROCESS_ID, 0, "self"))
        );
        given(
            processInstanceHierarchyRepository.findByAncestorIdInAndDepthGreaterThan(List.of("orphan-1"), -1)
        ).willReturn(List.of(new ProcessInstanceHierarchyEntity("orphan-1", "orphan-1", 0, "self")));
        given(processInstanceHierarchyRepository.findAllById(any())).willReturn(List.of(existing));

        processInstanceLinker.linkProcessInstances(MAIN_PROCESS_ID, List.of("orphan-1"), LINK_TYPE, TEST_USER);

        verify(processInstanceHierarchyRepository).saveAll(savedHierarchyRows.capture());
        assertThat(savedHierarchyRows.getValue()).isEmpty();
    }

    private static Stream<Arguments> provideInvalidProcessInstanceIds() {
        return Stream.of(Arguments.of((List<String>) null), Arguments.of(Collections.emptyList()));
    }

    private static ProcessInstanceEntity mainProcess(String initiator) {
        ProcessInstanceEntity mainProcess = buildProcessInstanceEntity();
        mainProcess.setId(MAIN_PROCESS_ID);
        mainProcess.setInitiator(initiator);
        return mainProcess;
    }

    private static ProcessInstanceEntity orphan(String id, String initiator) {
        ProcessInstanceEntity orphan = buildProcessInstanceEntity();
        orphan.setId(id);
        orphan.setInitiator(initiator);
        orphan.setLinkedProcessInstanceType(LINK_TYPE);
        return orphan;
    }
}
