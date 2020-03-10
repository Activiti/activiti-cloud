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

import javax.persistence.EntityManager;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.process.model.events.IntegrationEvent.IntegrationEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudIntegrationErrorReceivedEvent;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.BPMNActivityEntity.BPMNActivityStatus;
import org.activiti.cloud.services.query.model.QueryException;

public class IntegrationResultReceivedEventHandler implements QueryEventHandler {

    private final BPMNActivityRepository bpmnActivitiyRepository;
    private final EntityManager entityManager;

    public IntegrationResultReceivedEventHandler(BPMNActivityRepository activitiyRepository,
                                                 EntityManager entityManager) {
        this.bpmnActivitiyRepository = activitiyRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudIntegrationErrorReceivedEvent integrationEvent = CloudIntegrationErrorReceivedEvent.class.cast(event);

        IntegrationContext integrationContext = integrationEvent.getEntity();

        BPMNActivityEntity bpmnActivityEntity = bpmnActivitiyRepository.findByProcessInstanceIdAndElementId(integrationContext.getProcessInstanceId(),
                                                                                                            integrationContext.getClientId());

        // Let's create entity if does not exists
        if(bpmnActivityEntity == null) {
            bpmnActivityEntity = new BPMNActivityEntity(event.getServiceName(),
                                                        event.getServiceFullName(),
                                                        event.getServiceVersion(),
                                                        event.getAppName(),
                                                        event.getAppVersion());
            // Let use event id to persist activity id
            bpmnActivityEntity.setId(event.getId());
            bpmnActivityEntity.setElementId(integrationContext.getClientId());
            bpmnActivityEntity.setActivityName(integrationContext.getClientName());
            bpmnActivityEntity.setActivityType(integrationContext.getClientType());
            bpmnActivityEntity.setProcessDefinitionId(integrationContext.getProcessDefinitionId());
            bpmnActivityEntity.setProcessInstanceId(integrationContext.getProcessInstanceId());
            bpmnActivityEntity.setProcessDefinitionKey(integrationContext.getProcessDefinitionKey());
            bpmnActivityEntity.setProcessDefinitionVersion(integrationContext.getProcessDefinitionVersion());
            bpmnActivityEntity.setBusinessKey(integrationContext.getBusinessKey());
        }

        if (!BPMNActivityStatus.COMPLETED.equals(bpmnActivityEntity.getStatus())) {
            bpmnActivityEntity.setStatus(BPMNActivityStatus.COMPLETED);
        }

        persistIntoDatabase(event,
                            bpmnActivityEntity);

    }

    private void persistIntoDatabase(CloudRuntimeEvent<?, ?> event,
                                     BPMNActivityEntity entity) {
        try {
            bpmnActivitiyRepository.save(entity);
        } catch (Exception cause) {
            throw new QueryException("Error handling CloudBPMNActivityCancelledEvent[" + event + "]",
                                     cause);
        }
    }

    @Override
    public String getHandledEvent() {
        return IntegrationEvents.INTEGRATION_RESULT_RECEIVED.name();
    }
}
