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

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.process.model.events.IntegrationEvent.IntegrationEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.activiti.cloud.api.process.model.CloudIntegrationContext.IntegrationContextStatus;
import org.activiti.cloud.api.process.model.events.CloudIntegrationRequestedEvent;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.Date;

public class IntegrationRequestedEventHandler extends BaseIntegrationEventHandler implements QueryEventHandler {

    private final static Logger logger = LoggerFactory.getLogger(IntegrationRequestedEventHandler.class);

    public IntegrationRequestedEventHandler(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudIntegrationRequestedEvent integrationEvent = CloudIntegrationRequestedEvent.class.cast(event);
        IntegrationContext integrationContext = integrationEvent.getEntity();
        String entityId = IntegrationContextEntity.IdBuilderHelper.from(integrationContext);


        // Activity can be cyclical, so try to find existing before creating a new one
        IntegrationContextEntity entity = entityManager.find(IntegrationContextEntity.class, entityId);
        if (entity == null) {
            entity = new IntegrationContextEntity(event.getServiceName(),
                event.getServiceFullName(),
                event.getServiceVersion(),
                event.getAppName(),
                event.getAppVersion());
            entity.setId(entityId);
            entity.setClientId(integrationContext.getClientId());
        }
        entity.setClientName(integrationContext.getClientName());
        entity.setClientType(integrationContext.getClientType());
        entity.setConnectorType(integrationContext.getConnectorType());
        entity.setProcessDefinitionId(integrationContext.getProcessDefinitionId());
        entity.setProcessInstanceId(integrationContext.getProcessInstanceId());
        entity.setRootProcessInstanceId(integrationContext.getRootProcessInstanceId());
        entity.setExecutionId(integrationContext.getExecutionId());
        entity.setProcessDefinitionKey(integrationContext.getProcessDefinitionKey());
        entity.setProcessDefinitionVersion(integrationContext.getProcessDefinitionVersion());
        entity.setBusinessKey(integrationContext.getBusinessKey());
        entity.setRequestDate(new Date(integrationEvent.getTimestamp()));
        entity.setStatus(IntegrationContextStatus.INTEGRATION_REQUESTED);
        entity.setInBoundVariables(integrationEvent.getEntity().getInBoundVariables());

        ServiceTaskEntity serviceTaskEntity = entityManager.find(ServiceTaskEntity.class, entityId);
        serviceTaskEntity.setStatus(CloudBPMNActivity.BPMNActivityStatus.STARTED);
        serviceTaskEntity.setStartedDate(new Date(event.getTimestamp()));
        serviceTaskEntity.setCompletedDate(null);

        entity.setServiceTask(serviceTaskEntity);

        entityManager.persist(entity);
    }

    @Override
    public String getHandledEvent() {
        return IntegrationEvents.INTEGRATION_REQUESTED.name();
    }
}
