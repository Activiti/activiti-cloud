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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.subscription.CountChangedMessage;
import org.activiti.cloud.services.query.subscription.ScopeKeys;
import org.junit.jupiter.api.Test;

class AssignedTaskCounterTest {

    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final AssignedTaskCounter counter = new AssignedTaskCounter(taskRepository);

    @Test
    void shouldReportTheAssignedCountType() {
        assertThat(counter.type()).isEqualTo(ScopeKeys.PushedCountType.ASSIGNED);
    }

    @Test
    void shouldEmitOneAbsoluteCountPerAffectedUser_whenAUserIsAbsentFromTheResult() {
        Instant asOf = Instant.parse("2026-09-16T10:15:30Z");
        when(taskRepository.countGroupedByAssignee(any(), eq(Task.TaskStatus.ASSIGNED))).thenReturn(
            List.of(assigneeCount("alice", 2L), assigneeCount("bob", 1L))
        );

        List<CountChangedMessage> messages = counter.countFor(List.of("alice", "bob", "carol"), asOf);

        assertThat(messages)
            .extracting(CountChangedMessage::scopeKey, CountChangedMessage::count, CountChangedMessage::asOf)
            .containsExactlyInAnyOrder(
                tuple(ScopeKeys.assigned("alice"), 2L, asOf),
                tuple(ScopeKeys.assigned("bob"), 1L, asOf),
                tuple(ScopeKeys.assigned("carol"), 0L, asOf)
            );
    }

    @Test
    void shouldDeduplicateAffectedUsersAndQueryOnlyByAssignedStatus() {
        Instant asOf = Instant.parse("2026-09-16T10:15:30Z");
        when(taskRepository.countGroupedByAssignee(any(), eq(Task.TaskStatus.ASSIGNED))).thenReturn(
            List.of(assigneeCount("alice", 3L))
        );

        List<CountChangedMessage> messages = counter.countFor(List.of("alice", "alice"), asOf);

        verify(taskRepository).countGroupedByAssignee(eq(Set.of("alice")), eq(Task.TaskStatus.ASSIGNED));
        assertThat(messages)
            .extracting(CountChangedMessage::scopeKey, CountChangedMessage::count, CountChangedMessage::asOf)
            .containsExactly(tuple(ScopeKeys.assigned("alice"), 3L, asOf));
    }

    @Test
    void shouldReturnEmptyAndNotQuery_whenThereAreNoAffectedUsers() {
        assertThat(counter.countFor(Set.of(), Instant.now())).isEmpty();
        assertThat(counter.countFor(null, Instant.now())).isEmpty();
        verifyNoInteractions(taskRepository);
    }

    private static TaskRepository.AssigneeCount assigneeCount(String assignee, long count) {
        return new TaskRepository.AssigneeCount() {
            @Override
            public String getAssignee() {
                return assignee;
            }

            @Override
            public long getTaskCount() {
                return count;
            }
        };
    }
}
