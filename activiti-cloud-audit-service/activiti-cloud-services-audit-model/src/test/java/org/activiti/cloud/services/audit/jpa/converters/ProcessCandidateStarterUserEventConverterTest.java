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

import org.activiti.api.runtime.model.impl.ProcessCandidateStarterGroupImpl;
import org.activiti.api.runtime.model.impl.ProcessCandidateStarterUserImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessCandidateStarterGroupAddedEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCandidateStarterGroupRemovedEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCandidateStarterUserAddedEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCandidateStarterUserRemovedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterGroupAddedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterGroupRemovedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterUserAddedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterUserRemovedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCandidateStarterGroupAddedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCandidateStarterGroupRemovedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCandidateStarterUserAddedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCandidateStarterUserRemovedEventEntity;
import org.junit.jupiter.api.Test;

public class ProcessCandidateStarterUserEventConverterTest {

    private EventContextInfoAppender eventContextInfoAppender = new EventContextInfoAppender();
    private ProcessCandidateStarterUserAddedEventConverter candidateStarterUserAddedEventConverter = new ProcessCandidateStarterUserAddedEventConverter(
        eventContextInfoAppender
    );
    private ProcessCandidateStarterUserRemovedEventConverter candidateStarterUserRemovedEventConverter = new ProcessCandidateStarterUserRemovedEventConverter(
        eventContextInfoAppender
    );
    private ProcessCandidateStarterGroupAddedEventConverter candidateStarterGroupAddedEventConverter = new ProcessCandidateStarterGroupAddedEventConverter(
        eventContextInfoAppender
    );
    private ProcessCandidateStarterGroupRemovedEventConverter candidateStarterGroupRemovedEventConverter = new ProcessCandidateStarterGroupRemovedEventConverter(
        eventContextInfoAppender
    );

    @Test
    public void convertToEntityProcessCandidateStarterUserAddedEvent() {
        CloudProcessCandidateStarterUserAddedEvent event = createProcessCandidateUserAddedEvent();
        ProcessCandidateStarterUserAddedEventEntity auditEventEntity = (ProcessCandidateStarterUserAddedEventEntity) candidateStarterUserAddedEventConverter.convertToEntity(
            event
        );

        assertEvent(auditEventEntity, event);
        assertThat(auditEventEntity.getCandidateStarterUser().getProcessDefinitionId())
            .isEqualTo(event.getEntity().getProcessDefinitionId());
        assertThat(auditEventEntity.getCandidateStarterUser().getUserId()).isEqualTo(event.getEntity().getUserId());
    }

    @Test
    public void convertToEntityProcessCandidateStarterUserRemovedEvent() {
        CloudProcessCandidateStarterUserRemovedEvent event = createProcessCandidateUserRemovedEvent();
        ProcessCandidateStarterUserRemovedEventEntity auditEventEntity = (ProcessCandidateStarterUserRemovedEventEntity) candidateStarterUserRemovedEventConverter.convertToEntity(
            event
        );

        assertEvent(auditEventEntity, event);
        assertThat(auditEventEntity.getCandidateStarterUser().getProcessDefinitionId())
            .isEqualTo(event.getEntity().getProcessDefinitionId());
        assertThat(auditEventEntity.getCandidateStarterUser().getUserId()).isEqualTo(event.getEntity().getUserId());
    }

    @Test
    public void convertToEntityProcessCandidateStarterGroupAddedEvent() {
        CloudProcessCandidateStarterGroupAddedEvent event = createProcessCandidateGroupAddedEvent();
        ProcessCandidateStarterGroupAddedEventEntity auditEventEntity = (ProcessCandidateStarterGroupAddedEventEntity) candidateStarterGroupAddedEventConverter.convertToEntity(
            event
        );

        assertEvent(auditEventEntity, event);
        assertThat(auditEventEntity.getCandidateStarterGroup().getProcessDefinitionId())
            .isEqualTo(event.getEntity().getProcessDefinitionId());
        assertThat(auditEventEntity.getCandidateStarterGroup().getGroupId()).isEqualTo(event.getEntity().getGroupId());
    }

