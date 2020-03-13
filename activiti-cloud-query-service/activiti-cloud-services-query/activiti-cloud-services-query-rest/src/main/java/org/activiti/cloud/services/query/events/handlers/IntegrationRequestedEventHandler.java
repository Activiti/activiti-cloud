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

import javax.persistence.EntityManager;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.process.model.events.IntegrationEvent.IntegrationEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudIntegrationRequestedEvent;
import org.activiti.cloud.services.query.app.repository.IntegrationContextRepository;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.IntegrationContextEntity.IntegrationContextStatus;
import org.activiti.cloud.services.query.model.QueryException;

public class IntegrationRequestedEventHandler implements QueryEventHandler {

    private final IntegrationContextRepository repository;
    private final EntityManager entityManager;

    public IntegrationRequestedEventHandler(IntegrationContextRepository repository,
                                            EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudIntegrationRequestedEvent integrationEvent = CloudIntegrationRequestedEvent.class.cast(event);

        IntegrationContext integrationContext = integrationEvent.getEntity();

        IntegrationContextEntity entity = repository.findByProcessInstanceIdAndClientId(integrationContext.getProcessInstanceId(),
                                                                                        integrationContext.getClientId());

        // Let's create entity if does not exists
        if(entity == null) {
            entity = new IntegrationContextEntity(event.getServiceName(),
                                                  event.getServiceFullName(),
                                                  event.getServiceVersion(),
                                                  event.getAppName(),
                                                  event.getAppVersion());
            // Let use event id to persist activity id
            entity.setId(event.getId());
            entity.setClientId(integrationContext.getClientId());
            entity.setClientName(integrationContext.getClientName());
            entity.setClientType(integrationContext.getClientType());
            entity.setConnectorType(integrationContext.getConnectorType());
            entity.setProcessDefinitionId(integrationContext.getProcessDefinitionId());
            entity.setProcessInstanceId(integrationContext.getProcessInstanceId());
            entity.setProcessDefinitionKey(integrationContext.getProcessDefinitionKey());
            entity.setProcessDefinitionVersion(integrationContext.getProcessDefinitionVersion());
            entity.setBusinessKey(integrationContext.getBusinessKey());
        }

        entity.setRequestDate(new Date(integrationEvent.getTimestamp()));
        entity.setStatus(IntegrationContextStatus.INTEGRATION_REQUESTED);
        entity.setOutBoundVariables(entity.getOutBoundVariables());

        persistIntoDatabase(event,
                            entity);

    }

    private void persistIntoDatabase(CloudRuntimeEvent<?, ?> event,
                                     IntegrationContextEntity entity) {
        try {
            repository.save(entity);
        } catch (Exception cause) {
            throw new QueryException("Error handling CloudIntegrationRequestedEvent[" + event + "]",
                                     cause);
        }
    }

    @Override
    public String getHandledEvent() {
        return IntegrationEvents.INTEGRATION_REQUESTED.name();
    }
}
