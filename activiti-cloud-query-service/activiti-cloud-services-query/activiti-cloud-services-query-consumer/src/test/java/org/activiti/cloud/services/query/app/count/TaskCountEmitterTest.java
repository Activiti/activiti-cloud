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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.activiti.api.task.model.impl.TaskCandidateGroupImpl;
import org.activiti.api.task.model.impl.TaskCandidateUserImpl;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupRemovedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserRemovedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskCountEmitterTest {

    private static final long NOW = 1_700_000_000_000L;

    @Mock
    private TaskCountRecomputer recomputer;

    @Mock
    private TaskCountChangePublisher publisher;

    @Captor
    private ArgumentCaptor<Set<String>> taskIdsCaptor;

    @Captor
    private ArgumentCaptor<Set<String>> groupsCaptor;

    @Captor
    private ArgumentCaptor<Set<String>> usersCaptor;

    @Captor
    private ArgumentCaptor<List<TaskCountChangedEvent>> publishedCaptor;

    private TaskCountEmitter emitter;

    @BeforeEach
    void setUp() {
        emitter = new TaskCountEmitter(recomputer, publisher, Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC));
    }

    @Test
    void shouldDoNothingWhenTheBatchHasNoTaskEvents() {
        emitter.emitFor(List.of(new CloudProcessStartedEventImpl()));

        verifyNoInteractions(recomputer, publisher);
    }

    @Test
    void shouldTakeTaskIdsStraightOffTaskEvents() {
        given(recomputer.recompute(anySet(), anySet(), anySet())).willReturn(List.of());

        emitter.emitFor(List.of(taskCreated("task-1"), taskCreated("task-2"), taskCreated("task-1")));

        verify(recomputer).recompute(taskIdsCaptor.capture(), groupsCaptor.capture(), usersCaptor.capture());
        assertThat(taskIdsCaptor.getValue()).containsExactlyInAnyOrder("task-1", "task-2");
        assertThat(groupsCaptor.getValue()).isEmpty();
        assertThat(usersCaptor.getValue()).isEmpty();
    }

    @Test
    void shouldKeepBothIdentitiesOffCandidateEventsNotJustTheTaskId() {
        given(recomputer.recompute(anySet(), anySet(), anySet())).willReturn(List.of());

        emitter.emitFor(List.of(candidateGroupRemoved("task-1", "eng"), candidateUserRemoved("task-2", "alice")));

        verify(recomputer).recompute(taskIdsCaptor.capture(), groupsCaptor.capture(), usersCaptor.capture());
        assertThat(taskIdsCaptor.getValue()).containsExactlyInAnyOrder("task-1", "task-2");
        // Neither removed row can be read back from the database, so the events are the only source. Keeping
        // the group but discarding the user is what stopped per-user counts from being pushable at all.
        assertThat(groupsCaptor.getValue()).containsExactly("eng");
        assertThat(usersCaptor.getValue()).containsExactly("alice");
    }

    @Test
    void shouldPublishOneStampedChangePerAffectedSubscriber() {
        given(recomputer.recompute(anySet(), anySet(), anySet())).willReturn(
            List.of(
                TaskCountChange.forQueued("pluto", List.of("banana"), 3),
                TaskCountChange.forQueued("dave", List.of("banana"), 1)
            )
        );

        emitter.emitFor(List.of(taskCreated("task-1")));

        verify(publisher).publish(publishedCaptor.capture());
        assertThat(publishedCaptor.getValue()).containsExactlyInAnyOrder(
            new TaskCountChangedEvent("queued:pluto", List.of("banana"), 3, NOW),
            new TaskCountChangedEvent("queued:dave", List.of("banana"), 1, NOW)
        );
    }

    @Test
    void shouldNotPublishWhenNoAudienceIsAffected() {
        given(recomputer.recompute(anySet(), anySet(), anySet())).willReturn(List.of());

        emitter.emitFor(List.of(taskCreated("task-1")));

        verify(publisher, never()).publish(anyList());
    }

    @Test
    void shouldSwallowRecomputeFailuresBecauseTheBatchHasAlreadyCommitted() {
        given(recomputer.recompute(anySet(), anySet(), anySet())).willThrow(new IllegalStateException("database gone"));

        emitter.emitFor(List.of(taskCreated("task-1")));

        verify(publisher, never()).publish(anyList());
    }

    private static CloudRuntimeEvent<?, ?> taskCreated(String taskId) {
        TaskImpl task = new TaskImpl();
        task.setId(taskId);
        return new CloudTaskCreatedEventImpl(task);
    }

    private static CloudRuntimeEvent<?, ?> candidateGroupRemoved(String taskId, String groupId) {
        return new CloudTaskCandidateGroupRemovedEventImpl(new TaskCandidateGroupImpl(groupId, taskId));
    }

    private static CloudRuntimeEvent<?, ?> candidateUserRemoved(String taskId, String userId) {
        return new CloudTaskCandidateUserRemovedEventImpl(new TaskCandidateUserImpl(userId, taskId));
    }
}
