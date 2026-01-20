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
import java.util.Date;
import java.util.Optional;
import org.activiti.api.process.model.events.IntegrationEvent.IntegrationEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudIntegrationContext.IntegrationContextStatus;
import org.activiti.cloud.api.process.model.events.CloudIntegrationResultReceivedEvent;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;

public class IntegrationResultReceivedEventHandler extends BaseIntegrationEventHandler implements QueryEventHandler {

    public IntegrationResultReceivedEventHandler(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudIntegrationResultReceivedEvent integrationEvent = CloudIntegrationResultReceivedEvent.class.cast(event);

        Optional<IntegrationContextEntity> result = findIntegrationContextEntity(integrationEvent);
        boolean isNewEntity = result.isEmpty();

        // If entity doesn't exist (e.g., purged during migration), create a new one with the new PK format
        IntegrationContextEntity entity = result.orElseGet(() -> createMissingIntegrationContextEntity(integrationEvent)
        );

        String serviceTaskId = IntegrationContextEntity.IdBuilderHelper.from(integrationEvent.getEntity());
        ServiceTaskEntity serviceTaskEntity = entityManager.find(ServiceTaskEntity.class, serviceTaskId);

        if (serviceTaskEntity != null && entity.getServiceTask() == null) {
            entity.setServiceTask(serviceTaskEntity);

            // Increment counter if this is a newly created entity
            if (isNewEntity) {
                serviceTaskEntity.incrementIntegrationContextCounter();
            }
        }

        entity.setResultDate(new Date(integrationEvent.getTimestamp()));
        entity.setStatus(IntegrationContextStatus.INTEGRATION_RESULT_RECEIVED);
        entity.setOutBoundVariables(integrationEvent.getEntity().getOutBoundVariables());

        entityManager.persist(entity);
    }

    @Override
    public String getHandledEvent() {
        return IntegrationEvents.INTEGRATION_RESULT_RECEIVED.name();
    }
}
