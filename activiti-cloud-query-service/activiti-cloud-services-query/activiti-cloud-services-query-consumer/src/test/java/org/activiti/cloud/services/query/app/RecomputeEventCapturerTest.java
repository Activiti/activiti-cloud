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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.activiti.api.task.model.impl.TaskCandidateGroupImpl;
import org.activiti.api.task.model.impl.TaskCandidateUserImpl;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.CloudProcessInstanceImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessResumedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessSuspendedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskActivatedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskAssignedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCancelledEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupAddedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupRemovedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserAddedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserRemovedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCompletedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskSuspendedEventImpl;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.junit.jupiter.api.Test;

class RecomputeEventCapturerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private final ConsumerRecomputeBuffer buffer = new ConsumerRecomputeBuffer();
    private final ConsumerSubscriberRegistry registry = registeredRegistry();
    private boolean featureEnabled = true;
    private final FeatureToggle featureToggle = name ->
        featureEnabled && QueryFeatureToggles.FEATURE_PUSHED_COUNTS.equals(name);
    private final RecomputeEventCapturer capturer = new RecomputeEventCapturer(
        buffer,
        registry,
        featureToggle,
        Clock.fixed(T0, ZoneOffset.UTC)
    );

    @Test
    void doesNothing_whenFeatureDisabled() {
        featureEnabled = false;

        capturer.capture(List.of(taskCreated("task-1")));

        assertThat(buffer.isEmpty()).isTrue();
    }

    @Test
    void doesNothing_whenEventsIsNull() {
        capturer.capture(null);

        assertThat(buffer.isEmpty()).isTrue();
    }

    @Test
    void doesNothing_whenNoSubscribers() {
        RecomputeEventCapturer capturerWithNoWatchers = new RecomputeEventCapturer(
            buffer,
            new ConsumerSubscriberRegistry(),
            featureToggle,
            Clock.fixed(T0, ZoneOffset.UTC)
        );

        capturerWithNoWatchers.capture(List.of(taskCreated("task-1")));

        assertThat(buffer.isEmpty()).isTrue();
    }

    @Test
    void taskCreated_capturesTheTask() {
        capturer.capture(List.of(taskCreated("task-1")));

        assertThat(buffer.drainAndReset().taskIds()).containsExactly("task-1");
    }

    @Test
    void taskCreated_capturesTheAssigneeAndOwner_whenAlreadyPreAssignedOnCreation() {
        TaskImpl task = new TaskImpl();
        task.setId("task-1");
        task.setAssignee("alice");
        task.setOwner("bob");

        capturer.capture(List.of(new CloudTaskCreatedEventImpl(task)));

        ConsumerRecomputeWindow window = buffer.drainAndReset();
        assertThat(window.taskIds()).containsExactly("task-1");
        assertThat(window.namedUserIds()).containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void taskAssigned_capturesTheTask_andTheAssigneeAndOwner() {
        TaskImpl task = new TaskImpl();
        task.setId("task-1");
        task.setAssignee("alice");
        task.setOwner("bob");

        capturer.capture(List.of(new CloudTaskAssignedEventImpl("evt", 0L, task)));

        ConsumerRecomputeWindow window = buffer.drainAndReset();
        assertThat(window.taskIds()).containsExactly("task-1");
        assertThat(window.namedUserIds()).containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void taskCompleted_capturesTheTask_andCompletedByAndAssigneeAndOwner() {
        TaskImpl task = new TaskImpl();
        task.setId("task-1");
        task.setCompletedBy("alice");
        task.setAssignee("bob");
        task.setOwner("carol");

        capturer.capture(List.of(new CloudTaskCompletedEventImpl("evt", 0L, task)));

        ConsumerRecomputeWindow window = buffer.drainAndReset();
        assertThat(window.taskIds()).containsExactly("task-1");
        assertThat(window.namedUserIds()).containsExactlyInAnyOrder("alice", "bob", "carol");
    }

    @Test
    void taskCancelled_capturesTheTask_andTheAssigneeAndOwner() {
        TaskImpl task = new TaskImpl();
        task.setId("task-1");
        task.setAssignee("alice");
        task.setOwner("bob");

        capturer.capture(List.of(new CloudTaskCancelledEventImpl(task)));

        ConsumerRecomputeWindow window = buffer.drainAndReset();
        assertThat(window.taskIds()).containsExactly("task-1");
        assertThat(window.namedUserIds()).containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void taskSuspended_capturesTheTask_andTheAssigneeAndOwner() {
        TaskImpl task = new TaskImpl();
        task.setId("task-1");
        task.setAssignee("alice");
        task.setOwner("bob");

        capturer.capture(List.of(new CloudTaskSuspendedEventImpl(task)));

        ConsumerRecomputeWindow window = buffer.drainAndReset();
        assertThat(window.taskIds()).containsExactly("task-1");
        assertThat(window.namedUserIds()).containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void taskActivated_capturesTheTask_andTheAssigneeAndOwner() {
        TaskImpl task = new TaskImpl();
        task.setId("task-1");
        task.setAssignee("alice");
        task.setOwner("bob");

        capturer.capture(List.of(new CloudTaskActivatedEventImpl(task)));

        ConsumerRecomputeWindow window = buffer.drainAndReset();
        assertThat(window.taskIds()).containsExactly("task-1");
        assertThat(window.namedUserIds()).containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void taskCandidateUserAdded_capturesTheTaskAndTheUser() {
        capturer.capture(
            List.of(new CloudTaskCandidateUserAddedEventImpl(new TaskCandidateUserImpl("alice", "task-1")))
        );

        ConsumerRecomputeWindow window = buffer.drainAndReset();
        assertThat(window.taskIds()).containsExactly("task-1");
        assertThat(window.namedUserIds()).containsExactly("alice");
    }

    @Test
    void taskCandidateUserRemoved_capturesTheTaskAndTheUser() {
        capturer.capture(
            List.of(new CloudTaskCandidateUserRemovedEventImpl(new TaskCandidateUserImpl("alice", "task-1")))
        );

        ConsumerRecomputeWindow window = buffer.drainAndReset();
        assertThat(window.taskIds()).containsExactly("task-1");
        assertThat(window.namedUserIds()).containsExactly("alice");
    }

    @Test
    void taskCandidateGroupAdded_capturesTheTaskAndTheGroup() {
        capturer.capture(
            List.of(new CloudTaskCandidateGroupAddedEventImpl(new TaskCandidateGroupImpl("eng", "task-1")))
        );

        ConsumerRecomputeWindow window = buffer.drainAndReset();
        assertThat(window.taskIds()).containsExactly("task-1");
        assertThat(window.touchedGroupIds()).containsExactly("eng");
    }

    @Test
    void taskCandidateGroupRemoved_capturesTheTaskAndTheGroup() {
        capturer.capture(
            List.of(new CloudTaskCandidateGroupRemovedEventImpl(new TaskCandidateGroupImpl("eng", "task-1")))
        );

        ConsumerRecomputeWindow window = buffer.drainAndReset();
        assertThat(window.taskIds()).containsExactly("task-1");
        assertThat(window.touchedGroupIds()).containsExactly("eng");
    }

    @Test
    void processStarted_capturesTheProcessAndTheInitiator() {
        CloudProcessInstanceImpl process = new CloudProcessInstanceImpl();
        process.setId("proc-1");
        process.setInitiator("alice");

        capturer.capture(List.of(new CloudProcessStartedEventImpl(process)));

        ConsumerRecomputeWindow window = buffer.drainAndReset();
        assertThat(window.processInstanceIds()).containsExactly("proc-1");
        assertThat(window.namedInitiatorIds()).containsExactly("alice");
    }

    @Test
    void processCompleted_capturesTheProcessAndTheInitiator() {
        CloudProcessInstanceImpl process = new CloudProcessInstanceImpl();
        process.setId("proc-1");
        process.setInitiator("alice");

        capturer.capture(List.of(new CloudProcessCompletedEventImpl(process)));

        ConsumerRecomputeWindow window = buffer.drainAndReset();
        assertThat(window.processInstanceIds()).containsExactly("proc-1");
        assertThat(window.namedInitiatorIds()).containsExactly("alice");
    }

    @Test
    void processCancelled_capturesTheProcessAndTheInitiator() {
        CloudProcessInstanceImpl process = new CloudProcessInstanceImpl();
        process.setId("proc-1");
        process.setInitiator("alice");

        capturer.capture(List.of(new CloudProcessCancelledEventImpl(process)));

        ConsumerRecomputeWindow window = buffer.drainAndReset();
        assertThat(window.processInstanceIds()).containsExactly("proc-1");
        assertThat(window.namedInitiatorIds()).containsExactly("alice");
    }

    @Test
    void processSuspended_capturesTheProcessAndTheInitiator() {
        CloudProcessInstanceImpl process = new CloudProcessInstanceImpl();
        process.setId("proc-1");
        process.setInitiator("alice");

        capturer.capture(List.of(new CloudProcessSuspendedEventImpl(process)));

        ConsumerRecomputeWindow window = buffer.drainAndReset();
        assertThat(window.processInstanceIds()).containsExactly("proc-1");
        assertThat(window.namedInitiatorIds()).containsExactly("alice");
    }

    @Test
    void processResumed_capturesTheProcessAndTheInitiator() {
        CloudProcessInstanceImpl process = new CloudProcessInstanceImpl();
        process.setId("proc-1");
        process.setInitiator("alice");

        capturer.capture(List.of(new CloudProcessResumedEventImpl(process)));

        ConsumerRecomputeWindow window = buffer.drainAndReset();
        assertThat(window.processInstanceIds()).containsExactly("proc-1");
        assertThat(window.namedInitiatorIds()).containsExactly("alice");
    }

    @Test
    void unrelatedEventType_isIgnored() {
        CloudProcessInstanceImpl process = new CloudProcessInstanceImpl();
        process.setId("proc-1");

        capturer.capture(List.of(taskCreated("task-1"), new CloudProcessCreatedEventImpl(process)));

        ConsumerRecomputeWindow window = buffer.drainAndReset();
        assertThat(window.taskIds()).containsExactly("task-1");
        assertThat(window.processInstanceIds()).isEmpty();
    }

    private static CloudRuntimeEvent<?, ?> taskCreated(String taskId) {
        TaskImpl task = new TaskImpl();
        task.setId(taskId);
        return new CloudTaskCreatedEventImpl(task);
    }

    private static ConsumerSubscriberRegistry registeredRegistry() {
        ConsumerSubscriberRegistry registry = new ConsumerSubscriberRegistry();
        registry.register("watcher", Set.of(), "rest-1", T0);
        return registry;
    }
}
