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

import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.events.ProcessDefinitionEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessDeployedEvent;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.app.repository.ProcessModelRepository;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.query.model.ProcessModelEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessDeployedEventHandler implements QueryEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessDeployedEventHandler.class);

    private ProcessDefinitionRepository processDefinitionRepository;
    private ProcessModelRepository processModelRepository;

    public ProcessDeployedEventHandler(ProcessDefinitionRepository processDefinitionRepository,
                                       ProcessModelRepository processModelRepository) {
        this.processDefinitionRepository = processDefinitionRepository;
        this.processModelRepository = processModelRepository;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudProcessDeployedEvent processDeployedEvent = (CloudProcessDeployedEvent) event;
        ProcessDefinition eventProcessDefinition = processDeployedEvent.getEntity();
        LOGGER.debug("Handling process deployed event for " + eventProcessDefinition.getKey());
        ProcessDefinitionEntity processDefinition = new ProcessDefinitionEntity(processDeployedEvent.getServiceName(),
                                                                                      processDeployedEvent.getServiceFullName(),
                                                                                      processDeployedEvent.getServiceVersion(),
                                                                                      processDeployedEvent.getAppName(),
                                                                                      processDeployedEvent.getAppVersion());
        processDefinition.setId(eventProcessDefinition.getId());
        processDefinition.setDescription(eventProcessDefinition.getDescription());
        processDefinition.setFormKey(eventProcessDefinition.getFormKey());
        processDefinition.setKey(eventProcessDefinition.getKey());
        processDefinition.setName(eventProcessDefinition.getName());
        processDefinition.setVersion(eventProcessDefinition.getVersion());
        processDefinition.setServiceType(processDeployedEvent.getServiceType());
        processDefinitionRepository.save(processDefinition);

        ProcessModelEntity processModelEntity = new ProcessModelEntity(processDefinition,
                                                                       processDeployedEvent.getProcessModelContent());
        processModelEntity.setId(processDefinition.getId());
        processModelRepository.save(processModelEntity);
    }

    @Override
    public String getHandledEvent() {
        return ProcessDefinitionEvent.ProcessDefinitionEvents.PROCESS_DEPLOYED.name();
    }
}
