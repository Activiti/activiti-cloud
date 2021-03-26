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

import java.util.Optional;

import javax.persistence.EntityManager;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.events.CloudIntegrationEvent;
import org.activiti.cloud.services.query.app.repository.IntegrationContextRepository;
import org.activiti.cloud.services.query.app.repository.ServiceTaskRepository;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseIntegrationEventHandler {

    private final static Logger logger = LoggerFactory.getLogger(BaseIntegrationEventHandler.class);

    protected final IntegrationContextRepository integrationContextRepository;
    protected final ServiceTaskRepository serviceTaskRepository;
    protected final EntityManager entityManager;

    public BaseIntegrationEventHandler(IntegrationContextRepository integrationContextRepository,
                                       ServiceTaskRepository serviceTaskRepository,
                                       EntityManager entityManager) {
        this.integrationContextRepository = integrationContextRepository;
        this.serviceTaskRepository = serviceTaskRepository;
        this.entityManager = entityManager;
    }

    protected Optional<IntegrationContextEntity> findOrCreateIntegrationContextEntity(CloudIntegrationEvent event) {

        IntegrationContext integrationContext = event.getEntity();

        IntegrationContextEntity entity = integrationContextRepository.findByProcessInstanceIdAndClientIdAndExecutionId(integrationContext.getProcessInstanceId(),
                                                                                                                        integrationContext.getClientId(),
                                                                                                                        integrationContext.getExecutionId());
        // Let's create entity if does not exists
        if(entity == null) {
            ServiceTaskEntity serviceTaskEntity = serviceTaskRepository.findByProcessInstanceIdAndElementIdAndExecutionId(integrationContext.getProcessInstanceId(),
                                                                                                                             integrationContext.getClientId(),
                                                                                                                             integrationContext.getExecutionId());
            if (serviceTaskEntity != null) {
                logger.debug("Found BPMNActivityEntity: {}", serviceTaskEntity);

                entity = new IntegrationContextEntity(event.getServiceName(),
                                                      event.getServiceFullName(),
                                                      event.getServiceVersion(),
                                                      event.getAppName(),
                                                      event.getAppVersion());
                // Let use event id to persist integration context
                entity.setId(serviceTaskEntity.getId());
                entity.setClientId(integrationContext.getClientId());
                entity.setClientName(integrationContext.getClientName());
                entity.setClientType(integrationContext.getClientType());
                entity.setConnectorType(integrationContext.getConnectorType());
                entity.setProcessDefinitionId(integrationContext.getProcessDefinitionId());
                entity.setProcessInstanceId(integrationContext.getProcessInstanceId());
                entity.setExecutionId(integrationContext.getExecutionId());
                entity.setProcessDefinitionKey(integrationContext.getProcessDefinitionKey());
                entity.setProcessDefinitionVersion(integrationContext.getProcessDefinitionVersion());
                entity.setBusinessKey(integrationContext.getBusinessKey());
                entity.setServiceTask(serviceTaskEntity);

                entityManager.persist(entity);
            } else {
                logger.error("Cannot find BPMNActivityEntity for integrationContext: {}", integrationContext);
            }
        }

        return Optional.ofNullable(entity);
    }

}
