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

import java.util.Date;
import java.util.Optional;
import javax.persistence.EntityManager;
import org.activiti.api.process.model.events.IntegrationEvent.IntegrationEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudIntegrationContext.IntegrationContextStatus;
import org.activiti.cloud.api.process.model.events.CloudIntegrationResultReceivedEvent;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;

public class IntegrationResultReceivedEventHandler extends BaseIntegrationEventHandler implements QueryEventHandler {

    public IntegrationResultReceivedEventHandler(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudIntegrationResultReceivedEvent integrationEvent = CloudIntegrationResultReceivedEvent.class.cast(event);

        Optional<IntegrationContextEntity> result = findIntegrationContextEntity(integrationEvent);

        result.ifPresent(entity -> {
            entity.setResultDate(new Date(integrationEvent.getTimestamp()));
            entity.setStatus(IntegrationContextStatus.INTEGRATION_RESULT_RECEIVED);
            entity.setOutBoundVariables(integrationEvent.getEntity().getOutBoundVariables());

            entityManager.persist(entity);
        });
    }

    @Override
    public String getHandledEvent() {
        return IntegrationEvents.INTEGRATION_RESULT_RECEIVED.name();
    }
}
