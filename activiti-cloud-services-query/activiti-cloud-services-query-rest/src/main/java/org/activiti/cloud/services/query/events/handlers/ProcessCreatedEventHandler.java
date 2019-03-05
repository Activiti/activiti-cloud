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

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCreatedEvent;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessCreatedEventHandler implements QueryEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessCreatedEventHandler.class);
    private ProcessInstanceRepository processInstanceRepository;

    public ProcessCreatedEventHandler(ProcessInstanceRepository processInstanceRepository) {
        this.processInstanceRepository = processInstanceRepository;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudProcessCreatedEvent createdEvent = (CloudProcessCreatedEvent) event;
        LOGGER.debug("Handling created process Instance " + createdEvent.getEntity().getId());

        ProcessInstanceEntity createdProcessInstanceEntity = new ProcessInstanceEntity();
        createdProcessInstanceEntity.setServiceName(createdEvent.getServiceName());
        createdProcessInstanceEntity.setServiceFullName(createdEvent.getServiceFullName());
        createdProcessInstanceEntity.setServiceVersion(createdEvent.getServiceVersion());
        createdProcessInstanceEntity.setAppName(createdEvent.getAppName());
        createdProcessInstanceEntity.setAppVersion(createdEvent.getAppVersion());

        createdProcessInstanceEntity.setProcessDefinitionId(createdEvent.getEntity().getProcessDefinitionId());
        createdProcessInstanceEntity.setId(createdEvent.getEntity().getId());
        createdProcessInstanceEntity.setStatus(ProcessInstance.ProcessInstanceStatus.CREATED);
        createdProcessInstanceEntity.setLastModified(new Date(createdEvent.getTimestamp()));
        createdProcessInstanceEntity.setName(createdEvent.getEntity().getName());

        createdProcessInstanceEntity.setProcessDefinitionKey(createdEvent.getEntity().getProcessDefinitionKey());
        createdProcessInstanceEntity.setInitiator(createdEvent.getEntity().getInitiator());
        createdProcessInstanceEntity.setBusinessKey(createdEvent.getEntity().getBusinessKey());
        createdProcessInstanceEntity.setStartDate(createdEvent.getEntity().getStartDate());
        
        createdProcessInstanceEntity.setParentId(createdEvent.getEntity().getParentId());
        createdProcessInstanceEntity.setProcessDefinitionVersion(createdEvent.getEntity().getProcessDefinitionVersion());
        
        processInstanceRepository.save(createdProcessInstanceEntity);
    }

    @Override
    public String getHandledEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED.name();
    }
}