    @Test
    public void convertToEntityProcessCandidateStarterGroupRemovedEvent() {
        CloudProcessCandidateStarterGroupRemovedEvent event = createProcessCandidateGroupRemovedEvent();
        ProcessCandidateStarterGroupRemovedEventEntity auditEventEntity = (ProcessCandidateStarterGroupRemovedEventEntity) candidateStarterGroupRemovedEventConverter.convertToEntity(
            event
        );

        assertEvent(auditEventEntity, event);
        assertThat(auditEventEntity.getCandidateStarterGroup().getProcessDefinitionId())
            .isEqualTo(event.getEntity().getProcessDefinitionId());
        assertThat(auditEventEntity.getCandidateStarterGroup().getGroupId()).isEqualTo(event.getEntity().getGroupId());
    }

    @Test
    public void convertToAPITaskCandidateUserAddedEvent() {
        ProcessCandidateStarterUserAddedEventEntity auditEventEntity = (ProcessCandidateStarterUserAddedEventEntity) candidateStarterUserAddedEventConverter.convertToEntity(
            createProcessCandidateUserAddedEvent()
        );
        CloudProcessCandidateStarterUserAddedEvent cloudEvent = (CloudProcessCandidateStarterUserAddedEvent) candidateStarterUserAddedEventConverter.convertToAPI(
            auditEventEntity
        );

        assertEvent(auditEventEntity, cloudEvent);
        assertThat(auditEventEntity.getCandidateStarterUser().getProcessDefinitionId())
            .isEqualTo(cloudEvent.getEntity().getProcessDefinitionId());
        assertThat(auditEventEntity.getCandidateStarterUser().getUserId())
            .isEqualTo(cloudEvent.getEntity().getUserId());
    }

    @Test
    public void convertToAPITaskCandidateUserRemovedEvent() {
        ProcessCandidateStarterUserRemovedEventEntity auditEventEntity = (ProcessCandidateStarterUserRemovedEventEntity) candidateStarterUserRemovedEventConverter.convertToEntity(
            createProcessCandidateUserRemovedEvent()
        );
        CloudProcessCandidateStarterUserRemovedEvent cloudEvent = (CloudProcessCandidateStarterUserRemovedEvent) candidateStarterUserRemovedEventConverter.convertToAPI(
            auditEventEntity
        );

        assertEvent(auditEventEntity, cloudEvent);
        assertThat(auditEventEntity.getCandidateStarterUser().getProcessDefinitionId())
            .isEqualTo(cloudEvent.getEntity().getProcessDefinitionId());
        assertThat(auditEventEntity.getCandidateStarterUser().getUserId())
            .isEqualTo(cloudEvent.getEntity().getUserId());
    }

    @Test
    public void convertToAPITaskCandidateGroupAddedEvent() {
        ProcessCandidateStarterGroupAddedEventEntity auditEventEntity = (ProcessCandidateStarterGroupAddedEventEntity) candidateStarterGroupAddedEventConverter.convertToEntity(
            createProcessCandidateGroupAddedEvent()
        );
        CloudProcessCandidateStarterGroupAddedEvent cloudEvent = (CloudProcessCandidateStarterGroupAddedEvent) candidateStarterGroupAddedEventConverter.convertToAPI(
            auditEventEntity
        );

        assertEvent(auditEventEntity, cloudEvent);
        assertThat(auditEventEntity.getCandidateStarterGroup().getProcessDefinitionId())
            .isEqualTo(cloudEvent.getEntity().getProcessDefinitionId());
        assertThat(auditEventEntity.getCandidateStarterGroup().getGroupId())
            .isEqualTo(cloudEvent.getEntity().getGroupId());
    }

    @Test
    public void convertToAPITaskCandidateGroupRemovedEvent() {
        ProcessCandidateStarterGroupRemovedEventEntity auditEventEntity = (ProcessCandidateStarterGroupRemovedEventEntity) candidateStarterGroupRemovedEventConverter.convertToEntity(
            createProcessCandidateGroupRemovedEvent()
        );
        CloudProcessCandidateStarterGroupRemovedEvent cloudEvent = (CloudProcessCandidateStarterGroupRemovedEvent) candidateStarterGroupRemovedEventConverter.convertToAPI(
            auditEventEntity
        );

        //then
        assertEvent(auditEventEntity, cloudEvent);
        assertThat(auditEventEntity.getCandidateStarterGroup().getProcessDefinitionId())
            .isEqualTo(cloudEvent.getEntity().getProcessDefinitionId());
        assertThat(auditEventEntity.getCandidateStarterGroup().getGroupId())
            .isEqualTo(cloudEvent.getEntity().getGroupId());
    }

