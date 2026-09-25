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
import static org.mockito.Mockito.when;

import java.util.HashMap;
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
import org.activiti.cloud.services.query.subscription.SubscriberDirectory;
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

    private FakeSubscriberDirectory registry;
    private RecomputeAudienceResolver resolver;

    @BeforeEach
    void setUp() {
        registry = new FakeSubscriberDirectory();
        resolver = new RecomputeAudienceResolver(
            registry,
            taskCandidateUserRepository,
            taskCandidateGroupRepository,
            taskRepository
        );
    }

    @Test
    void namedUser_isIncluded_onlyWhenWatching() {
        registry.register("alice", Set.of());
        PushedCountsRecomputeWindow window = window(
            Set.of("task-1"),
            Set.of(),
            Set.of("alice", "bob"),
            Set.of(),
            Set.of()
        );

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.ASSIGNED)).containsExactly("alice");
        assertThat(audience.get(PushedCountType.QUEUED)).containsExactly("alice");
    }

    @Test
    void namedUserDoor_readsBackCandidateUsersOfTouchedTasks() {
        registry.register("carol", Set.of());
        when(taskCandidateUserRepository.findByTaskIdIn(Set.of("task-1"))).thenReturn(
            Set.of(new TaskCandidateUserEntity("task-1", "carol"))
        );
        PushedCountsRecomputeWindow window = window(Set.of("task-1"), Set.of(), Set.of(), Set.of(), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.ASSIGNED)).containsExactly("carol");
    }

    @Test
    void groupDoor_matchesWatchedUsersWhoseGroupsIntersectTheTouchedGroups() {
        registry.register("dave", Set.of("eng"));
        registry.register("erin", Set.of("fin"));
        PushedCountsRecomputeWindow window = window(Set.of("task-1"), Set.of("eng"), Set.of(), Set.of(), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.QUEUED)).containsExactly("dave");
        assertThat(audience.get(PushedCountType.PROCESSES)).isEmpty();
    }

    @Test
    void groupDoor_alsoReadsBackCandidateGroupsOfTouchedTasks() {
        registry.register("frank", Set.of("ops"));
        when(taskCandidateGroupRepository.findByTaskIdIn(Set.of("task-1"))).thenReturn(
            Set.of(new TaskCandidateGroupEntity("task-1", "ops"))
        );
        PushedCountsRecomputeWindow window = window(Set.of("task-1"), Set.of(), Set.of(), Set.of(), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.ASSIGNED)).containsExactly("frank");
        assertThat(audience.get(PushedCountType.PROCESSES)).isEmpty();
    }

    @Test
    void taskDomainAudience_alsoFeedsRunningProcesses() {
        registry.register("alice", Set.of());
        PushedCountsRecomputeWindow window = window(Set.of("task-1"), Set.of(), Set.of("alice"), Set.of(), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.PROCESSES)).containsExactly("alice");
    }

    @Test
    void processDomain_includesNamedInitiators_butNoGroupDoor() {
        registry.register("gina", Set.of("eng"));
        registry.register("henry", Set.of());
        PushedCountsRecomputeWindow window = window(Set.of(), Set.of(), Set.of(), Set.of("proc-1"), Set.of("henry"));

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.PROCESSES)).containsExactly("henry");
        assertThat(audience.get(PushedCountType.ASSIGNED)).isEmpty();
        assertThat(audience.get(PushedCountType.QUEUED)).isEmpty();
    }

    @Test
    void processDomain_alsoIncludesCurrentAssigneesOfTasksInTheTouchedProcess() {
        registry.register("iris", Set.of());
        TaskEntity task = new TaskEntity();
        task.setAssignee("iris");
        when(taskRepository.findByProcessInstanceIdIn(Set.of("proc-1"))).thenReturn(List.of(task));
        PushedCountsRecomputeWindow window = window(Set.of(), Set.of(), Set.of(), Set.of("proc-1"), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.PROCESSES)).containsExactly("iris");
        assertThat(audience.get(PushedCountType.ASSIGNED)).containsExactly("iris");
        assertThat(audience.get(PushedCountType.QUEUED)).containsExactly("iris");
    }

    @Test
    void processDomain_ignoresUnassignedTasksInTheTouchedProcess() {
        registry.register("iris", Set.of());
        TaskEntity task = new TaskEntity();
        when(taskRepository.findByProcessInstanceIdIn(Set.of("proc-1"))).thenReturn(List.of(task));
        PushedCountsRecomputeWindow window = window(Set.of(), Set.of(), Set.of(), Set.of("proc-1"), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.PROCESSES)).isEmpty();
    }

    @Test
    void processDomain_alsoIncludesCandidateUsersOfTasksInTheTouchedProcess() {
        registry.register("jack", Set.of());
        when(taskCandidateUserRepository.findByTask_ProcessInstanceIdIn(Set.of("proc-1"))).thenReturn(
            Set.of(new TaskCandidateUserEntity("task-1", "jack"))
        );
        PushedCountsRecomputeWindow window = window(Set.of(), Set.of(), Set.of(), Set.of("proc-1"), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.PROCESSES)).containsExactly("jack");
        assertThat(audience.get(PushedCountType.ASSIGNED)).containsExactly("jack");
        assertThat(audience.get(PushedCountType.QUEUED)).containsExactly("jack");
    }

    @Test
    void openTask_unassignedWithNoCandidates_feedsEveryWatchedUser_butNotProcesses() {
        registry.register("liz", Set.of());
        registry.register("moe", Set.of("ops"));
        TaskEntity task = new TaskEntity();
        task.setId("task-1");
        when(taskRepository.findAllById(Set.of("task-1"))).thenReturn(List.of(task));
        PushedCountsRecomputeWindow window = window(Set.of("task-1"), Set.of(), Set.of(), Set.of(), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.ASSIGNED)).containsExactlyInAnyOrder("liz", "moe");
        assertThat(audience.get(PushedCountType.QUEUED)).containsExactlyInAnyOrder("liz", "moe");
        assertThat(audience.get(PushedCountType.PROCESSES)).isEmpty();
    }

    @Test
    void openTask_thatIsAssigned_doesNotFeedEveryWatchedUser() {
        registry.register("liz", Set.of());
        TaskEntity task = new TaskEntity();
        task.setId("task-1");
        task.setAssignee("someone-else");
        when(taskRepository.findAllById(Set.of("task-1"))).thenReturn(List.of(task));
        PushedCountsRecomputeWindow window = window(Set.of("task-1"), Set.of(), Set.of(), Set.of(), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.QUEUED)).isEmpty();
    }

    @Test
    void openTask_thatHasACandidate_doesNotQueryOrFeedEveryWatchedUser() {
        registry.register("liz", Set.of());
        when(taskCandidateUserRepository.findByTaskIdIn(Set.of("task-1"))).thenReturn(
            Set.of(new TaskCandidateUserEntity("task-1", "someone-else"))
        );
        PushedCountsRecomputeWindow window = window(Set.of("task-1"), Set.of(), Set.of(), Set.of(), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.QUEUED)).isEmpty();
    }

    @Test
    void openProcessTask_unassignedWithNoCandidates_feedsEveryWatchedUser_butNotProcesses() {
        registry.register("liz", Set.of());
        registry.register("moe", Set.of("ops"));
        TaskEntity task = new TaskEntity();
        task.setId("task-1");
        when(taskRepository.findByProcessInstanceIdIn(Set.of("proc-1"))).thenReturn(List.of(task));
        PushedCountsRecomputeWindow window = window(Set.of(), Set.of(), Set.of(), Set.of("proc-1"), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.ASSIGNED)).containsExactlyInAnyOrder("liz", "moe");
        assertThat(audience.get(PushedCountType.QUEUED)).containsExactlyInAnyOrder("liz", "moe");
        assertThat(audience.get(PushedCountType.PROCESSES)).isEmpty();
    }

    @Test
    void processDomain_candidateGroupOfTasksInTheTouchedProcess_feedsAssignedAndQueued_butNotProcesses() {
        registry.register("karl", Set.of("ops"));
        when(taskCandidateGroupRepository.findByTask_ProcessInstanceIdIn(Set.of("proc-1"))).thenReturn(
            Set.of(new TaskCandidateGroupEntity("task-1", "ops"))
        );
        PushedCountsRecomputeWindow window = window(Set.of(), Set.of(), Set.of(), Set.of("proc-1"), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.ASSIGNED)).containsExactly("karl");
        assertThat(audience.get(PushedCountType.QUEUED)).containsExactly("karl");
        assertThat(audience.get(PushedCountType.PROCESSES)).isEmpty();
    }

    @Test
    void emptyWindow_resolvesToNoAudience() {
        PushedCountsRecomputeWindow window = window(Set.of(), Set.of(), Set.of(), Set.of(), Set.of());

        Map<PushedCountType, Set<String>> audience = resolver.resolve(window);

        assertThat(audience.get(PushedCountType.ASSIGNED)).isEmpty();
        assertThat(audience.get(PushedCountType.QUEUED)).isEmpty();
        assertThat(audience.get(PushedCountType.PROCESSES)).isEmpty();
    }

    private static PushedCountsRecomputeWindow window(
        Set<String> taskIds,
        Set<String> touchedGroupIds,
        Set<String> namedUserIds,
        Set<String> processInstanceIds,
        Set<String> namedInitiatorIds
    ) {
        return new PushedCountsRecomputeWindow(
            taskIds,
            touchedGroupIds,
            namedUserIds,
            processInstanceIds,
            namedInitiatorIds
        );
    }

    private static final class FakeSubscriberDirectory implements SubscriberDirectory {

        private final Map<String, Set<String>> watching = new HashMap<>();

        void register(String userId, Set<String> groups) {
            watching.put(userId, groups);
        }

        @Override
        public boolean isWatching(String userId) {
            return watching.containsKey(userId);
        }

        @Override
        public Set<String> groupsOf(String userId) {
            return watching.getOrDefault(userId, Set.of());
        }

        @Override
        public Set<String> watchedUserIds() {
            return Set.copyOf(watching.keySet());
        }
    }
}
