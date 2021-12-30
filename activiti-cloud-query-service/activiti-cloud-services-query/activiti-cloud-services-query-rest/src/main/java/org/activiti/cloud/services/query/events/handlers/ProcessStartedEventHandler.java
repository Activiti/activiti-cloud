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

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessStartedEvent;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.Optional;

public class ProcessStartedEventHandler implements QueryEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessStartedEventHandler.class);

    private final EntityManager entityManager;

    public ProcessStartedEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudProcessStartedEvent startedEvent = (CloudProcessStartedEvent) event;
        String processInstanceId = startedEvent.getEntity().getId();
        LOGGER.debug("Handling start of process Instance " + processInstanceId);

        Optional<ProcessInstanceEntity> findResult = Optional.ofNullable(entityManager.getReference(ProcessInstanceEntity.class,
                                                                                                    processInstanceId));
        ProcessInstanceEntity processInstanceEntity = findResult.orElseThrow(
                () -> new QueryException("Unable to find process instance with the given id: " + processInstanceId));

        if (ProcessInstance.ProcessInstanceStatus.CREATED.equals(processInstanceEntity.getStatus())) {
            processInstanceEntity.setStatus(ProcessInstance.ProcessInstanceStatus.RUNNING);
            //instance name is not available in ProcessCreatedEvent, so we need to updated it here
            processInstanceEntity.setName(startedEvent.getEntity().getName());
            processInstanceEntity.setLastModified(new Date(startedEvent.getTimestamp()));
            processInstanceEntity.setStartDate(startedEvent.getEntity().getStartDate());

            entityManager.persist(processInstanceEntity);
        }
    }

    @Override
    public String getHandledEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED.name();
    }
}
