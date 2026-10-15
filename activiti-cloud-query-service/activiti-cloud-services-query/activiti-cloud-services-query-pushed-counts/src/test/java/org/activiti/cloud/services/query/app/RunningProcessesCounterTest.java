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
package org.activiti.cloud.services.query.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.subscription.ScopeKeys;
import org.junit.jupiter.api.Test;

class RunningProcessesCounterTest {

    private static final ProcessInstance.ProcessInstanceStatus RUNNING = ProcessInstance.ProcessInstanceStatus.RUNNING;

    private final ProcessInstanceRepository processInstanceRepository = mock(ProcessInstanceRepository.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final TaskCandidateUserRepository taskCandidateUserRepository = mock(TaskCandidateUserRepository.class);
    private final RunningProcessesCounter counter = new RunningProcessesCounter(
        processInstanceRepository,
        taskRepository,
        taskCandidateUserRepository
    );

    @Test
    void shouldReportTheProcessesCountType() {
        assertThat(counter.type()).isEqualTo(ScopeKeys.PushedCountType.PROCESSES);
    }

    @Test
    void shouldReturnAbsoluteCountPerAffectedUser_withZeroForThoseWithNone() {
        when(processInstanceRepository.findRunningByInitiatorIn(any(), eq(RUNNING))).thenReturn(
            List.of(initiatorProcess("alice", "proc-1"))
        );

        Map<String, Long> counts = counter.compute(Set.of("alice", "bob"));

        assertThat(counts).containsOnly(entry("alice", 1L), entry("bob", 0L));
    }

    @Test
    void shouldCountEachDoorIndependently() {
        when(processInstanceRepository.findRunningByInitiatorIn(any(), eq(RUNNING))).thenReturn(
            List.of(initiatorProcess("alice", "proc-1"))
        );
        when(taskRepository.findRunningProcessesByAssigneeIn(any(), eq(RUNNING))).thenReturn(
            List.of(assigneeProcess("bob", "proc-2"))
        );
        when(taskCandidateUserRepository.findRunningProcessesByCandidateUserIn(any(), eq(RUNNING))).thenReturn(
            List.of(candidateProcess("carol", "proc-3"))
        );

        Map<String, Long> counts = counter.compute(Set.of("alice", "bob", "carol"));

        assertThat(counts).containsOnly(entry("alice", 1L), entry("bob", 1L), entry("carol", 1L));
    }

    @Test
    void shouldCountAProcessOnce_whenVisibleThroughTwoDoorsAtOnce() {
        when(processInstanceRepository.findRunningByInitiatorIn(any(), eq(RUNNING))).thenReturn(
            List.of(initiatorProcess("alice", "proc-1"))
        );
        when(taskRepository.findRunningProcessesByAssigneeIn(any(), eq(RUNNING))).thenReturn(
            List.of(assigneeProcess("alice", "proc-1"))
        );

        Map<String, Long> counts = counter.compute(Set.of("alice"));

        assertThat(counts).containsOnly(entry("alice", 1L));
    }

    @Test
    void shouldQueryAllThreeDoorsWithTheAffectedUsersAndRunningStatus() {
        counter.compute(Set.of("alice"));

        verify(processInstanceRepository).findRunningByInitiatorIn(Set.of("alice"), RUNNING);
        verify(taskRepository).findRunningProcessesByAssigneeIn(Set.of("alice"), RUNNING);
        verify(taskCandidateUserRepository).findRunningProcessesByCandidateUserIn(Set.of("alice"), RUNNING);
    }

    @Test
    void shouldReturnEmptyAndNotQuery_whenThereAreNoAffectedUsers() {
        assertThat(counter.compute(Set.of())).isEmpty();
        verifyNoInteractions(processInstanceRepository, taskRepository, taskCandidateUserRepository);
    }

    private static ProcessInstanceRepository.InitiatorProcess initiatorProcess(
        String userId,
        String processInstanceId
    ) {
        return new ProcessInstanceRepository.InitiatorProcess() {
            @Override
            public String getUserId() {
                return userId;
            }

            @Override
            public String getProcessInstanceId() {
                return processInstanceId;
            }
        };
    }

    private static TaskRepository.AssigneeProcess assigneeProcess(String userId, String processInstanceId) {
        return new TaskRepository.AssigneeProcess() {
            @Override
            public String getUserId() {
                return userId;
            }

            @Override
            public String getProcessInstanceId() {
                return processInstanceId;
            }
        };
    }

    private static TaskCandidateUserRepository.CandidateProcess candidateProcess(
        String userId,
        String processInstanceId
    ) {
        return new TaskCandidateUserRepository.CandidateProcess() {
            @Override
            public String getUserId() {
                return userId;
            }

            @Override
            public String getProcessInstanceId() {
                return processInstanceId;
            }
        };
    }
}
