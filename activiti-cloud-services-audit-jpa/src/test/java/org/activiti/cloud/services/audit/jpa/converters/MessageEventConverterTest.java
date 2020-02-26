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

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.MessagePayloadBuilder;
import org.activiti.api.process.model.payloads.MessageEventPayload;
import org.activiti.api.runtime.model.impl.BPMNMessageImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNMessageEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNMessageReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNMessageSentEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNMessageWaitingEventImpl;
import org.activiti.cloud.services.audit.jpa.events.MessageAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.MessageReceivedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.MessageSentAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.MessageWaitingAuditEventEntity;
import org.junit.Test;

public class MessageEventConverterTest {

    private MessageSentEventConverter messageSentEventComverter = new MessageSentEventConverter(new EventContextInfoAppender());
    private MessageWaitingEventConverter messageWaitingEventConverter = new MessageWaitingEventConverter(new EventContextInfoAppender());
    private MessageReceivedEventConverter messageReceivedEventConverter = new MessageReceivedEventConverter(new EventContextInfoAppender());

    @Test
    public void checkConvertToEntityMessageSentEvent() {
        //given
        CloudBPMNMessageSentEventImpl event = createMessageSentEvent();

        //when
        MessageSentAuditEventEntity auditEventEntity = (MessageSentAuditEventEntity) messageSentEventComverter.convertToEntity(event);

        //then
        assertThatIsEqualTo(auditEventEntity, event);
    }

    @Test
    public void checkConvertToEntityMessageWaitingEvent() {
        //given
        CloudBPMNMessageWaitingEventImpl event = createMessageWaitingEvent();

        //when
        MessageWaitingAuditEventEntity auditEventEntity = (MessageWaitingAuditEventEntity) messageWaitingEventConverter.convertToEntity(event);

        //then
        assertThatIsEqualTo(auditEventEntity, event);
    }

    @Test
    public void checkConvertToEntityMessageReceivedEvent() {
        //given
        CloudBPMNMessageReceivedEventImpl event = createMessageReceivedEvent();

        //when
        MessageAuditEventEntity auditEventEntity = (MessageReceivedAuditEventEntity) messageReceivedEventConverter.convertToEntity(event);

        //then
        assertThatIsEqualTo(auditEventEntity, event);
    }

    @Test
    public void checkConvertToAPIMessageSentEvent() {
        //given
        MessageSentAuditEventEntity auditEventEntity = (MessageSentAuditEventEntity) messageSentEventComverter.convertToEntity(createMessageSentEvent());

        //when
        CloudBPMNMessageSentEventImpl cloudEvent = (CloudBPMNMessageSentEventImpl) messageSentEventComverter.convertToAPI(auditEventEntity);

        assertThatIsEqualTo(auditEventEntity, cloudEvent);
    }

    @Test
    public void checkConvertToAPIMessageWaitingEvent() {
        //given
        MessageWaitingAuditEventEntity auditEventEntity = (MessageWaitingAuditEventEntity) messageWaitingEventConverter.convertToEntity(createMessageWaitingEvent());

        //when
        CloudBPMNMessageWaitingEventImpl cloudEvent = (CloudBPMNMessageWaitingEventImpl) messageWaitingEventConverter.convertToAPI(auditEventEntity);

        assertThatIsEqualTo(auditEventEntity, cloudEvent);
    }

    @Test
    public void checkConvertToAPIMessageReceivedEvent() {
        //given
        MessageReceivedAuditEventEntity auditEventEntity = (MessageReceivedAuditEventEntity) messageReceivedEventConverter.convertToEntity(createMessageReceivedEvent());

        //when
        CloudBPMNMessageReceivedEventImpl cloudEvent = (CloudBPMNMessageReceivedEventImpl) messageReceivedEventConverter.convertToAPI(auditEventEntity);

        assertThatIsEqualTo(auditEventEntity, cloudEvent);
    }

    private CloudBPMNMessageSentEventImpl createMessageSentEvent() {
        //given
        ProcessInstanceImpl processInstance = createProcess();

        BPMNMessageImpl message = createBPMNMessage(processInstance);

        CloudBPMNMessageSentEventImpl event = new CloudBPMNMessageSentEventImpl("eventId",
                                                                                System.currentTimeMillis(),
                                                                                message,
                                                                                message.getProcessDefinitionId(),
                                                                                message.getProcessInstanceId());
        appendEventInfo(event, processInstance);

        return event;
    }