    private CloudProcessCandidateStarterUserAddedEventImpl createProcessCandidateUserAddedEvent() {
        ProcessCandidateStarterUserImpl candidateStarterUser = new ProcessCandidateStarterUserImpl(
            "processId",
            "userId"
        );
        CloudProcessCandidateStarterUserAddedEventImpl candidateUserAddedEvent = new CloudProcessCandidateStarterUserAddedEventImpl(
            "eventId",
            System.currentTimeMillis(),
            candidateStarterUser
        );
        setEventDetails(candidateUserAddedEvent);
        return candidateUserAddedEvent;
    }

    private CloudProcessCandidateStarterUserRemovedEventImpl createProcessCandidateUserRemovedEvent() {
        ProcessCandidateStarterUserImpl candidateStarterUser = new ProcessCandidateStarterUserImpl(
            "processId",
            "userId"
        );
        CloudProcessCandidateStarterUserRemovedEventImpl candidateUserRemovedEvent = new CloudProcessCandidateStarterUserRemovedEventImpl(
            "eventId",
            System.currentTimeMillis(),
            candidateStarterUser
        );
        setEventDetails(candidateUserRemovedEvent);
        return candidateUserRemovedEvent;
    }

    private CloudProcessCandidateStarterGroupAddedEventImpl createProcessCandidateGroupAddedEvent() {
        ProcessCandidateStarterGroupImpl candidateStarterGroup = new ProcessCandidateStarterGroupImpl(
            "processId",
            "groupId"
        );
        CloudProcessCandidateStarterGroupAddedEventImpl candidateGroupAddedEvent = new CloudProcessCandidateStarterGroupAddedEventImpl(
            "eventId",
            System.currentTimeMillis(),
            candidateStarterGroup
        );
        setEventDetails(candidateGroupAddedEvent);
        return candidateGroupAddedEvent;
    }

    private CloudProcessCandidateStarterGroupRemovedEventImpl createProcessCandidateGroupRemovedEvent() {
        ProcessCandidateStarterGroupImpl candidateStarterGroup = new ProcessCandidateStarterGroupImpl(
            "processId",
            "groupId"
        );
        CloudProcessCandidateStarterGroupRemovedEventImpl candidateGroupRemovedEvent = new CloudProcessCandidateStarterGroupRemovedEventImpl(
            "eventId",
            System.currentTimeMillis(),
            candidateStarterGroup
        );
        setEventDetails(candidateGroupRemovedEvent);
        return candidateGroupRemovedEvent;
    }

    private void setEventDetails(CloudRuntimeEventImpl event) {
        event.setEntityId("entityId");
        event.setProcessInstanceId("processInstanceId");
        event.setProcessDefinitionId("processDefinitionId");
        event.setProcessDefinitionKey("processDefinitionKey");
        event.setBusinessKey("businessKey");
        event.setParentProcessInstanceId("parentProcessInstanceId");
        event.setMessageId("messageId");
        event.setSequenceNumber(0);
    }

    private void assertEvent(AuditEventEntity auditEventEntity, CloudRuntimeEvent event) {
        assertThat(auditEventEntity).isNotNull();
        assertThat(auditEventEntity.getEntityId()).isEqualTo(event.getEntityId());
        assertThat(auditEventEntity.getProcessInstanceId()).isEqualTo(event.getProcessInstanceId());
        assertThat(auditEventEntity.getProcessDefinitionId()).isEqualTo(event.getProcessDefinitionId());
        assertThat(auditEventEntity.getProcessDefinitionKey()).isEqualTo(event.getProcessDefinitionKey());
        assertThat(auditEventEntity.getBusinessKey()).isEqualTo(event.getBusinessKey());
        assertThat(auditEventEntity.getParentProcessInstanceId()).isEqualTo(event.getParentProcessInstanceId());
    }
}
