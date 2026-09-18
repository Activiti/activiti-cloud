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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.subscription.ScopeKeys.PushedCountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecomputeAudienceResolverTest {

    @Mock
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @Mock
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @Mock
    private TaskRepository taskRepository;

    private ConsumerSubscriberRegistry registry;
    private RecomputeAudienceResolver resolver;

    @BeforeEach
    void setUp() {
        registry = new ConsumerSubscriberRegistry();
        resolver = new RecomputeAudienceResolver(
            registry,
            taskCandidateUserRepository,
            taskCandidateGroupRepository,
            taskRepository
        );
        lenient()
            .when(taskCandidateUserRepository.findByTaskIdIn(org.mockito.ArgumentMatchers.<Set<String>>any()))
            .thenReturn(Set.of());
        lenient()
            .when(taskCandidateGroupRepository.findByTaskIdIn(org.mockito.ArgumentMatchers.<Set<String>>any()))
            .thenReturn(Set.of());
        lenient()
            .when(
                taskCandidateUserRepository.findByTask_ProcessInstanceIdIn(
                    org.mockito.ArgumentMatchers.<Set<String>>any()
                )
            )
            .thenReturn(Set.of());
        lenient()
            .when(taskRepository.findByProcessInstanceIdIn(org.mockito.ArgumentMatchers.<Set<String>>any()))
            .thenReturn(List.of());
    }

    @Test
    void namedUser_isIncluded_onlyWhenWatching() {
        registry.register("alice", Set.of(), "rest-1", java.time.Instant.EPOCH);
        // bob is named but never registered.
        ConsumerRecomputeWindow window = window(Set.of("task-1"), Set.of(), Set.of("alice", "bob"), Set.of(), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.ASSIGNED)).containsExactly("alice");
        assertThat(audience.get(PushedCountType.QUEUED)).containsExactly("alice");
    }

    @Test
    void namedUserDoor_readsBackCandidateUsersOfTouchedTasks() {
        registry.register("carol", Set.of(), "rest-1", java.time.Instant.EPOCH);
        when(taskCandidateUserRepository.findByTaskIdIn(eq(Set.of("task-1")))).thenReturn(
            Set.of(new TaskCandidateUserEntity("task-1", "carol"))
        );
        ConsumerRecomputeWindow window = window(Set.of("task-1"), Set.of(), Set.of(), Set.of(), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.ASSIGNED)).containsExactly("carol");
    }

    @Test
    void groupDoor_matchesWatchedUsersWhoseGroupsIntersectTheTouchedGroups() {
        registry.register("dave", Set.of("eng"), "rest-1", java.time.Instant.EPOCH);
        registry.register("erin", Set.of("fin"), "rest-1", java.time.Instant.EPOCH);
        ConsumerRecomputeWindow window = window(Set.of("task-1"), Set.of("eng"), Set.of(), Set.of(), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.QUEUED)).containsExactly("dave");
        assertThat(audience.get(PushedCountType.PROCESSES)).isEmpty();
    }

    @Test
    void groupDoor_alsoReadsBackCandidateGroupsOfTouchedTasks() {
        registry.register("frank", Set.of("ops"), "rest-1", java.time.Instant.EPOCH);
        when(taskCandidateGroupRepository.findByTaskIdIn(eq(Set.of("task-1")))).thenReturn(
            Set.of(new TaskCandidateGroupEntity("task-1", "ops"))
        );
        ConsumerRecomputeWindow window = window(Set.of("task-1"), Set.of(), Set.of(), Set.of(), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.ASSIGNED)).containsExactly("frank");
        assertThat(audience.get(PushedCountType.PROCESSES)).isEmpty();
    }

    @Test
    void taskDomainAudience_alsoFeedsRunningProcesses() {
        registry.register("alice", Set.of(), "rest-1", java.time.Instant.EPOCH);
        ConsumerRecomputeWindow window = window(Set.of("task-1"), Set.of(), Set.of("alice"), Set.of(), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.PROCESSES)).containsExactly("alice");
    }

    @Test
    void processDomain_includesNamedInitiators_butNoGroupDoor() {
        registry.register("gina", Set.of("eng"), "rest-1", java.time.Instant.EPOCH);
        registry.register("henry", Set.of(), "rest-1", java.time.Instant.EPOCH);
        ConsumerRecomputeWindow window = window(Set.of(), Set.of(), Set.of(), Set.of("proc-1"), Set.of("henry"));

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.PROCESSES)).containsExactly("henry");
        assertThat(audience.get(PushedCountType.ASSIGNED)).isEmpty();
        assertThat(audience.get(PushedCountType.QUEUED)).isEmpty();
    }

    @Test
    void processDomain_alsoIncludesCurrentAssigneesOfTasksInTheTouchedProcess() {
        registry.register("iris", Set.of(), "rest-1", java.time.Instant.EPOCH);
        TaskEntity task = new TaskEntity();
        task.setAssignee("iris");
        when(taskRepository.findByProcessInstanceIdIn(eq(Set.of("proc-1")))).thenReturn(List.of(task));
        ConsumerRecomputeWindow window = window(Set.of(), Set.of(), Set.of(), Set.of("proc-1"), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.PROCESSES)).containsExactly("iris");
    }

    @Test
    void processDomain_ignoresUnassignedTasksInTheTouchedProcess() {
        registry.register("iris", Set.of(), "rest-1", java.time.Instant.EPOCH);
        TaskEntity task = new TaskEntity();
        when(taskRepository.findByProcessInstanceIdIn(eq(Set.of("proc-1")))).thenReturn(List.of(task));
        ConsumerRecomputeWindow window = window(Set.of(), Set.of(), Set.of(), Set.of("proc-1"), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.PROCESSES)).isEmpty();
    }

    @Test
    void processDomain_alsoIncludesCandidateUsersOfTasksInTheTouchedProcess() {
        registry.register("jack", Set.of(), "rest-1", java.time.Instant.EPOCH);
        when(taskCandidateUserRepository.findByTask_ProcessInstanceIdIn(eq(Set.of("proc-1")))).thenReturn(
            Set.of(new TaskCandidateUserEntity("task-1", "jack"))
        );
        ConsumerRecomputeWindow window = window(Set.of(), Set.of(), Set.of(), Set.of("proc-1"), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.PROCESSES)).containsExactly("jack");
    }

    @Test
    void emptyWindow_resolvesToNoAudience() {
        ConsumerRecomputeWindow window = window(Set.of(), Set.of(), Set.of(), Set.of(), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.ASSIGNED)).isEmpty();
        assertThat(audience.get(PushedCountType.QUEUED)).isEmpty();
        assertThat(audience.get(PushedCountType.PROCESSES)).isEmpty();
    }

    private static ConsumerRecomputeWindow window(
        Set<String> taskIds,
        Set<String> touchedGroupIds,
        Set<String> namedUserIds,
        Set<String> processInstanceIds,
        Set<String> namedInitiatorIds
    ) {
        return new ConsumerRecomputeWindow(
            taskIds,
            touchedGroupIds,
            namedUserIds,
            processInstanceIds,
            namedInitiatorIds
        );
    }
}
