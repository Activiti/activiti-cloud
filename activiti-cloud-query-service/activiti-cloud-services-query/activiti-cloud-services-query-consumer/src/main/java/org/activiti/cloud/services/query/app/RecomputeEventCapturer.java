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

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.TaskCandidateGroup;
import org.activiti.api.task.model.TaskCandidateUser;
import org.activiti.api.task.model.events.TaskCandidateGroupEvent;
import org.activiti.api.task.model.events.TaskCandidateUserEvent;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessRuntimeEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateGroupEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateUserEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskRuntimeEvent;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.QueryFeatureToggles;

/**
 * Maps each event a committed batch carries onto {@link ConsumerRecomputeBuffer} captures, reading
 * only what the event itself names - no queries. Irrelevant event types (variables, BPMN activity,
 * integration events, {@code PROCESS_CREATED} - a duplicate of {@code PROCESS_STARTED}, ...) are
 * ignored.
 */
public class RecomputeEventCapturer {

    private static final Set<String> CANDIDATE_USER_EVENT_TYPES = Set.of(
        TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_ADDED.name(),
        TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_REMOVED.name()
    );

    private static final Set<String> CANDIDATE_GROUP_EVENT_TYPES = Set.of(
        TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_ADDED.name(),
        TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_REMOVED.name()
    );

    private static final Set<String> PROCESS_EVENT_TYPES = Set.of(
        ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED.name(),
        ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED.name(),
        ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED.name(),
        ProcessRuntimeEvent.ProcessEvents.PROCESS_SUSPENDED.name(),
        ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED.name()
    );

    /** One entry per task event type this pipeline cares about; everything else falls through as a no-op. */
    private static final Map<String, TaskCapture> TASK_CAPTURES = Map.ofEntries(
        Map.entry(TaskRuntimeEvent.TaskEvents.TASK_CREATED.name(), (buffer, task, at) ->
            buffer.captureTask(task.getId(), at)
        ),
        Map.entry(TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED.name(), (buffer, task, at) ->
            buffer.captureTask(task.getId(), at, task.getAssignee(), task.getOwner())
        ),
        Map.entry(TaskRuntimeEvent.TaskEvents.TASK_COMPLETED.name(), (buffer, task, at) ->
            buffer.captureTask(task.getId(), at, task.getCompletedBy(), task.getAssignee(), task.getOwner())
        ),
        Map.entry(TaskRuntimeEvent.TaskEvents.TASK_CANCELLED.name(), (buffer, task, at) ->
            buffer.captureTask(task.getId(), at, task.getAssignee(), task.getOwner())
        ),
        Map.entry(TaskRuntimeEvent.TaskEvents.TASK_SUSPENDED.name(), (buffer, task, at) ->
            buffer.captureTask(task.getId(), at, task.getAssignee(), task.getOwner())
        ),
        Map.entry(TaskRuntimeEvent.TaskEvents.TASK_ACTIVATED.name(), (buffer, task, at) ->
            buffer.captureTask(task.getId(), at, task.getAssignee(), task.getOwner())
        )
    );

    private final ConsumerRecomputeBuffer buffer;
    private final FeatureToggle featureToggle;
    private final Clock clock;

    public RecomputeEventCapturer(ConsumerRecomputeBuffer buffer, FeatureToggle featureToggle, Clock clock) {
        this.buffer = buffer;
        this.featureToggle = featureToggle;
        this.clock = clock;
    }

    public void capture(List<CloudRuntimeEvent<?, ?>> events) {
        if (events == null || !featureToggle.isEnabled(QueryFeatureToggles.FEATURE_PUSHED_COUNTS)) {
            return;
        }
        Instant at = clock.instant();
        for (CloudRuntimeEvent<?, ?> event : events) {
            captureOne(event, at);
        }
    }

    private void captureOne(CloudRuntimeEvent<?, ?> event, Instant at) {
        String eventType = event.getEventType().name();
        TaskCapture taskCapture = TASK_CAPTURES.get(eventType);
        if (taskCapture != null) {
            taskCapture.capture(buffer, ((CloudTaskRuntimeEvent) event).getEntity(), at);
        } else if (CANDIDATE_USER_EVENT_TYPES.contains(eventType)) {
            TaskCandidateUser candidate = ((CloudTaskCandidateUserEvent) event).getEntity();
            buffer.captureTask(candidate.getTaskId(), at, candidate.getUserId());
        } else if (CANDIDATE_GROUP_EVENT_TYPES.contains(eventType)) {
            TaskCandidateGroup candidate = ((CloudTaskCandidateGroupEvent) event).getEntity();
            buffer.captureTaskCandidateGroup(candidate.getTaskId(), candidate.getGroupId(), at);
        } else if (PROCESS_EVENT_TYPES.contains(eventType)) {
            ProcessInstance process = ((CloudProcessRuntimeEvent) event).getEntity();
            buffer.captureProcess(process.getId(), process.getInitiator(), at);
        }
    }

    @FunctionalInterface
    private interface TaskCapture {
        void capture(ConsumerRecomputeBuffer buffer, Task task, Instant at);
    }
}
