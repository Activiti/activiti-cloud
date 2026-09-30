/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
import java.util.Optional;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.events.CloudIntegrationEvent;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;

public abstract class BaseIntegrationEventHandler {

    protected final EntityManager entityManager;

    public BaseIntegrationEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected Optional<IntegrationContextEntity> findIntegrationContextEntity(CloudIntegrationEvent event) {
        IntegrationContext integrationContext = event.getEntity();

        IntegrationContextEntity entity = entityManager.find(
            IntegrationContextEntity.class,
            integrationContext.getId()
        );

        return Optional.ofNullable(entity);
    }

    protected IntegrationContextEntity createMissingIntegrationContextEntity(CloudIntegrationEvent integrationEvent) {
        IntegrationContext integrationContext = integrationEvent.getEntity();

        IntegrationContextEntity entity = new IntegrationContextEntity(
            integrationEvent.getServiceName(),
            integrationEvent.getServiceFullName(),
            integrationEvent.getServiceVersion(),
            integrationEvent.getAppName(),
            integrationEvent.getAppVersion()
        );

        entity.setId(integrationContext.getId());
        entity.setClientId(integrationContext.getClientId());
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
        entity.setInBoundVariables(integrationContext.getInBoundVariables());

        return entity;
    }

    protected ServiceTaskEntity linkServiceTaskAndHandleCounter(
        CloudIntegrationEvent integrationEvent,
        IntegrationContextEntity entity,
        boolean isNewEntity
    ) {
        String serviceTaskId = IntegrationContextEntity.IdBuilderHelper.from(integrationEvent.getEntity());
        ServiceTaskEntity serviceTaskEntity = entityManager.find(ServiceTaskEntity.class, serviceTaskId);

        if (serviceTaskEntity != null && entity.getServiceTask() == null) {
            entity.setServiceTask(serviceTaskEntity);

            if (isNewEntity) {
                serviceTaskEntity.incrementIntegrationContextCounter();
            }
        }
        return serviceTaskEntity;
    }
}
