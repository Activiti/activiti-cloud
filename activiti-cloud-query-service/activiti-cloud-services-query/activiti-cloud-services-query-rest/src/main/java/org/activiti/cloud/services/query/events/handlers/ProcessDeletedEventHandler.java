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

import java.util.Set;
import javax.persistence.EntityManager;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessDeletedEvent;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QueryException;

public class ProcessDeletedEventHandler implements QueryEventHandler {

    protected final String INVALID_PROCESS_INSTANCE_STATE =
        "Process Instance %s is not in a valid state: %s. " +
        "Only process instances in status COMPLETED or CANCELLED can be deleted.";

    private Set<ProcessInstanceStatus> ALLOWED_STATUS = Set.of(
        ProcessInstanceStatus.CANCELLED,
        ProcessInstanceStatus.COMPLETED
    );

    private final EntityManager entityManager;
    private final EntityManagerFinder entityManagerFinder;

    public ProcessDeletedEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.entityManagerFinder = new EntityManagerFinder(entityManager);
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudProcessDeletedEvent deletedEvent = (CloudProcessDeletedEvent) event;

        ProcessInstance eventProcessInstance = deletedEvent.getEntity();

        ProcessInstanceEntity processInstanceEntity = entityManagerFinder
            .findProcessInstanceWithRelatedEntities(eventProcessInstance.getId())
            .orElseThrow(() ->
                new QueryException("Unable to find process instance with the given id: " + eventProcessInstance.getId())
            );

        if (ALLOWED_STATUS.contains(processInstanceEntity.getStatus())) {
            processInstanceEntity.getTasks().stream().forEach(entityManager::remove);
            processInstanceEntity.getVariables().stream().forEach(entityManager::remove);
            processInstanceEntity.getServiceTasks().stream().forEach(entityManager::remove);
            processInstanceEntity.getActivities().stream().forEach(entityManager::remove);
            processInstanceEntity.getSequenceFlows().stream().forEach(entityManager::remove);

            entityManager.remove(processInstanceEntity);
        } else {
            throw new IllegalStateException(
                String.format(
                    INVALID_PROCESS_INSTANCE_STATE,
                    processInstanceEntity.getId(),
                    processInstanceEntity.getStatus().name()
                )
            );
        }
    }

    @Override
    public String getHandledEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_DELETED.name();
    }
}
