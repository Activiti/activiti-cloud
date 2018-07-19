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

import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskCandidateUser;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.CloudTaskCandidateUserAddedEvent;
import org.activiti.runtime.api.event.TaskCandidateUserEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskCandidateUserAddedEventHandler implements QueryEventHandler {

    private final TaskCandidateUserRepository taskCandidateUserRepository;

    @Autowired
    public TaskCandidateUserAddedEventHandler(TaskCandidateUserRepository taskCandidateUserRepository) {
        this.taskCandidateUserRepository = taskCandidateUserRepository;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudTaskCandidateUserAddedEvent taskCandidateUserAddedEvent = (CloudTaskCandidateUserAddedEvent) event;
        org.activiti.runtime.api.model.TaskCandidateUser taskCandidateUser = taskCandidateUserAddedEvent.getEntity();

        try {
            taskCandidateUserRepository.save(new TaskCandidateUser(taskCandidateUser.getTaskId(),
                                                                   taskCandidateUser.getUserId()));
        } catch (Exception cause) {
            throw new QueryException("Error handling TaskCandidateUserAddedEvent[" + event + "]",
                                     cause);
        }
    }

    @Override
    public String getHandledEvent() {
        return TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_ADDED.name();
    }
}