    private CloudBPMNMessageWaitingEventImpl createMessageWaitingEvent() {
        //given
        //given
        ProcessInstanceImpl processInstance = createProcess();

        BPMNMessageImpl message = createBPMNMessage(processInstance);

        CloudBPMNMessageWaitingEventImpl event = new CloudBPMNMessageWaitingEventImpl("eventId",
                                                                                      System.currentTimeMillis(),
                                                                                      message,
                                                                                      message.getProcessDefinitionId(),
                                                                                      message.getProcessInstanceId());
        appendEventInfo(event, processInstance);

        return event;
    }

    private CloudBPMNMessageReceivedEventImpl createMessageReceivedEvent() {
        //given
        //given
        ProcessInstanceImpl processInstance = createProcess();

        BPMNMessageImpl message = createBPMNMessage(processInstance);

        CloudBPMNMessageReceivedEventImpl event = new CloudBPMNMessageReceivedEventImpl("eventId",
                                                                                        System.currentTimeMillis(),
                                                                                        message,
                                                                                        message.getProcessDefinitionId(),
                                                                                        message.getProcessInstanceId());
        appendEventInfo(event, processInstance);

        return event;
    }

    private ProcessInstanceImpl createProcess() {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("processInstanceId");
        processInstance.setProcessDefinitionId("processDefinitionId");
        processInstance.setProcessDefinitionKey("processDefinitionKey");
        processInstance.setBusinessKey("businessKey");
        processInstance.setParentId("parentId");

        return processInstance;
    }

    private BPMNMessageImpl createBPMNMessage(ProcessInstanceImpl processInstance) {
        BPMNMessageImpl message = new BPMNMessageImpl("entityId");
        message.setProcessDefinitionId(processInstance.getProcessDefinitionId());
        message.setProcessInstanceId(processInstance.getId());
        message.setMessagePayload(createMessagePayload());
        return message;
    }

    private MessageEventPayload createMessagePayload() {
        MessageEventPayload messagePayload = MessagePayloadBuilder.event("messageName")
                                                                  .withBusinessKey("businessId")
                                                                  .withCorrelationKey("correlationId")
                                                                  .withVariable("name", "value")
                                                                  .build();

        return messagePayload;
    }

    private void appendEventInfo(CloudBPMNMessageEventImpl event, ProcessInstance processInstance) {
        event.setEntityId("entityId");
        event.setProcessInstanceId(processInstance.getId());
        event.setProcessDefinitionId(processInstance.getProcessDefinitionId());
        event.setProcessDefinitionKey(processInstance.getProcessDefinitionKey());
        event.setBusinessKey(processInstance.getBusinessKey());
        event.setParentProcessInstanceId(processInstance.getParentId());
        event.setMessageId("message-id");
        event.setSequenceNumber(0);

    }

    private void assertThatIsEqualTo(MessageAuditEventEntity auditEventEntity, CloudBPMNMessageEvent event) {
        assertThat(event).isNotNull();
        assertThat(auditEventEntity).isNotNull();
        assertThat(auditEventEntity.getEntityId()).isEqualTo(event.getEntityId());
        assertThat(auditEventEntity.getProcessInstanceId()).isEqualTo(event.getProcessInstanceId());
        assertThat(auditEventEntity.getProcessDefinitionId()).isEqualTo(event.getProcessDefinitionId());
        assertThat(auditEventEntity.getProcessDefinitionKey()).isEqualTo(event.getProcessDefinitionKey());
        assertThat(auditEventEntity.getBusinessKey()).isEqualTo(event.getBusinessKey());
        assertThat(auditEventEntity.getParentProcessInstanceId()).isEqualTo(event.getParentProcessInstanceId());
        assertThat(auditEventEntity.getMessage().getProcessInstanceId()).isEqualTo(event.getEntity()
                                                                                        .getProcessInstanceId());
        assertThat(auditEventEntity.getMessage().getProcessDefinitionId()).isEqualTo(event.getEntity()
                                                                                          .getProcessDefinitionId());
        assertThat(auditEventEntity.getMessage().getMessagePayload()).isEqualTo(event.getEntity().getMessagePayload());
    }
}