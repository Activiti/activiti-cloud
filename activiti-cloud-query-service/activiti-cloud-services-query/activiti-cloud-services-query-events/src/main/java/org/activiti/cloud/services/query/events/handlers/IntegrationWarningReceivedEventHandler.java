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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.activiti.api.process.model.events.IntegrationEvent.IntegrationEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.activiti.cloud.api.process.model.CloudIntegrationContext.IntegrationContextStatus;
import org.activiti.cloud.api.process.model.events.CloudIntegrationWarningReceivedEvent;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegrationWarningReceivedEventHandler extends BaseIntegrationEventHandler implements QueryEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationWarningReceivedEventHandler.class);

    public IntegrationWarningReceivedEventHandler(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudIntegrationWarningReceivedEvent warningEvent =
            CloudIntegrationWarningReceivedEvent.class.cast(event);

        LOGGER.debug("Handling integration warning: code={}, message={}",
            warningEvent.getWarningCode(), warningEvent.getWarningMessage());

        // No status change, no error date, no service task status update
        // Just persist the warning context for audit/query purposes
        Optional<IntegrationContextEntity> result = findIntegrationContextEntity(warningEvent);
        IntegrationContextEntity entity = result.orElseGet(
            () -> createMissingIntegrationContextEntity(warningEvent)
        );

        entity.setInBoundVariables(warningEvent.getEntity().getInBoundVariables());
        entity.setOutBoundVariables(warningEvent.getEntity().getOutBoundVariables());

        entityManager.persist(entity);
    }

    @Override
    public String getHandledEvent() {
        return IntegrationEvents.INTEGRATION_WARNING_RECEIVED.name();
    }
}

