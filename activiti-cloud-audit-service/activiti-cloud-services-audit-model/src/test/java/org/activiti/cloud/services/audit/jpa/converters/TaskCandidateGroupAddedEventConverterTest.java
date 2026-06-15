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
package org.activiti.cloud.services.audit.jpa.converters;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.api.task.model.events.TaskCandidateGroupEvent;
import org.activiti.api.task.model.impl.TaskCandidateGroupImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupAddedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskCandidateGroupAddedEventEntity;
import org.junit.jupiter.api.Test;

class TaskCandidateGroupAddedEventConverterTest {

    private final TaskCandidateGroupAddedEventConverter eventConverter = new TaskCandidateGroupAddedEventConverter(
        new EventContextInfoAppender()
    );

    @Test
    void should_returnTaskCandidateGroupAdded_when_getSupportedEvent() {
        assertThat(eventConverter.getSupportedEvent())
            .isEqualTo(TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_ADDED.name());
    }

    @Test
    void should_buildEntityWithCandidateGroupAndProcessContext_when_convertToEntity() {
        //given
        CloudTaskCandidateGroupAddedEventImpl event = createTaskCandidateGroupAddedEvent();

        //when
        AuditEventEntity auditEventEntity = eventConverter.convertToEntity(event);

        //then
        assertThat(auditEventEntity).isNotNull();
        assertThat(auditEventEntity).isInstanceOf(TaskCandidateGroupAddedEventEntity.class);
        assertThat(((TaskCandidateGroupAddedEventEntity) auditEventEntity).getCandidateGroup().getTaskId())
            .isEqualTo(event.getEntity().getTaskId());
        assertThat(((TaskCandidateGroupAddedEventEntity) auditEventEntity).getCandidateGroup().getGroupId())
            .isEqualTo(event.getEntity().getGroupId());
        assertThat(auditEventEntity.getEntityId()).isEqualTo(event.getEntityId());
        assertThat(auditEventEntity.getProcessInstanceId()).isEqualTo(event.getProcessInstanceId());
        assertThat(auditEventEntity.getProcessDefinitionId()).isEqualTo(event.getProcessDefinitionId());
        assertThat(auditEventEntity.getProcessDefinitionKey()).isEqualTo(event.getProcessDefinitionKey());
        assertThat(auditEventEntity.getBusinessKey()).isEqualTo(event.getBusinessKey());
        assertThat(auditEventEntity.getParentProcessInstanceId()).isEqualTo(event.getParentProcessInstanceId());
    }

    @Test
    void should_buildCloudEventWithCandidateGroupAndProcessContext_when_convertToAPI() {
        //given
        AuditEventEntity auditEventEntity = eventConverter.convertToEntity(createTaskCandidateGroupAddedEvent());

        //when
        CloudRuntimeEvent cloudEvent = eventConverter.convertToAPI(auditEventEntity);

        //then
        assertThat(cloudEvent).isNotNull();
        assertThat(cloudEvent).isInstanceOf(CloudTaskCandidateGroupAddedEventImpl.class);
        assertThat(((TaskCandidateGroupAddedEventEntity) auditEventEntity).getCandidateGroup().getTaskId())
            .isEqualTo(((CloudTaskCandidateGroupAddedEventImpl) cloudEvent).getEntity().getTaskId());
        assertThat(((TaskCandidateGroupAddedEventEntity) auditEventEntity).getCandidateGroup().getGroupId())
            .isEqualTo(((CloudTaskCandidateGroupAddedEventImpl) cloudEvent).getEntity().getGroupId());
        assertThat(auditEventEntity.getEntityId()).isEqualTo(cloudEvent.getEntityId());
        assertThat(auditEventEntity.getProcessInstanceId()).isEqualTo(cloudEvent.getProcessInstanceId());
        assertThat(auditEventEntity.getProcessDefinitionId()).isEqualTo(cloudEvent.getProcessDefinitionId());
        assertThat(auditEventEntity.getProcessDefinitionKey()).isEqualTo(cloudEvent.getProcessDefinitionKey());
        assertThat(auditEventEntity.getBusinessKey()).isEqualTo(cloudEvent.getBusinessKey());
        assertThat(auditEventEntity.getParentProcessInstanceId()).isEqualTo(cloudEvent.getParentProcessInstanceId());
    }

    private CloudTaskCandidateGroupAddedEventImpl createTaskCandidateGroupAddedEvent() {
        TaskCandidateGroupImpl taskCandidateGroup = new TaskCandidateGroupImpl("groupId", "1234-abc-5678-def");

        CloudTaskCandidateGroupAddedEventImpl candidateGroupAddedEvent = new CloudTaskCandidateGroupAddedEventImpl(
            "TaskCandidateGroupAddedEventId",
            System.currentTimeMillis(),
            taskCandidateGroup
        );
        candidateGroupAddedEvent.setEntityId("entityId");
        candidateGroupAddedEvent.setProcessInstanceId("processInstanceId");
        candidateGroupAddedEvent.setProcessDefinitionId("processDefinitionId");
        candidateGroupAddedEvent.setProcessDefinitionKey("processDefinitionKey");
        candidateGroupAddedEvent.setBusinessKey("businessKey");
        candidateGroupAddedEvent.setParentProcessInstanceId("parentProcessInstanceId");
        candidateGroupAddedEvent.setMessageId("messageId");
        candidateGroupAddedEvent.setSequenceNumber(0);

        return candidateGroupAddedEvent;
    }
}
