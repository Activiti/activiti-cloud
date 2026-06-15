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
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupRemovedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskCandidateGroupRemovedEventEntity;
import org.junit.jupiter.api.Test;

class TaskCandidateGroupRemovedEventConverterTest {

    private final TaskCandidateGroupRemovedEventConverter eventConverter = new TaskCandidateGroupRemovedEventConverter(
        new EventContextInfoAppender()
    );

    @Test
    void should_returnTaskCandidateGroupRemoved_when_getSupportedEvent() {
        assertThat(eventConverter.getSupportedEvent())
            .isEqualTo(TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_REMOVED.name());
    }

    @Test
    void should_buildEntityWithCandidateGroupAndProcessContext_when_convertToEntity() {
        //given
        CloudTaskCandidateGroupRemovedEventImpl event = createTaskCandidateGroupRemovedEvent();

        //when
        AuditEventEntity auditEventEntity = eventConverter.convertToEntity(event);

        //then
        assertThat(auditEventEntity)
            .isNotNull()
            .isInstanceOf(TaskCandidateGroupRemovedEventEntity.class)
            .returns(
                event.getEntity().getTaskId(),
                e -> ((TaskCandidateGroupRemovedEventEntity) e).getCandidateGroup().getTaskId()
            )
            .returns(
                event.getEntity().getGroupId(),
                e -> ((TaskCandidateGroupRemovedEventEntity) e).getCandidateGroup().getGroupId()
            )
            .returns(event.getEntityId(), AuditEventEntity::getEntityId)
            .returns(event.getProcessInstanceId(), AuditEventEntity::getProcessInstanceId)
            .returns(event.getProcessDefinitionId(), AuditEventEntity::getProcessDefinitionId)
            .returns(event.getProcessDefinitionKey(), AuditEventEntity::getProcessDefinitionKey)
            .returns(event.getBusinessKey(), AuditEventEntity::getBusinessKey)
            .returns(event.getParentProcessInstanceId(), AuditEventEntity::getParentProcessInstanceId);
    }

    @Test
    void should_buildCloudEventWithCandidateGroupAndProcessContext_when_convertToAPI() {
        //given
        AuditEventEntity auditEventEntity = eventConverter.convertToEntity(createTaskCandidateGroupRemovedEvent());
        TaskCandidateGroupRemovedEventEntity entity = (TaskCandidateGroupRemovedEventEntity) auditEventEntity;

        //when
        CloudRuntimeEvent cloudEvent = eventConverter.convertToAPI(auditEventEntity);

        //then
        assertThat(cloudEvent)
            .isNotNull()
            .isInstanceOf(CloudTaskCandidateGroupRemovedEventImpl.class)
            .returns(
                entity.getCandidateGroup().getTaskId(),
                e -> ((CloudTaskCandidateGroupRemovedEventImpl) e).getEntity().getTaskId()
            )
            .returns(
                entity.getCandidateGroup().getGroupId(),
                e -> ((CloudTaskCandidateGroupRemovedEventImpl) e).getEntity().getGroupId()
            )
            .returns(entity.getEntityId(), CloudRuntimeEvent::getEntityId)
            .returns(entity.getProcessInstanceId(), CloudRuntimeEvent::getProcessInstanceId)
            .returns(entity.getProcessDefinitionId(), CloudRuntimeEvent::getProcessDefinitionId)
            .returns(entity.getProcessDefinitionKey(), CloudRuntimeEvent::getProcessDefinitionKey)
            .returns(entity.getBusinessKey(), CloudRuntimeEvent::getBusinessKey)
            .returns(entity.getParentProcessInstanceId(), CloudRuntimeEvent::getParentProcessInstanceId);
    }

    private CloudTaskCandidateGroupRemovedEventImpl createTaskCandidateGroupRemovedEvent() {
        TaskCandidateGroupImpl taskCandidateGroup = new TaskCandidateGroupImpl("groupId", "1234-abc-5678-def");

        CloudTaskCandidateGroupRemovedEventImpl candidateGroupRemovedEvent = new CloudTaskCandidateGroupRemovedEventImpl(
            "TaskCandidateGroupRemovedEventId",
            System.currentTimeMillis(),
            taskCandidateGroup
        );
        candidateGroupRemovedEvent.setEntityId("entityId");
        candidateGroupRemovedEvent.setProcessInstanceId("processInstanceId");
        candidateGroupRemovedEvent.setProcessDefinitionId("processDefinitionId");
        candidateGroupRemovedEvent.setProcessDefinitionKey("processDefinitionKey");
        candidateGroupRemovedEvent.setBusinessKey("businessKey");
        candidateGroupRemovedEvent.setParentProcessInstanceId("parentProcessInstanceId");
        candidateGroupRemovedEvent.setMessageId("messageId");
        candidateGroupRemovedEvent.setSequenceNumber(0);

        return candidateGroupRemovedEvent;
    }
}
