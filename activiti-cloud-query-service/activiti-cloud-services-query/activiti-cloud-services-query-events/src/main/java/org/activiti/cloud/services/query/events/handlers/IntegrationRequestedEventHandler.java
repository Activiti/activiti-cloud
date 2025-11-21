/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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

import jakarta.persistence.EntityManager;
import java.util.Date;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.process.model.events.IntegrationEvent.IntegrationEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.activiti.cloud.api.process.model.CloudIntegrationContext.IntegrationContextStatus;
import org.activiti.cloud.api.process.model.events.CloudIntegrationRequestedEvent;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;

public class IntegrationRequestedEventHandler extends BaseIntegrationEventHandler implements QueryEventHandler {

    public IntegrationRequestedEventHandler(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudIntegrationRequestedEvent integrationEvent = CloudIntegrationRequestedEvent.class.cast(event);
        IntegrationContext integrationContext = integrationEvent.getEntity();
        String entityId = integrationContext.getId();

        // Activity can be cyclical, so instead of using aggregation of process instance id, client id and execution id (like we did before),
        // we use the integration context id as primary key
        IntegrationContextEntity entity = entityManager.find(IntegrationContextEntity.class, entityId);
        if (entity == null) {
            entity =
                new IntegrationContextEntity(
                    event.getServiceName(),
                    event.getServiceFullName(),
                    event.getServiceVersion(),
                    event.getAppName(),
                    event.getAppVersion()
                );
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

        ServiceTaskEntity serviceTaskEntity = createServiceTaskEntity(integrationEvent, event);
        entity.setServiceTask(serviceTaskEntity);

        entityManager.persist(entity);
    }

    @Override
    public String getHandledEvent() {
        return IntegrationEvents.INTEGRATION_REQUESTED.name();
    }

    private ServiceTaskEntity createServiceTaskEntity(
        CloudIntegrationRequestedEvent integrationEvent,
        CloudRuntimeEvent<?, ?> event
    ) {
        IntegrationContext integrationContext = integrationEvent.getEntity();

        ServiceTaskEntity serviceTaskEntity = new ServiceTaskEntity(
            event.getServiceName(),
            event.getServiceFullName(),
            event.getServiceVersion(),
            event.getAppName(),
            event.getAppVersion()
        );
        serviceTaskEntity.setId(integrationContext.getId());
        serviceTaskEntity.setElementId(integrationContext.getClientId());
        serviceTaskEntity.setActivityName(integrationContext.getClientName());
        serviceTaskEntity.setActivityType("serviceTask");
        serviceTaskEntity.setProcessDefinitionId(integrationContext.getProcessDefinitionId());
        serviceTaskEntity.setProcessInstanceId(integrationContext.getProcessInstanceId());
        serviceTaskEntity.setExecutionId(integrationContext.getExecutionId());
        serviceTaskEntity.setProcessDefinitionKey(integrationContext.getProcessDefinitionKey());
        serviceTaskEntity.setProcessDefinitionVersion(integrationContext.getProcessDefinitionVersion());
        serviceTaskEntity.setBusinessKey(integrationContext.getBusinessKey());
        serviceTaskEntity.setStatus(CloudBPMNActivity.BPMNActivityStatus.STARTED);
        serviceTaskEntity.setStartedDate(new Date(event.getTimestamp()));
        serviceTaskEntity.setCompletedDate(null);

        return serviceTaskEntity;
    }
}
