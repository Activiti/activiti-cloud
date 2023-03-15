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

import javax.persistence.EntityManager;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.events.ProcessDefinitionEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessDeployedEvent;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.query.model.ProcessModelEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessDeployedEventHandler implements QueryEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessDeployedEventHandler.class);

    private EntityManager entityManager;

    public ProcessDeployedEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudProcessDeployedEvent processDeployedEvent = CloudProcessDeployedEvent.class.cast(event);
        ProcessDefinition processDefinition = processDeployedEvent.getEntity();
        LOGGER.debug("Handling process deployed event for " + processDefinition.getKey());
        ProcessDefinitionEntity processDefinitionEntity = new ProcessDefinitionEntity(
            processDeployedEvent.getServiceName(),
            processDeployedEvent.getServiceFullName(),
            processDeployedEvent.getServiceVersion(),
            processDeployedEvent.getAppName(),
            processDeployedEvent.getAppVersion()
        );
        processDefinitionEntity.setId(processDefinition.getId());
        processDefinitionEntity.setDescription(processDefinition.getDescription());
        processDefinitionEntity.setFormKey(processDefinition.getFormKey());
        processDefinitionEntity.setKey(processDefinition.getKey());
        processDefinitionEntity.setName(processDefinition.getName());
        processDefinitionEntity.setVersion(processDefinition.getVersion());
        processDefinitionEntity.setCategory(processDefinition.getCategory());
        processDefinitionEntity.setServiceType(processDeployedEvent.getServiceType());
        entityManager.merge(processDefinitionEntity);

        ProcessModelEntity processModelEntity = new ProcessModelEntity(
            processDefinitionEntity,
            processDeployedEvent.getProcessModelContent()
        );
        processModelEntity.setId(processDefinitionEntity.getId());
        entityManager.merge(processModelEntity);
    }

    @Override
    public String getHandledEvent() {
        return ProcessDefinitionEvent.ProcessDefinitionEvents.PROCESS_DEPLOYED.name();
    }
}
