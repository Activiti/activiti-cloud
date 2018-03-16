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

import java.util.Date;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.events.ProcessCreatedEvent;
import org.activiti.cloud.services.query.model.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProcessCreatedEventHandler implements QueryEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessCreatedEventHandler.class);
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    public ProcessCreatedEventHandler(ProcessInstanceRepository processInstanceRepository) {
        this.processInstanceRepository = processInstanceRepository;
    }

    @Override
    public void handle(ProcessEngineEvent createdEvent) {
        LOGGER.debug("Handling created process Instance " + createdEvent.getProcessInstanceId());

        ProcessInstance createdProcessInstance = new ProcessInstance();
        createdProcessInstance.setApplicationName(createdEvent.getApplicationName());
        createdProcessInstance.setProcessDefinitionId(createdEvent.getProcessDefinitionId());
        createdProcessInstance.setId(createdEvent.getProcessInstanceId());
        createdProcessInstance.setStatus("CREATED");
        createdProcessInstance.setLastModified(new Date(createdEvent.getTimestamp()));

        // Augment Query Process Instance with internal Process Instance from event
        if (((ProcessCreatedEvent) createdEvent).getProcessInstance() != null) {
            createdProcessInstance.setProcessDefinitionKey(((ProcessCreatedEvent) createdEvent).getProcessInstance().getProcessDefinitionKey());
            createdProcessInstance.setInitiator(((ProcessCreatedEvent) createdEvent).getProcessInstance().getInitiator());
            createdProcessInstance.setBusinessKey(((ProcessCreatedEvent) createdEvent).getProcessInstance().getBusinessKey());
            createdProcessInstance.setDescription(((ProcessCreatedEvent) createdEvent).getProcessInstance().getDescription());
            createdProcessInstance.setStartDate(((ProcessCreatedEvent) createdEvent).getProcessInstance().getStartDate());
        }

        processInstanceRepository.save(createdProcessInstance);
    }

    @Override
    public Class<? extends ProcessEngineEvent> getHandledEventClass() {
        return ProcessCreatedEvent.class;
    }
}
