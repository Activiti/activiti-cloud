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
import org.activiti.cloud.services.api.model.TaskCandidateGroup;

public class MockTaskCandidateGroupEvent extends MockProcessEngineEvent {
    private TaskCandidateGroup taskCandidateGroup;

    public MockTaskCandidateGroupEvent(Long timestamp, String eventType) {
        super(timestamp,
                eventType);
    }

    public static ProcessEngineEvent[] aTaskCandidateGroupAddedEvent(long timestamp,
                                                         TaskCandidateGroup taskCandidateGroup,
                                                         String processInstanceId) {
        MockTaskCandidateGroupEvent taskCandidateGroupAddedEvent = new MockTaskCandidateGroupEvent(timestamp,
                "TaskCandidateGroupAddedEvent");
        taskCandidateGroupAddedEvent.setTaskCandidateGroup(taskCandidateGroup);
        taskCandidateGroupAddedEvent.setProcessInstanceId(processInstanceId);
        ProcessEngineEvent[] events = {taskCandidateGroupAddedEvent};
        return events;
    }

    public static ProcessEngineEvent[] aTaskCandidateGroupRemovedEvent(long timestamp,
                                                                    TaskCandidateGroup taskCandidateGroup,
                                                                    String processInstanceId) {
        MockTaskCandidateGroupEvent taskCandidateGroupRemovedEvent = new MockTaskCandidateGroupEvent(timestamp,
                "TaskCandidateGroupRemovedEvent");
        taskCandidateGroupRemovedEvent.setTaskCandidateGroup(taskCandidateGroup);
        taskCandidateGroupRemovedEvent.setProcessInstanceId(processInstanceId);
        ProcessEngineEvent[] events = {taskCandidateGroupRemovedEvent};
        return events;
    }

    public TaskCandidateGroup getTaskCandidateGroup() {
        return taskCandidateGroup;
    }

    public void setTaskCandidateGroup(TaskCandidateGroup taskCandidateGroup) {
        this.taskCandidateGroup = taskCandidateGroup;
    }
}
