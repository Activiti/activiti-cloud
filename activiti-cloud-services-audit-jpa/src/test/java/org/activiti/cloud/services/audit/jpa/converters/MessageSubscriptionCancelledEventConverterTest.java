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

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.api.process.model.MessageSubscription;
import org.activiti.api.runtime.model.impl.MessageSubscriptionImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.events.CloudMessageSubscriptionCancelledEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudMessageSubscriptionCancelledEventImpl;
import org.activiti.cloud.services.audit.jpa.events.MessageSubscriptionCancelledAuditEventEntity;
import org.junit.Test;

public class MessageSubscriptionCancelledEventConverterTest {

    private MessageSubscriptionCancelledEventConverter eventConverter = new MessageSubscriptionCancelledEventConverter(new EventContextInfoAppender());

    @Test
    public void should_convert_toEntityMessageSubscriptionCancelledEvent() {
        CloudMessageSubscriptionCancelledEventImpl event = createMessageSubscriptionCancelledEvent();

        MessageSubscriptionCancelledAuditEventEntity auditEventEntity = (MessageSubscriptionCancelledAuditEventEntity) eventConverter.convertToEntity(event);

        assertThatIsEqualTo(auditEventEntity, event);
    }
    
    @Test
    public void should_convertToAPIErrorReceivedEvent() {
        //given
        MessageSubscriptionCancelledAuditEventEntity auditEventEntity = (MessageSubscriptionCancelledAuditEventEntity) eventConverter.convertToEntity(createMessageSubscriptionCancelledEvent());
        
        CloudMessageSubscriptionCancelledEventImpl event= (CloudMessageSubscriptionCancelledEventImpl) eventConverter.convertToAPI(auditEventEntity);
        assertThatIsEqualTo(auditEventEntity, event);
    }
    
    private CloudMessageSubscriptionCancelledEventImpl createMessageSubscriptionCancelledEvent() {
        //given
        ProcessInstanceImpl processInstanceStarted = new ProcessInstanceImpl();
        processInstanceStarted.setId("processInstanceId");
        processInstanceStarted.setProcessDefinitionId("processDefinitionId");
        processInstanceStarted.setProcessDefinitionKey("processDefinitionKey");
        processInstanceStarted.setBusinessKey("businessKey");
        processInstanceStarted.setParentId("parentId");
            
        MessageSubscription messageSubscription = MessageSubscriptionImpl.builder()
                                                    .withId("entityId")
                                                    .withEventName("messageName")
                                                    .withConfiguration("correlationKey")
                                                    .withProcessDefinitionId(processInstanceStarted.getProcessDefinitionId())
                                                    .withProcessInstanceId(processInstanceStarted.getId())
                                                    .withBusinessKey(processInstanceStarted.getBusinessKey())
                                                    .build();
        
        CloudMessageSubscriptionCancelledEventImpl event = CloudMessageSubscriptionCancelledEventImpl.builder()
                                                                .withEntity(messageSubscription)
                                                                .build();
        
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
    
    private void assertThatIsEqualTo(MessageSubscriptionCancelledAuditEventEntity auditEventEntity, CloudMessageSubscriptionCancelledEvent event) {
        assertThat(event).isNotNull();
        assertThat(auditEventEntity).isNotNull();
        assertThat(auditEventEntity.getEntityId()).isEqualTo(event.getEntityId());
        assertThat(auditEventEntity.getProcessInstanceId()).isEqualTo(event.getProcessInstanceId());
        assertThat(auditEventEntity.getProcessDefinitionId()).isEqualTo(event.getProcessDefinitionId());
        assertThat(auditEventEntity.getProcessDefinitionKey()).isEqualTo(event.getProcessDefinitionKey());
        assertThat(auditEventEntity.getBusinessKey()).isEqualTo(event.getBusinessKey());
        assertThat(auditEventEntity.getParentProcessInstanceId()).isEqualTo(event.getParentProcessInstanceId());
        assertThat(auditEventEntity.getMessageSubscription()).isEqualTo(event.getEntity());
    }
}