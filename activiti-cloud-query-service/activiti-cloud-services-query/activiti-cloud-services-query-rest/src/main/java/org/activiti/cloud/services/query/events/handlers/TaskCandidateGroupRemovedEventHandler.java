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

import java.util.Optional;
import jakarta.persistence.EntityManager;
import org.activiti.api.task.model.TaskCandidateGroup;
import org.activiti.api.task.model.events.TaskCandidateGroupEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateGroupRemovedEvent;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateGroupId;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskCandidateGroupRemovedEventHandler implements QueryEventHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(TaskCandidateGroupRemovedEventHandler.class);
    private final EntityManager entityManager;
    private final EntityManagerFinder entityManagerFinder;

    public TaskCandidateGroupRemovedEventHandler(EntityManager entityManager, EntityManagerFinder entityManagerFinder) {
        this.entityManager = entityManager;
        this.entityManagerFinder = entityManagerFinder;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudTaskCandidateGroupRemovedEvent taskCandidateGroupRemovedEvent = (CloudTaskCandidateGroupRemovedEvent) event;
        TaskCandidateGroup taskCandidateGroup = taskCandidateGroupRemovedEvent.getEntity();
        Optional<TaskEntity> findResult = entityManagerFinder.findTaskWithCandidateGroups(
            taskCandidateGroup.getTaskId()
        );

        // if a task was cancelled / completed do not handle this event
        if (findResult.isPresent() && !findResult.get().isInFinalState()) {
            // Persist into database
            try {
                TaskCandidateGroupId id = new TaskCandidateGroupId(
                    taskCandidateGroup.getTaskId(),
                    taskCandidateGroup.getGroupId()
                );
                Optional
                    .ofNullable(entityManager.find(TaskCandidateGroupEntity.class, id))
                    .ifPresent(entityManager::remove);
            } catch (Exception cause) {
                LOGGER.debug("Error handling TaskCandidateGroupRemovedEvent[" + event + "]", cause);
            }
        }
    }

    @Override
    public String getHandledEvent() {
        return TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_REMOVED.name();
    }
}
