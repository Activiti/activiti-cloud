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
import org.activiti.cloud.services.api.model.TaskCandidateUser;

public class MockTaskCandidateUserEvent extends MockProcessEngineEvent {
    private TaskCandidateUser taskCandidateUser;

    public MockTaskCandidateUserEvent(Long timestamp, String eventType) {
        super(timestamp,
                eventType);
    }

    public static ProcessEngineEvent[] aTaskCandidateUserAddedEvent(long timestamp,
                                                         TaskCandidateUser taskCandidateUser,
                                                         String processInstanceId) {
        MockTaskCandidateUserEvent taskCandidateUserAddedEvent = new MockTaskCandidateUserEvent(timestamp,
                "TaskCandidateUserAddedEvent");
        taskCandidateUserAddedEvent.setTaskCandidateUser(taskCandidateUser);
        taskCandidateUserAddedEvent.setProcessInstanceId(processInstanceId);
        ProcessEngineEvent[] events = {taskCandidateUserAddedEvent};
        return events;
    }

    public static ProcessEngineEvent[] aTaskCandidateUserRemovedEvent(long timestamp,
                                                                    TaskCandidateUser taskCandidateUser,
                                                                    String processInstanceId) {
        MockTaskCandidateUserEvent taskCandidateUserRemovedEvent = new MockTaskCandidateUserEvent(timestamp,
                "TaskCandidateUserRemovedEvent");
        taskCandidateUserRemovedEvent.setTaskCandidateUser(taskCandidateUser);
        taskCandidateUserRemovedEvent.setProcessInstanceId(processInstanceId);
        ProcessEngineEvent[] events = {taskCandidateUserRemovedEvent};
        return events;
    }

    public TaskCandidateUser getTaskCandidateUser() {
        return taskCandidateUser;
    }

    public void setTaskCandidateUser(TaskCandidateUser taskCandidateUser) {
        this.taskCandidateUser = taskCandidateUser;
    }
}
