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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceHierarchyRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
    private PlatformTransactionManager transactionManager;

    @Mock
    private ThreadPoolTaskScheduler retryScheduler;

    @Captor
    private ArgumentCaptor<List<ProcessInstanceHierarchyEntity>> savedHierarchyRows;

    @BeforeEach
    void setUp() {
        processInstanceLinker = new ProcessInstanceLinker(
            processInstanceRepository,
            processInstanceHierarchyRepository,
            new TransactionTemplate(transactionManager),
            3,
            2,
            retryScheduler
        );
    }

    @Test
    void should_linkProcess_when_initiatorsMatch() {
        ProcessInstanceEntity mainProcess = mainProcess(TEST_USER);
        ProcessInstanceEntity orphan = orphan("orphan-1", TEST_USER);

        given(processInstanceRepository.findById(MAIN_PROCESS_ID)).willReturn(Optional.of(mainProcess));
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(List.of(orphan));

        processInstanceLinker.linkProcessInstances(MAIN_PROCESS_ID, List.of("orphan-1"), LINK_TYPE, TEST_USER);

        assertThat(orphan.getLinkedProcessInstanceId()).isEqualTo(MAIN_PROCESS_ID);
        verify(processInstanceRepository).saveAll(List.of(orphan));
    }

    @Test
    void should_linkProcess_when_only_matchingInitiators() {
        ProcessInstanceEntity mainProcess = mainProcess(TEST_USER);
        ProcessInstanceEntity orphan1 = orphan("orphan-1", TEST_USER);
        ProcessInstanceEntity orphan2 = orphan("orphan-2", "anotherUser");
        ProcessInstanceEntity orphan3 = orphan("orphan-3", TEST_USER);

        given(processInstanceRepository.findById(MAIN_PROCESS_ID)).willReturn(Optional.of(mainProcess));
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(
            List.of(orphan1, orphan2, orphan3)
        );

        processInstanceLinker.linkProcessInstances(
            MAIN_PROCESS_ID,
            List.of("orphan-1", "orphan-2", "orphan-3"),
            LINK_TYPE,
            TEST_USER
        );

        verify(processInstanceRepository).saveAll(List.of(orphan1, orphan3));
        assertThat(orphan2.getLinkedProcessInstanceId()).isNull();
    }

    @ParameterizedTest
    @MethodSource("provideInvalidProcessInstanceIds")
    void should_not_linkProcess_when_processInstanceIds_isInvalid(List<String> processInstanceIds) {
        processInstanceLinker.linkProcessInstances(MAIN_PROCESS_ID, processInstanceIds, LINK_TYPE, TEST_USER);

        verify(processInstanceRepository, never()).findAll(any(ProcessInstanceSpecification.class));
        verify(processInstanceRepository, never()).saveAll(any());
        verifyNoInteractions(processInstanceHierarchyRepository);
    }

    @Test
    void should_not_linkProcess_when_orphanProcesses_notFound() {
        given(processInstanceRepository.findById(MAIN_PROCESS_ID)).willReturn(Optional.of(mainProcess(TEST_USER)));
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(
            Collections.emptyList()
        );

        processInstanceLinker.linkProcessInstances(
            MAIN_PROCESS_ID,
            List.of("orphan-1", "orphan-2"),
            LINK_TYPE,
            TEST_USER
        );

        verify(processInstanceRepository, never()).saveAll(any());
        verifyNoInteractions(processInstanceHierarchyRepository);
    }

    @Test
    void should_not_linkProcess_when_initiators_dontMatch() {
        ProcessInstanceEntity orphan = orphan("orphan-1", TEST_USER);

        given(processInstanceRepository.findById(MAIN_PROCESS_ID)).willReturn(Optional.of(mainProcess("anotherUser")));
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(List.of(orphan));

        processInstanceLinker.linkProcessInstances(MAIN_PROCESS_ID, List.of("orphan-1"), LINK_TYPE, TEST_USER);

        assertThat(orphan.getLinkedProcessInstanceId()).isNull();
        verifyNoInteractions(processInstanceHierarchyRepository);
    }

    @Test
    void should_linkProcess_when_mainProcess_foundOnRetry() {
        ProcessInstanceEntity orphan = orphan("orphan-1", TEST_USER);

        given(processInstanceRepository.findById(MAIN_PROCESS_ID)).willReturn(
            Optional.empty(),
            Optional.of(mainProcess(TEST_USER))
        );
        runScheduledTasksImmediately();
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(List.of(orphan));

        processInstanceLinker.linkProcessInstances(MAIN_PROCESS_ID, List.of("orphan-1"), LINK_TYPE, TEST_USER);

        verify(retryScheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
        assertThat(orphan.getLinkedProcessInstanceId()).isEqualTo(MAIN_PROCESS_ID);
    }

    @Test
    void should_stopRetrying_when_mainProcess_neverFound() {
        given(processInstanceRepository.findById(MAIN_PROCESS_ID)).willReturn(Optional.empty());
        runScheduledTasksImmediately();

        processInstanceLinker.linkProcessInstances(MAIN_PROCESS_ID, List.of("orphan-1"), LINK_TYPE, TEST_USER);

        verify(retryScheduler, times(3)).schedule(any(Runnable.class), any(Instant.class));
        verify(processInstanceRepository, times(4)).findById(MAIN_PROCESS_ID);
        verify(processInstanceRepository, never()).findAll(any(ProcessInstanceSpecification.class));
        verifyNoInteractions(processInstanceHierarchyRepository);
    }

    @Test
    void should_retryLinking_when_linkFailsWithException() {
        ProcessInstanceEntity failedAttemptOrphan = orphan("orphan-1", TEST_USER);
        ProcessInstanceEntity retriedOrphan = orphan("orphan-1", TEST_USER);

        given(processInstanceRepository.findById(MAIN_PROCESS_ID)).willReturn(Optional.of(mainProcess(TEST_USER)));
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(
            List.of(failedAttemptOrphan),
            List.of(retriedOrphan)
        );
        given(processInstanceRepository.saveAll(any()))
            .willThrow(new IllegalStateException("database unavailable"))
            .willReturn(List.of(retriedOrphan));
        runScheduledTasksImmediately();

        processInstanceLinker.linkProcessInstances(MAIN_PROCESS_ID, List.of("orphan-1"), LINK_TYPE, TEST_USER);

        verify(retryScheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
        verify(transactionManager).rollback(any());
        verify(transactionManager).commit(any());
        assertThat(retriedOrphan.getLinkedProcessInstanceId()).isEqualTo(MAIN_PROCESS_ID);
    }

    @Test
    void should_notLinkProcess_when_alreadyLinked() {
        ProcessInstanceEntity alreadyLinked = orphan("orphan-1", TEST_USER);
        alreadyLinked.setLinkedProcessInstanceId("other-main");

        given(processInstanceRepository.findById(MAIN_PROCESS_ID)).willReturn(Optional.of(mainProcess(TEST_USER)));
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(
            List.of(alreadyLinked)
        );

        processInstanceLinker.linkProcessInstances(MAIN_PROCESS_ID, List.of("orphan-1"), LINK_TYPE, TEST_USER);

        assertThat(alreadyLinked.getLinkedProcessInstanceId()).isEqualTo("other-main");
        verifyNoInteractions(processInstanceHierarchyRepository);
    }

    @Test
    void should_keepShortestDepth_when_linkedProcessIsAlsoDescendantOfAnotherLinkedProcess() {
        ProcessInstanceEntity parent = orphan("orphan-1", TEST_USER);
        ProcessInstanceEntity child = orphan("child-1", TEST_USER);

        given(processInstanceRepository.findById(MAIN_PROCESS_ID)).willReturn(Optional.of(mainProcess(TEST_USER)));
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(
            List.of(parent, child)
        );
        given(processInstanceHierarchyRepository.findByDescendantId(MAIN_PROCESS_ID)).willReturn(
            List.of(new ProcessInstanceHierarchyEntity(MAIN_PROCESS_ID, MAIN_PROCESS_ID, 0, "self"))
        );
        given(
            processInstanceHierarchyRepository.findByAncestorIdInAndDepthGreaterThan(List.of("orphan-1", "child-1"), -1)
        ).willReturn(
            List.of(
                new ProcessInstanceHierarchyEntity("orphan-1", "orphan-1", 0, "self"),
                new ProcessInstanceHierarchyEntity("orphan-1", "child-1", 1, "subprocess"),
                new ProcessInstanceHierarchyEntity("child-1", "child-1", 0, "self")
            )
        );

        processInstanceLinker.linkProcessInstances(
            MAIN_PROCESS_ID,
            List.of("orphan-1", "child-1"),
            LINK_TYPE,
            TEST_USER
        );

        verify(processInstanceHierarchyRepository).saveAll(savedHierarchyRows.capture());
        assertThat(savedHierarchyRows.getValue())
            .extracting(
                ProcessInstanceHierarchyEntity::getAncestorId,
                ProcessInstanceHierarchyEntity::getDescendantId,
                ProcessInstanceHierarchyEntity::getDepth
            )
            .containsExactlyInAnyOrder(tuple(MAIN_PROCESS_ID, "orphan-1", 1), tuple(MAIN_PROCESS_ID, "child-1", 1));
    }

    @Test
    void should_registerLinkedProcessAndItsSubtree_when_linked() {
        ProcessInstanceEntity orphan = orphan("orphan-1", TEST_USER);

        given(processInstanceRepository.findById(MAIN_PROCESS_ID)).willReturn(Optional.of(mainProcess(TEST_USER)));
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

        given(processInstanceRepository.findById(MAIN_PROCESS_ID)).willReturn(Optional.of(mainProcess(TEST_USER)));
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

    @Test
    void should_shutdownRetryScheduler_when_shutdown() {
        processInstanceLinker.shutdown();

        verify(retryScheduler).shutdown();
    }

    private void runScheduledTasksImmediately() {
        given(retryScheduler.schedule(any(Runnable.class), any(Instant.class))).willAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        });
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
