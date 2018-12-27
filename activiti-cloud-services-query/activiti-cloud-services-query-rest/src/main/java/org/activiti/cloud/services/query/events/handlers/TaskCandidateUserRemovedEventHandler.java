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

package org.activiti.cloud.services.query.events.handlers;

import org.activiti.api.task.model.events.TaskCandidateUserEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateUserRemovedEvent;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskCandidateUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskCandidateUserRemovedEventHandler implements QueryEventHandler {

    private final TaskCandidateUserRepository taskCandidateUserRepository;

    @Autowired
    public TaskCandidateUserRemovedEventHandler(TaskCandidateUserRepository taskCandidateUserRepository) {
        this.taskCandidateUserRepository = taskCandidateUserRepository;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudTaskCandidateUserRemovedEvent taskCandidateUserRemovedEvent = (CloudTaskCandidateUserRemovedEvent) event;
        org.activiti.api.task.model.TaskCandidateUser taskCandidateUser = taskCandidateUserRemovedEvent.getEntity();
 
        try {
            taskCandidateUserRepository.delete(new TaskCandidateUser(taskCandidateUser.getTaskId(),
                                                                     taskCandidateUser.getUserId()));
        } catch (Exception cause) {
            throw new QueryException("Error handling TaskCandidateUserRemovedEvent[" + event + "]",
                                     cause);
        }
    }

    @Override
    public String getHandledEvent() {
        return TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_REMOVED.name();
    }
}
