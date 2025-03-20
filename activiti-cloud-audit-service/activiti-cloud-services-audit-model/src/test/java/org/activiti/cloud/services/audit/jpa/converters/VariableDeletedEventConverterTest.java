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

package org.activiti.cloud.services.audit.jpa.converters;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.VariableDeletedEventEntity;
import org.junit.jupiter.api.Test;

class VariableDeletedEventConverterTest {

    private final VariableDeletedEventConverter variableDeletedEventConverter = new VariableDeletedEventConverter(
        new EventContextInfoAppender()
    );

    @Test
    void should_supportVariableDeletedEvent() {
        assertThat(variableDeletedEventConverter.getSupportedEvent())
            .isEqualTo(VariableEvent.VariableEvents.VARIABLE_DELETED.name());
    }

    @Test
    void shouldConvertFromCloudEventToEventEntity() {
        CloudVariableDeletedEventImpl cloudEvent = buildCloudDeletedEvent(buildVariableInstance());

        VariableDeletedEventEntity eventEntity = variableDeletedEventConverter.createEventEntity(cloudEvent);

        assertHasSameAttributeValues(eventEntity, cloudEvent);
    }

    @Test
    void shouldConvertFromCloudEventToEventEntity_when_itsEphemeralVariable() {
        CloudVariableDeletedEventImpl cloudEvent = buildCloudDeletedEvent(buildVariableInstance());
        cloudEvent.setEphemeralVariable(true);

        VariableDeletedEventEntity eventEntity = variableDeletedEventConverter.createEventEntity(cloudEvent);

        assertHasSameAttributeValues(eventEntity, cloudEvent);
    }

    private VariableInstanceImpl<String> buildVariableInstance() {
        return new VariableInstanceImpl<>("var", "string", "any", "processInstanceId", null);
    }

    private void assertHasSameAttributeValues(
        VariableDeletedEventEntity eventEntity,
        CloudVariableDeletedEventImpl cloudEvent
    ) {
        assertThat(eventEntity.getEventId()).isEqualTo(cloudEvent.getId());
        assertThat(eventEntity.getTimestamp()).isEqualTo(cloudEvent.getTimestamp());
        assertThat(eventEntity.getEventType()).isEqualTo(cloudEvent.getEventType().name());
        assertThat(eventEntity.getAppName()).isEqualTo(cloudEvent.getAppName());
        assertThat(eventEntity.getAppVersion()).isEqualTo(cloudEvent.getAppVersion());
        assertThat(eventEntity.getServiceName()).isEqualTo(cloudEvent.getServiceName());
        assertThat(eventEntity.getServiceFullName()).isEqualTo(cloudEvent.getServiceFullName());
        assertThat(eventEntity.getServiceType()).isEqualTo(cloudEvent.getServiceType());
        assertThat(eventEntity.getServiceVersion()).isEqualTo(cloudEvent.getServiceVersion());
        assertThat(eventEntity.getMessageId()).isEqualTo(cloudEvent.getMessageId());
        assertThat(eventEntity.getSequenceNumber()).isEqualTo(cloudEvent.getSequenceNumber());
        assertThat(eventEntity.getEntityId()).isEqualTo(cloudEvent.getEntityId());
        assertThat(eventEntity.getProcessInstanceId()).isEqualTo(cloudEvent.getProcessInstanceId());
        assertThat(eventEntity.getProcessDefinitionId()).isEqualTo(cloudEvent.getProcessDefinitionId());
        assertThat(eventEntity.getProcessDefinitionKey()).isEqualTo(cloudEvent.getProcessDefinitionKey());
        assertThat(eventEntity.getBusinessKey()).isEqualTo(cloudEvent.getBusinessKey());
        assertThat(eventEntity.getParentProcessInstanceId()).isEqualTo(cloudEvent.getParentProcessInstanceId());
        assertThat(eventEntity.getVariableName()).isEqualTo(cloudEvent.getEntity().getName());
        assertThat(eventEntity.getVariableType()).isEqualTo(cloudEvent.getEntity().getType());
        assertThat(eventEntity.getVariableInstance()).isEqualTo(cloudEvent.getEntity());
        assertThat(eventEntity.getTaskId()).isEqualTo(cloudEvent.getEntity().getTaskId());
        assertThat(eventEntity.isEphemeralVariable()).isEqualTo(cloudEvent.isEphemeralVariable());
    }

    private CloudVariableDeletedEventImpl buildCloudDeletedEvent(VariableInstanceImpl<String> variableInstance) {
        CloudVariableDeletedEventImpl cloudEvent = new CloudVariableDeletedEventImpl(
            "id",
            System.currentTimeMillis(),
            variableInstance
        );
        cloudEvent.setAppName("appName");
        cloudEvent.setAppVersion("appVersion");
        cloudEvent.setServiceName("serviceName");
        cloudEvent.setServiceFullName("serviceFullName");
        cloudEvent.setServiceType("serviceType");
        cloudEvent.setServiceVersion("serviceVersion");
        cloudEvent.setMessageId("messageId");
        cloudEvent.setSequenceNumber(2);
        cloudEvent.setProcessInstanceId("processInstanceId");
        cloudEvent.setProcessDefinitionId("processDefinitionId");
        cloudEvent.setProcessDefinitionKey("processDefinitionKey");
        cloudEvent.setBusinessKey("businessKey");
        cloudEvent.setParentProcessInstanceId("parentProcessInstanceId");
        return cloudEvent;
    }

    @Test
    void createAPIEvent_should_createCloudEventWithBasicInformation() {
        VariableDeletedEventEntity variableDeletedEventEntity = new VariableDeletedEventEntity();
        variableDeletedEventEntity.setEventId("eventId");
        variableDeletedEventEntity.setTimestamp(System.currentTimeMillis());
        variableDeletedEventEntity.setVariableInstance(buildVariableInstance());

        CloudVariableDeletedEventImpl event = (CloudVariableDeletedEventImpl) variableDeletedEventConverter.createAPIEvent(
            variableDeletedEventEntity
        );

        assertThat(event).isNotNull();
        assertThat(event.getId()).isEqualTo(variableDeletedEventEntity.getEventId());
        assertThat(event.getTimestamp()).isEqualTo(variableDeletedEventEntity.getTimestamp());
        assertThat(event.getEntity()).isEqualTo(variableDeletedEventEntity.getVariableInstance());
        assertThat(event.isEphemeralVariable()).isEqualTo(variableDeletedEventEntity.isEphemeralVariable());
    }

    @Test
    void createAPIEvent_should_createCloudEventWithBasicInformationAndEphemeralAttribute() {
        CloudVariableDeletedEventImpl cloudEvent = new CloudVariableDeletedEventImpl(buildVariableInstance(), true);
        cloudEvent.setSequenceNumber(1);
        VariableDeletedEventEntity variableDeletedEventEntity = new VariableDeletedEventEntity(cloudEvent);
        variableDeletedEventEntity.setEventId("eventId");
        variableDeletedEventEntity.setTimestamp(System.currentTimeMillis());

        CloudVariableDeletedEventImpl event = (CloudVariableDeletedEventImpl) variableDeletedEventConverter.createAPIEvent(
            variableDeletedEventEntity
        );

        assertThat(event).isNotNull();
        assertThat(event.getId()).isEqualTo(variableDeletedEventEntity.getEventId());
        assertThat(event.getTimestamp()).isEqualTo(variableDeletedEventEntity.getTimestamp());
        assertThat(event.getEntity()).isEqualTo(variableDeletedEventEntity.getVariableInstance());
        assertThat(event.isEphemeralVariable()).isTrue();
    }
}
