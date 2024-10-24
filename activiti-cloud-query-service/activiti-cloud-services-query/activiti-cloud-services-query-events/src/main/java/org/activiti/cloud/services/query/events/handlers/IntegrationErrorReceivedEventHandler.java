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

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.activiti.api.process.model.events.IntegrationEvent.IntegrationEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.activiti.cloud.api.process.model.CloudIntegrationContext.IntegrationContextStatus;
import org.activiti.cloud.api.process.model.events.CloudIntegrationErrorReceivedEvent;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.apache.commons.lang3.ObjectUtils;

public class IntegrationErrorReceivedEventHandler extends BaseIntegrationEventHandler implements QueryEventHandler {

    public IntegrationErrorReceivedEventHandler(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudIntegrationErrorReceivedEvent integrationEvent = CloudIntegrationErrorReceivedEvent.class.cast(event);

        Optional<IntegrationContextEntity> result = findIntegrationContextEntity(integrationEvent);

        result.ifPresent(entity -> {
            entity.setErrorDate(new Date(integrationEvent.getTimestamp()));
            entity.setStatus(IntegrationContextStatus.INTEGRATION_ERROR_RECEIVED);
            entity.setErrorCode(integrationEvent.getErrorCode());
            entity.setErrorMessage(integrationEvent.getErrorMessage());
            entity.setErrorClassName(integrationEvent.getErrorClassName());
            entity.setStackTraceElements(addFullErrorMessageAsFirstStackTraceElement(integrationEvent));
            entity.setInBoundVariables(integrationEvent.getEntity().getInBoundVariables());
            entity.setOutBoundVariables(integrationEvent.getEntity().getOutBoundVariables());

            entityManager.persist(entity);

            ServiceTaskEntity serviceTaskEntity = entityManager.find(ServiceTaskEntity.class, entity.getId());
            serviceTaskEntity.setStatus(CloudBPMNActivity.BPMNActivityStatus.ERROR);

            entityManager.persist(serviceTaskEntity);
        });
    }

    private List<StackTraceElement> addFullErrorMessageAsFirstStackTraceElement(
        CloudIntegrationErrorReceivedEvent integrationEvent
    ) {
        StackTraceElement newTopStackTraceElement = getNewTopStackTraceElement(integrationEvent);
        List<StackTraceElement> stackTraceElements = new ArrayList<>(List.of(newTopStackTraceElement));
        stackTraceElements.addAll(integrationEvent.getStackTraceElements());
        return stackTraceElements;
    }

    private StackTraceElement getNewTopStackTraceElement(CloudIntegrationErrorReceivedEvent integrationEvent) {
        if (ObjectUtils.isNotEmpty(integrationEvent.getStackTraceElements())) {
            StackTraceElement firstElement = integrationEvent.getStackTraceElements().get(0);
            return new StackTraceElement(
                integrationEvent.getErrorMessage(),
                "",
                firstElement.getFileName(),
                firstElement.getLineNumber()
            );
        } else {
            return new StackTraceElement(integrationEvent.getErrorMessage(), "", "", 0);
        }
    }

    @Override
    public String getHandledEvent() {
        return IntegrationEvents.INTEGRATION_ERROR_RECEIVED.name();
    }
}
