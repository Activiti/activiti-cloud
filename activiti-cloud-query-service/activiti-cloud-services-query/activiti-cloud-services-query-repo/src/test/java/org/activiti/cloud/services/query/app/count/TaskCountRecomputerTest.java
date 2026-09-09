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
package org.activiti.cloud.services.query.app.count;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository.CandidateUserTaskCount;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.specification.TaskSpecification;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskCountRecomputerTest {

    private static final Set<String> NO_GROUPS_NAMED = Set.of();
    private static final Set<String> NO_USERS_NAMED = Set.of();

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @Mock
    private TaskCandidateUserRepository taskCandidateUserRepository;

    private SubscriberScopeRegistry registry;
    private TaskCountRecomputer recomputer;

    @BeforeEach
    void setUp() {
        registry = new SubscriberScopeRegistry(Duration.ofMinutes(5), 100);
        recomputer = new TaskCountRecomputer(
            taskRepository,
            taskCandidateGroupRepository,
            taskCandidateUserRepository,
            registry,
            200
        );
    }

    @Test
    void shouldDoNothingWhenThereIsNothingToRecomputeFrom() {
        registry.record("pluto", List.of("eng"));

        assertThat(recomputer.recompute(Set.of(), NO_GROUPS_NAMED, NO_USERS_NAMED)).isEmpty();

        verifyNoInteractions(taskRepository, taskCandidateGroupRepository, taskCandidateUserRepository);
    }

    @Test
    void shouldNotTouchTheDatabaseWhenNobodyIsListening() {
        assertThat(recomputer.recompute(Set.of("task-1"), NO_GROUPS_NAMED, NO_USERS_NAMED)).isEmpty();

        verifyNoInteractions(taskRepository, taskCandidateGroupRepository, taskCandidateUserRepository);
    }

    @Test
    void shouldPublishOnePerUserCountAndCostTwoQueriesPerGroupSet() {
        registry.record("pluto", List.of("eng"));
        registry.record("dave", List.of("eng"));
        registry.record("pippo", List.of("eng", "hr"));
        registry.record("nobody-here", List.of("finance"));
        given(taskCandidateGroupRepository.findByTaskIdIn(Set.of("task-1"))).willReturn(
            Set.of(new TaskCandidateGroupEntity("task-1", "eng"))
        );
        given(taskRepository.count(any(Specification.class))).willReturn(4L);
        given(
            taskCandidateUserRepository.countTasksNamingUserOutsideGroups(
                anyCollection(),
                anyCollection(),
                anyCollection()
            )
        ).willReturn(List.of(candidateUserCount("pluto", 2L)));

        List<TaskCountChange> changes = recomputer.recompute(Set.of("task-1"), NO_GROUPS_NAMED, NO_USERS_NAMED);

        // One message per person, never per group set - and pluto's own two tasks are in his number.
        assertThat(changes)
            .extracting(TaskCountChange::scopeKey, TaskCountChange::count)
            .containsExactlyInAnyOrder(tuple("queued:pluto", 6L), tuple("queued:dave", 4L), tuple("queued:pippo", 4L));
        // {eng} and {eng, hr} are two buckets, so two shared COUNTs, not one per subscriber.
        verify(taskRepository, times(2)).count(any(TaskSpecification.class));
        verify(taskCandidateUserRepository, times(2)).countTasksNamingUserOutsideGroups(
            anyCollection(),
            anyCollection(),
            anyCollection()
        );
    }

    @Test
    void shouldExcludeTheBucketsOwnGroupsFromTheRemainderSoNothingIsCountedTwice() {
        registry.record("pluto", List.of("eng", "hr"));
        given(taskCandidateGroupRepository.findByTaskIdIn(Set.of("task-1"))).willReturn(
            Set.of(new TaskCandidateGroupEntity("task-1", "eng"))
        );
        given(taskRepository.count(any(Specification.class))).willReturn(5L);

        recomputer.recompute(Set.of("task-1"), NO_GROUPS_NAMED, NO_USERS_NAMED);

        // The remainder is a set difference against the bucket's whole group set - that is what makes the two
        // halves disjoint and the sum legal.
        verify(taskCandidateUserRepository).countTasksNamingUserOutsideGroups(
            eq(List.of("pluto")),
            eq(PushedTaskCountFilter.QUEUED.status()),
            eq(List.of("eng", "hr"))
        );
    }

    @Test
    void shouldReachAUserNamedOnTheTaskWhoseGroupsIntersectNothing() {
        registry.record("pluto", List.of("banana"));
        registry.record("pippo", List.of("cherry"));
        given(taskCandidateGroupRepository.findByTaskIdIn(Set.of("task-3"))).willReturn(
            Set.of(new TaskCandidateGroupEntity("task-3", "banana"))
        );
        given(taskCandidateUserRepository.findByTaskIdIn(Set.of("task-3"))).willReturn(
            Set.of(new TaskCandidateUserEntity("task-3", "pippo"))
        );
        given(taskRepository.count(any(Specification.class))).willReturn(1L);
        given(
            taskCandidateUserRepository.countTasksNamingUserOutsideGroups(
                anyCollection(),
                anyCollection(),
                anyCollection()
            )
        ).willReturn(List.of(candidateUserCount("pippo", 1L)));

        List<TaskCountChange> changes = recomputer.recompute(Set.of("task-3"), NO_GROUPS_NAMED, NO_USERS_NAMED);

        // The group door alone would never create pippo's bucket: {cherry} intersects nothing the batch
        // touched. He is reachable only as a candidate user of a task in this batch. Under the old design he
        // received no message at all.
        assertThat(changes).extracting(TaskCountChange::scopeKey).contains("queued:pippo");
    }

    @Test
    void shouldReachAUserOnlyNamedByTheEventsWhenTheirRowIsAlreadyGone() {
        registry.record("pippo", List.of("cherry"));
        given(taskCandidateGroupRepository.findByTaskIdIn(Set.of("task-3"))).willReturn(
            Set.of(new TaskCandidateGroupEntity("task-3", "banana"))
        );
        // The candidate row was deleted in the committed batch, so nothing links the task to pippo any more.
        given(taskCandidateUserRepository.findByTaskIdIn(Set.of("task-3"))).willReturn(Set.of());
        given(taskRepository.count(any(Specification.class))).willReturn(0L);

        List<TaskCountChange> changes = recomputer.recompute(Set.of("task-3"), NO_GROUPS_NAMED, Set.of("pippo"));

        assertThat(changes).extracting(TaskCountChange::scopeKey).containsExactly("queued:pippo");
    }

    @Test
    void shouldRecomputeForAGroupOnlyNamedByTheEventsWhenItsRowIsAlreadyGone() {
        registry.record("pluto", List.of("eng"));
        given(taskCandidateGroupRepository.findByTaskIdIn(Set.of("task-1"))).willReturn(
            Set.of(new TaskCandidateGroupEntity("task-1", "hr"))
        );
        given(taskRepository.count(any(Specification.class))).willReturn(2L);

        List<TaskCountChange> changes = recomputer.recompute(Set.of("task-1"), Set.of("eng"), NO_USERS_NAMED);

        assertThat(changes).extracting(TaskCountChange::scopeKey).containsExactly("queued:pluto");
    }

    @Test
    void shouldRecomputeForEverySubscriberWhenATaskHasNoCandidatesAtAll() {
        registry.record("pluto", List.of("eng"));
        registry.record("pippo", List.of("finance"));
        given(taskCandidateGroupRepository.findByTaskIdIn(Set.of("task-1"))).willReturn(Set.of());
        given(taskCandidateUserRepository.findByTaskIdIn(anyCollection())).willReturn(Set.of());
        given(taskRepository.count(any(Specification.class))).willReturn(9L);

        List<TaskCountChange> changes = recomputer.recompute(Set.of("task-1"), NO_GROUPS_NAMED, NO_USERS_NAMED);

        // Such a task matches the specification's "no candidate users or groups" branch, which everybody
        // sees, so there is no set of groups to narrow by.
        assertThat(changes)
            .extracting(TaskCountChange::scopeKey)
            .containsExactlyInAnyOrder("queued:pluto", "queued:pippo");
    }

    @Test
    void shouldNotTreatACandidateUserOnlyTaskAsVisibleToEveryone() {
        registry.record("pluto", List.of("eng"));
        given(taskCandidateGroupRepository.findByTaskIdIn(Set.of("task-1"))).willReturn(Set.of());
        given(taskCandidateUserRepository.findByTaskIdIn(anyCollection())).willReturn(
            Set.of(new TaskCandidateUserEntity("task-1", "alice"))
        );

        // Both candidate collections must be empty for the catch-all branch to match, and alice is not
        // registered, so nobody's count moved.
        assertThat(recomputer.recompute(Set.of("task-1"), NO_GROUPS_NAMED, NO_USERS_NAMED)).isEmpty();

        verify(taskRepository, never()).count(any(Specification.class));
    }

    @Test
    void shouldNotRecomputeWhenNoSubscriberIsAffected() {
        registry.record("pluto", List.of("eng"));
        given(taskCandidateGroupRepository.findByTaskIdIn(anySet())).willReturn(
            Set.of(new TaskCandidateGroupEntity("task-1", "legal"))
        );

        assertThat(recomputer.recompute(Set.of("task-1"), NO_GROUPS_NAMED, NO_USERS_NAMED)).isEmpty();

        verify(taskRepository, never()).count(any(Specification.class));
    }

    @Test
    void shouldCountAGrouplessSubscriberDirectlyRatherThanBucketingThem() {
        registry.record("solo", List.of());
        given(taskCandidateGroupRepository.findByTaskIdIn(Set.of("task-1"))).willReturn(Set.of());
        given(taskCandidateUserRepository.findByTaskIdIn(anyCollection())).willReturn(Set.of());
        given(taskRepository.count(any(Specification.class))).willReturn(1L);

        List<TaskCountChange> changes = recomputer.recompute(Set.of("task-1"), NO_GROUPS_NAMED, NO_USERS_NAMED);

        // Nothing to share and no groups to subtract, so there is no remainder query to run: the shared half
        // would be an unrestricted count and an empty JPQL IN list is not portable.
        assertThat(changes)
            .extracting(TaskCountChange::scopeKey, TaskCountChange::count)
            .containsExactly(tuple("queued:solo", 1L));
        verify(taskCandidateUserRepository, never()).countTasksNamingUserOutsideGroups(
            anyCollection(),
            anyCollection(),
            anyCollection()
        );
    }

    private static CandidateUserTaskCount candidateUserCount(String userId, long count) {
        return new CandidateUserTaskCount() {
            @Override
            public String getUserId() {
                return userId;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }
}
