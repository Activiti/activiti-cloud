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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.specification.TaskSpecification;
import org.activiti.cloud.services.query.subscription.ScopeKeys;
import org.junit.jupiter.api.Test;

class QueuedTaskCounterTest {

    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final ConsumerSubscriberRegistry subscriberRegistry = mock(ConsumerSubscriberRegistry.class);
    private final QueuedTaskCounter counter = new QueuedTaskCounter(taskRepository, subscriberRegistry);

    @Test
    void shouldReportTheQueuedCountType() {
        assertThat(counter.type()).isEqualTo(ScopeKeys.PushedCountType.QUEUED);
    }

    @Test
    void shouldReturnEmptyAndNotQuery_whenThereAreNoAffectedUsers() {
        assertThat(counter.compute(Set.of())).isEmpty();
        verifyNoInteractions(taskRepository, subscriberRegistry);
    }

    @Test
    void shouldSumSharedGroupVisibleCountAndPersonalRemainder_whenAUserHasNoRemainder() {
        when(subscriberRegistry.groupsOf("alice")).thenReturn(Set.of("g1"));
        when(subscriberRegistry.groupsOf("bob")).thenReturn(Set.of("g1"));
        when(taskRepository.count(any(TaskSpecification.class))).thenReturn(10L);
        when(
            taskRepository.countQueuedPersonalRemainderGroupedByUser(
                List.of("alice", "bob"),
                Task.TaskStatus.CREATED,
                Set.of("g1")
            )
        ).thenReturn(List.of(userCount("alice", 3L)));

        Map<String, Long> counts = counter.compute(new LinkedHashSet<>(List.of("alice", "bob")));

        assertThat(counts).containsOnly(entry("alice", 13L), entry("bob", 10L));
    }

    @Test
    void shouldComputeTheSharedCountOncePerGroupSetBucket_whenMultipleUsersShareAGroupSet() {
        when(subscriberRegistry.groupsOf("alice")).thenReturn(Set.of("g1"));
        when(subscriberRegistry.groupsOf("bob")).thenReturn(Set.of("g1"));
        when(subscriberRegistry.groupsOf("carol")).thenReturn(Set.of("g2"));
        // One shared value per bucket, returned in bucket-creation order: {g1} then {g2}.
        when(taskRepository.count(any(TaskSpecification.class))).thenReturn(10L, 4L);
        when(
            taskRepository.countQueuedPersonalRemainderGroupedByUser(
                List.of("alice", "bob"),
                Task.TaskStatus.CREATED,
                Set.of("g1")
            )
        ).thenReturn(List.of(userCount("alice", 3L)));
        when(
            taskRepository.countQueuedPersonalRemainderGroupedByUser(
                List.of("carol"),
                Task.TaskStatus.CREATED,
                Set.of("g2")
            )
        ).thenReturn(List.of(userCount("carol", 1L)));

        Map<String, Long> counts = counter.compute(new LinkedHashSet<>(List.of("alice", "bob", "carol")));

        assertThat(counts).containsOnly(entry("alice", 13L), entry("bob", 10L), entry("carol", 5L));
        verify(taskRepository, times(2)).count(any(TaskSpecification.class));
    }

    @Test
    void shouldReturnZeroForAnAffectedUserWithNoVisibleQueuedTasks() {
        when(subscriberRegistry.groupsOf("dave")).thenReturn(Set.of("g9"));
        when(taskRepository.count(any(TaskSpecification.class))).thenReturn(0L);
        when(
            taskRepository.countQueuedPersonalRemainderGroupedByUser(
                List.of("dave"),
                Task.TaskStatus.CREATED,
                Set.of("g9")
            )
        ).thenReturn(List.of());

        Map<String, Long> counts = counter.compute(Set.of("dave"));

        // No shared visibility and no personal candidacy still yields an explicit zero, never an omission.
        assertThat(counts).containsOnly(entry("dave", 0L));
    }

    private static TaskRepository.UserCount userCount(String userId, long count) {
        return new TaskRepository.UserCount() {
            @Override
            public String getUserId() {
                return userId;
            }

            @Override
            public long getTaskCount() {
                return count;
            }
        };
    }
}
