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
import java.util.Date;
import java.util.Optional;
import org.activiti.api.process.model.BPMNSequenceFlow;
import org.activiti.api.process.model.events.SequenceFlowEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudSequenceFlowTakenEvent;
import org.activiti.cloud.services.query.model.BPMNSequenceFlowEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BPMNSequenceFlowTakenEventHandler implements QueryEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(BPMNSequenceFlowTakenEventHandler.class);

    private final EntityManager entityManager;

    public BPMNSequenceFlowTakenEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudSequenceFlowTakenEvent sequenceFlowTakenEvent = CloudSequenceFlowTakenEvent.class.cast(event);
        var entityId = getEntityId(sequenceFlowTakenEvent);

        Optional
            .ofNullable(entityManager.find(BPMNSequenceFlowEntity.class, entityId))
            .ifPresentOrElse(
                sequenceFlowEntity -> {
                    logger.warn(
                        "Sequence flow '" +
                        sequenceFlowEntity.getElementId() +
                        "' with eventId '" +
                        event.getId() +
                        "' already exists in the process '" +
                        sequenceFlowTakenEvent.getEntity().getProcessInstanceId() +
                        "'!"
                    );
                },
                () -> {
                    var sequenceFlowTakenEntity = createBpmnSequenceFlowEntity(entityId, sequenceFlowTakenEvent);
                    entityManager.persist(sequenceFlowTakenEntity);
                }
            );
    }

    private BPMNSequenceFlowEntity createBpmnSequenceFlowEntity(String entityId, CloudSequenceFlowTakenEvent event) {
        BPMNSequenceFlow bpmnSequenceFlow = event.getEntity();

        BPMNSequenceFlowEntity bpmnSequenceFlowEntity = new BPMNSequenceFlowEntity(
            event.getServiceName(),
            event.getServiceFullName(),
            event.getServiceVersion(),
            event.getAppName(),
            event.getAppVersion()
        );
        bpmnSequenceFlowEntity.setId(entityId);
        bpmnSequenceFlowEntity.setElementId(bpmnSequenceFlow.getElementId());
        bpmnSequenceFlowEntity.setProcessDefinitionId(bpmnSequenceFlow.getProcessDefinitionId());
        bpmnSequenceFlowEntity.setProcessInstanceId(bpmnSequenceFlow.getProcessInstanceId());
        bpmnSequenceFlowEntity.setDate(new Date(event.getTimestamp()));
        bpmnSequenceFlowEntity.setSourceActivityElementId(bpmnSequenceFlow.getSourceActivityElementId());
        bpmnSequenceFlowEntity.setSourceActivityType(bpmnSequenceFlow.getSourceActivityType());
        bpmnSequenceFlowEntity.setSourceActivityName(bpmnSequenceFlow.getSourceActivityName());
        bpmnSequenceFlowEntity.setTargetActivityElementId(bpmnSequenceFlow.getTargetActivityElementId());
        bpmnSequenceFlowEntity.setTargetActivityType(bpmnSequenceFlow.getTargetActivityType());
        bpmnSequenceFlowEntity.setTargetActivityName(bpmnSequenceFlow.getTargetActivityName());
        bpmnSequenceFlowEntity.setProcessDefinitionKey(event.getProcessDefinitionKey());
        bpmnSequenceFlowEntity.setProcessDefinitionVersion(event.getProcessDefinitionVersion());
        bpmnSequenceFlowEntity.setBusinessKey(event.getBusinessKey());
        bpmnSequenceFlowEntity.setEventId(event.getId());

        return bpmnSequenceFlowEntity;
    }

    private String getEntityId(CloudSequenceFlowTakenEvent event) {
        return new StringBuilder()
            .append(event.getEntity().getProcessInstanceId())
            .append(":")
            .append(event.getEntity().getElementId())
            .append(":")
            .append(event.getId())
            .toString();
    }

    @Override
    public String getHandledEvent() {
        return SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN.name();
    }
}
