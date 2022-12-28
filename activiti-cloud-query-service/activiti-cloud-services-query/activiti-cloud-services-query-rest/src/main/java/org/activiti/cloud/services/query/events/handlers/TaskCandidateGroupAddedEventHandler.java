/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.services.query.events.handlers;

import org.activiti.api.task.model.TaskCandidateGroup;
import org.activiti.api.task.model.events.TaskCandidateGroupEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateGroupAddedEvent;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateGroupId;

import javax.persistence.EntityManager;

public class TaskCandidateGroupAddedEventHandler implements QueryEventHandler {

    private final EntityManager entityManager;

    public TaskCandidateGroupAddedEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {

        CloudTaskCandidateGroupAddedEvent taskCandidateGroupAddedEvent = (CloudTaskCandidateGroupAddedEvent) event;
        TaskCandidateGroup taskCandidateGroup = taskCandidateGroupAddedEvent.getEntity();
        TaskCandidateGroupEntity taskCandidateGroupEntity = new TaskCandidateGroupEntity(taskCandidateGroup.getTaskId(),
                                                                                         taskCandidateGroup.getGroupId());

        try {
            if (!taskCandidateEntityAlreadyExists(taskCandidateGroupEntity)) {
                entityManager.persist(taskCandidateGroupEntity);
            }
        } catch (Exception cause) {
            throw new QueryException("Error handling TaskCandidateGroupAddedEvent[" + event + "]",
                                     cause);
        }
    }

    @Override
    public String getHandledEvent() {
        return TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_ADDED.name();
    }

    private boolean taskCandidateEntityAlreadyExists(TaskCandidateGroupEntity entity) {
        return entityManager.find(TaskCandidateGroupEntity.class,
                                  new TaskCandidateGroupId(entity.getTaskId(), entity.getGroupId())) != null;
    }
}
