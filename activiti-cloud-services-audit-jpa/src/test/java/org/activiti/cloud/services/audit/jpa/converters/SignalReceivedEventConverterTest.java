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

package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.cloud.api.process.model.impl.events.CloudBPMNSignalReceivedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.SignalReceivedAuditEventEntity;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.runtime.model.impl.BPMNSignalImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class SignalReceivedEventConverterTest {

    private SignalReceivedEventConverter eventConverter = new SignalReceivedEventConverter(new EventContextInfoAppender());

    @Test
    public void checkConvertToEntitySignalReceivedEvent() {
        //given
        CloudBPMNSignalReceivedEventImpl event = createSignalReceivedEvent();
           
        //when
        SignalReceivedAuditEventEntity auditEventEntity = (SignalReceivedAuditEventEntity) eventConverter.convertToEntity(event);
     
        //then
        assertThat(auditEventEntity).isNotNull();
        assertThat(auditEventEntity.getEntityId()).isEqualTo(event.getEntityId());
        assertThat(auditEventEntity.getProcessInstanceId()).isEqualTo(event.getProcessInstanceId());
        assertThat(auditEventEntity.getProcessDefinitionId()).isEqualTo(event.getProcessDefinitionId());
        assertThat(auditEventEntity.getProcessDefinitionKey()).isEqualTo(event.getProcessDefinitionKey());
        assertThat(auditEventEntity.getBusinessKey()).isEqualTo(event.getBusinessKey());
        assertThat(auditEventEntity.getParentProcessInstanceId()).isEqualTo(event.getParentProcessInstanceId());
        assertThat(auditEventEntity.getSignal().getSignalPayload().getId()).isEqualTo(event.getEntity().getSignalPayload().getId());
        assertThat(auditEventEntity.getSignal().getSignalPayload().getName()).isEqualTo(event.getEntity().getSignalPayload().getName());
        assertThat(auditEventEntity.getSignal().getSignalPayload().getVariables()).isEqualTo(event.getEntity().getSignalPayload().getVariables());
    }
    
    @Test
    public void checkConvertToAPISignalReceivedEvent() {
        //given
        SignalReceivedAuditEventEntity auditEventEntity = (SignalReceivedAuditEventEntity) eventConverter.convertToEntity(createSignalReceivedEvent());
        
        //when
        CloudBPMNSignalReceivedEventImpl cloudEvent= (CloudBPMNSignalReceivedEventImpl) eventConverter.convertToAPI(auditEventEntity);
        assertThat(cloudEvent).isNotNull();
        assertThat(auditEventEntity.getEntityId()).isEqualTo(cloudEvent.getEntityId());
        assertThat(auditEventEntity.getProcessInstanceId()).isEqualTo(cloudEvent.getProcessInstanceId());
        assertThat(auditEventEntity.getProcessDefinitionId()).isEqualTo(cloudEvent.getProcessDefinitionId());
        assertThat(auditEventEntity.getProcessDefinitionKey()).isEqualTo(cloudEvent.getProcessDefinitionKey());
        assertThat(auditEventEntity.getBusinessKey()).isEqualTo(cloudEvent.getBusinessKey());
        assertThat(auditEventEntity.getParentProcessInstanceId()).isEqualTo(cloudEvent.getParentProcessInstanceId());
        assertThat(auditEventEntity.getSignal().getSignalPayload().getId()).isEqualTo(cloudEvent.getEntity().getSignalPayload().getId());
        assertThat(auditEventEntity.getSignal().getSignalPayload().getName()).isEqualTo(cloudEvent.getEntity().getSignalPayload().getName());
        assertThat(auditEventEntity.getSignal().getSignalPayload().getVariables()).isEqualTo(cloudEvent.getEntity().getSignalPayload().getVariables());
    }
    
    private CloudBPMNSignalReceivedEventImpl createSignalReceivedEvent() {
        //given
        ProcessInstanceImpl processInstanceStarted = new ProcessInstanceImpl();
        processInstanceStarted.setId("processInstanceId");
        processInstanceStarted.setProcessDefinitionId("processDefinitionId");
        processInstanceStarted.setProcessDefinitionKey("processDefinitionKey");
        processInstanceStarted.setBusinessKey("businessKey");
        processInstanceStarted.setParentId("parentId");
            
        BPMNSignalImpl signal = new BPMNSignalImpl("entityId");
        signal.setProcessDefinitionId(processInstanceStarted.getProcessDefinitionId());
        signal.setProcessInstanceId(processInstanceStarted.getId());

        SignalPayload signalPayload = ProcessPayloadBuilder.signal()
                .withName("SignalName")
                .withVariable("signal-variable",
                              "test")
                .build();
        signal.setSignalPayload(signalPayload);

        CloudBPMNSignalReceivedEventImpl event = new CloudBPMNSignalReceivedEventImpl("eventId",
                                                                                      System.currentTimeMillis(),
                                                                                      signal,
                                                                                      signal.getProcessDefinitionId(),
                                                                                      signal.getProcessInstanceId());
        
        //Set explicitly to be sure
        event.setEntityId("entityId");
        event.setProcessInstanceId(processInstanceStarted.getId());
        event.setProcessDefinitionId(processInstanceStarted.getProcessDefinitionId());
        event.setProcessDefinitionKey(processInstanceStarted.getProcessDefinitionKey());
        event.setBusinessKey(processInstanceStarted.getBusinessKey());
        event.setParentProcessInstanceId(processInstanceStarted.getParentId());
        event.setMessageId("message-id");
        event.setSequenceNumber(0);
        
        
        return event;
    }
}