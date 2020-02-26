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

import org.activiti.api.task.model.events.TaskCandidateGroupEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateGroupAddedEvent;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskCandidateGroup;

public class TaskCandidateGroupAddedEventHandler implements QueryEventHandler {

    private final TaskCandidateGroupRepository taskCandidateGroupRepository;

    public TaskCandidateGroupAddedEventHandler(TaskCandidateGroupRepository taskCandidateGroupRepository) {
        this.taskCandidateGroupRepository = taskCandidateGroupRepository;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {

        CloudTaskCandidateGroupAddedEvent taskCandidateGroupAddedEvent = (CloudTaskCandidateGroupAddedEvent) event;
        TaskCandidateGroup taskCandidateGroup = new TaskCandidateGroup(taskCandidateGroupAddedEvent.getEntity().getTaskId(),
                                                                       taskCandidateGroupAddedEvent.getEntity().getGroupId());

        // not going to look up task as candidate can be created before task

        // Persist into database
        try {
            taskCandidateGroupRepository.save(taskCandidateGroup);
        } catch (Exception cause) {
            throw new QueryException("Error handling TaskCandidateGroupAddedEvent[" + event + "]",
                                     cause);
        }
    }

    @Override
    public String getHandledEvent() {
        return TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_ADDED.name();
    }
}
