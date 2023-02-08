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

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.task.model.Task.TaskStatus;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCompletedEvent;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskEntity;

public class ProcessCompletedEventHandler implements QueryEventHandler {

    private final EntityManager entityManager;

    public ProcessCompletedEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudProcessCompletedEvent completedEvent = (CloudProcessCompletedEvent) event;
        String processInstanceId = completedEvent.getEntity().getId();
        Optional<ProcessInstanceEntity> findResult = Optional.ofNullable(entityManager.find(ProcessInstanceEntity.class,
                                                                                            processInstanceId));
        if (findResult.isPresent()) {
            ProcessInstanceEntity processInstanceEntity = findResult.get();
            processInstanceEntity.setStatus(ProcessInstance.ProcessInstanceStatus.COMPLETED);
            processInstanceEntity.setLastModified(new Date(completedEvent.getTimestamp()));
            processInstanceEntity.setCompletedDate(new Date(completedEvent.getTimestamp()));
            entityManager.persist(processInstanceEntity);

            markAllChildTasksAsCancelledWhenTheyAreAssignedOrCreated(processInstanceEntity);
        } else {
            throw new QueryException("Unable to find process instance with the given id: " + processInstanceId);
        }
    }

    private void markAllChildTasksAsCancelledWhenTheyAreAssignedOrCreated(ProcessInstanceEntity processInstanceEntity) {
        Predicate<TaskEntity> cancellableTasks = task -> TaskStatus.ASSIGNED.equals(task.getStatus())
            || TaskStatus.CREATED.equals(task.getStatus());
        Stream.ofNullable(processInstanceEntity.getTasks())
            .flatMap(Set::stream)
            .filter(cancellableTasks)
            .peek(task -> task.setStatus(TaskStatus.CANCELLED))
            .forEach(entityManager::persist);
    }

    @Override
    public String getHandledEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED.name();
    }
}
