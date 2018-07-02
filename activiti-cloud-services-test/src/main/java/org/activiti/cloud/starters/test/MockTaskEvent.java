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

package org.activiti.cloud.starters.test;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.api.model.Task;

public class MockTaskEvent extends MockProcessEngineEvent {

    private Task task;

    public MockTaskEvent(Long timestamp, String eventType) {
        super(timestamp,
              eventType);
    }

    public static ProcessEngineEvent[] aTaskCreatedEvent(long timestamp,
                                                         Task task,
                                                         String processInstanceId) {
        MockTaskEvent taskCreatedEvent = new MockTaskEvent(timestamp,
                                                           "TaskCreatedEvent");
        taskCreatedEvent.setTask(task);
        taskCreatedEvent.setProcessInstanceId(processInstanceId);
        ProcessEngineEvent[] events = {taskCreatedEvent};
        return events;
    }

    public static ProcessEngineEvent[] aTaskAssignedEvent(long timestamp,
                                                       Task task) {
        MockTaskEvent taskAssignedEvent = new MockTaskEvent(timestamp,
                                                           "TaskAssignedEvent");
        taskAssignedEvent.setTask(task);
        ProcessEngineEvent[] events = {taskAssignedEvent};
        return events;
    }

    public static ProcessEngineEvent[] aTaskCompletedEvent(long timestamp,
                                                       Task task) {
        MockTaskEvent taskCompletedEvent = new MockTaskEvent(timestamp,
                                                           "TaskCompletedEvent");
        taskCompletedEvent.setTask(task);
        ProcessEngineEvent[] events = {taskCompletedEvent};
        return events;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }
}
