/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.events.listeners;

import org.activiti.cloud.services.events.converter.ToCloudTaskRuntimeEventConverter;
import org.activiti.runtime.api.event.TaskActivated;
import org.activiti.runtime.api.event.TaskAssignedEvent;
import org.activiti.runtime.api.event.TaskCancelled;
import org.activiti.runtime.api.event.TaskCandidateGroupAddedEvent;
import org.activiti.runtime.api.event.TaskCandidateUserAddedEvent;
import org.activiti.runtime.api.event.TaskCompletedEvent;
import org.activiti.runtime.api.event.TaskCreatedEvent;
import org.activiti.runtime.api.event.TaskSuspendedEvent;
import org.activiti.runtime.api.event.listener.TaskRuntimeEventListener;

public class TaskRuntimeExternalEventsProducer implements TaskRuntimeEventListener {

    private ToCloudTaskRuntimeEventConverter converter;
    private ProcessEngineEventsAggregator eventsAggregator;

    public TaskRuntimeExternalEventsProducer(ToCloudTaskRuntimeEventConverter converter,
                                             ProcessEngineEventsAggregator eventsAggregator) {
        this.converter = converter;
        this.eventsAggregator = eventsAggregator;
    }

    @Override
    public void onTaskCreated(TaskCreatedEvent event) {
        eventsAggregator.add(converter.from(event));
    }

    @Override
    public void onTaskAssigned(TaskAssignedEvent event) {
        eventsAggregator.add(converter.from(event));
    }

    @Override
    public void onTaskSuspended(TaskSuspendedEvent event) {
        eventsAggregator.add(converter.from(event));
    }

    @Override
    public void onTaskActivated(TaskActivated event) {
        eventsAggregator.add(converter.from(event));
    }

    @Override
    public void onTaskCompleted(TaskCompletedEvent event) {
        eventsAggregator.add(converter.from(event));
    }

    @Override
    public void onTaskCancelled(TaskCancelled event) {
        eventsAggregator.add(converter.from(event));
    }

    @Override
    public void onTaskCandidateUserAdded(TaskCandidateUserAddedEvent event) {
        eventsAggregator.add(converter.from(event));
    }

    @Override
    public void onTaskCandidateGroupAdded(TaskCandidateGroupAddedEvent event) {
        eventsAggregator.add(converter.from(event));
    }
}
