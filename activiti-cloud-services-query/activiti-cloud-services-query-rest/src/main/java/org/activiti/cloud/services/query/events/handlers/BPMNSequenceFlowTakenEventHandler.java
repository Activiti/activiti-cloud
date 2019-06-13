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
import java.util.UUID;

import javax.persistence.EntityManager;

import org.activiti.api.process.model.BPMNSequenceFlow;
import org.activiti.api.process.model.events.SequenceFlowEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudSequenceFlowTakenEvent;
import org.activiti.cloud.services.query.app.repository.BPMNSequenceFlowRepository;
import org.activiti.cloud.services.query.model.BPMNSequenceFlowEntity;
import org.activiti.cloud.services.query.model.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BPMNSequenceFlowTakenEventHandler implements QueryEventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(BPMNSequenceFlowTakenEventHandler.class);

    private final BPMNSequenceFlowRepository bpmnSequenceFlowRepository;
    private final EntityManager entityManager;

    public BPMNSequenceFlowTakenEventHandler(BPMNSequenceFlowRepository activitiyRepository,
                                             EntityManager entityManager) {
        this.bpmnSequenceFlowRepository = activitiyRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudSequenceFlowTakenEvent activityEvent = CloudSequenceFlowTakenEvent.class.cast(event);
        
        BPMNSequenceFlow bpmnSequenceFlow = activityEvent.getEntity();
        
        // Let's find if there an existing record with event's messageId
        BPMNSequenceFlowEntity bpmnSequenceFlowEntity = bpmnSequenceFlowRepository.findByEventId(event.getId());
        
        // Let's add sequence flow record if does not exist
        if(bpmnSequenceFlowEntity == null) {
            bpmnSequenceFlowEntity = new BPMNSequenceFlowEntity(event.getServiceName(),
                                                            event.getServiceFullName(),
                                                            event.getServiceVersion(),
                                                            event.getAppName(),
                                                            event.getAppVersion());
            
            bpmnSequenceFlowEntity.setId(UUID.randomUUID().toString());
            bpmnSequenceFlowEntity.setElementId(bpmnSequenceFlow.getElementId());
            bpmnSequenceFlowEntity.setProcessDefinitionId(bpmnSequenceFlow.getProcessDefinitionId());
            bpmnSequenceFlowEntity.setProcessInstanceId(bpmnSequenceFlow.getProcessInstanceId());
            bpmnSequenceFlowEntity.setDate(new Date(activityEvent.getTimestamp()));
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
    
            persistIntoDatabase(event,
                                bpmnSequenceFlowEntity);
        } else {
            logger.debug("Found existing record {} with message id {}. ", bpmnSequenceFlowEntity, event.getMessageId());
        }
    }

    private void persistIntoDatabase(CloudRuntimeEvent<?, ?> event,
                                     BPMNSequenceFlowEntity entity) {
        try {
            bpmnSequenceFlowRepository.save(entity);
        } catch (Exception cause) {
            throw new QueryException("Error handling CloudSequenceFlowTakenEvent[" + event + "]",
                                     cause);
        }
    }

    @Override
    public String getHandledEvent() {
        return SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN.name();
    }
}
