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

import org.activiti.api.task.model.events.TaskCandidateUserEvent;
import org.activiti.api.task.model.impl.TaskCandidateUserImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserAddedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskCandidateUserAddedEventEntity;
import org.junit.jupiter.api.Test;

class TaskCandidateUserAddedEventConverterTest {

    private final TaskCandidateUserAddedEventConverter eventConverter = new TaskCandidateUserAddedEventConverter(
        new EventContextInfoAppender()
    );

    @Test
    void should_returnTaskCandidateUserAdded_when_getSupportedEvent() {
        assertThat(eventConverter.getSupportedEvent())
            .isEqualTo(TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_ADDED.name());
    }

    @Test
    void should_buildEntityWithCandidateUserAndProcessContext_when_convertToEntity() {
        //given
        CloudTaskCandidateUserAddedEventImpl event = createTaskCandidateUserAddedEvent();

        //when
        AuditEventEntity auditEventEntity = eventConverter.convertToEntity(event);

        //then
        assertThat(auditEventEntity)
            .isNotNull()
            .isInstanceOf(TaskCandidateUserAddedEventEntity.class)
            .returns(
                event.getEntity().getTaskId(),
                e -> ((TaskCandidateUserAddedEventEntity) e).getCandidateUser().getTaskId()
            )
            .returns(
                event.getEntity().getUserId(),
                e -> ((TaskCandidateUserAddedEventEntity) e).getCandidateUser().getUserId()
            )
            .returns(event.getEntityId(), AuditEventEntity::getEntityId)
            .returns(event.getProcessInstanceId(), AuditEventEntity::getProcessInstanceId)
            .returns(event.getProcessDefinitionId(), AuditEventEntity::getProcessDefinitionId)
            .returns(event.getProcessDefinitionKey(), AuditEventEntity::getProcessDefinitionKey)
            .returns(event.getBusinessKey(), AuditEventEntity::getBusinessKey)
            .returns(event.getParentProcessInstanceId(), AuditEventEntity::getParentProcessInstanceId);
    }

    @Test
    void should_buildCloudEventWithCandidateUserAndProcessContext_when_convertToAPI() {
        //given
        AuditEventEntity auditEventEntity = eventConverter.convertToEntity(createTaskCandidateUserAddedEvent());
        TaskCandidateUserAddedEventEntity entity = (TaskCandidateUserAddedEventEntity) auditEventEntity;

        //when
        CloudRuntimeEvent cloudEvent = eventConverter.convertToAPI(auditEventEntity);

        //then
        assertThat(cloudEvent)
            .isNotNull()
            .isInstanceOf(CloudTaskCandidateUserAddedEventImpl.class)
            .returns(
                entity.getCandidateUser().getTaskId(),
                e -> ((CloudTaskCandidateUserAddedEventImpl) e).getEntity().getTaskId()
            )
            .returns(
                entity.getCandidateUser().getUserId(),
                e -> ((CloudTaskCandidateUserAddedEventImpl) e).getEntity().getUserId()
            )
            .returns(entity.getEntityId(), CloudRuntimeEvent::getEntityId)
            .returns(entity.getProcessInstanceId(), CloudRuntimeEvent::getProcessInstanceId)
            .returns(entity.getProcessDefinitionId(), CloudRuntimeEvent::getProcessDefinitionId)
            .returns(entity.getProcessDefinitionKey(), CloudRuntimeEvent::getProcessDefinitionKey)
            .returns(entity.getBusinessKey(), CloudRuntimeEvent::getBusinessKey)
            .returns(entity.getParentProcessInstanceId(), CloudRuntimeEvent::getParentProcessInstanceId);
    }

    private CloudTaskCandidateUserAddedEventImpl createTaskCandidateUserAddedEvent() {
        TaskCandidateUserImpl taskCandidateUser = new TaskCandidateUserImpl("userId", "1234-abc-5678-def");

        CloudTaskCandidateUserAddedEventImpl candidateUserAddedEvent = new CloudTaskCandidateUserAddedEventImpl(
            "TaskCandidateUserAddedEventId",
            System.currentTimeMillis(),
            taskCandidateUser
        );
        candidateUserAddedEvent.setEntityId("entityId");
        candidateUserAddedEvent.setProcessInstanceId("processInstanceId");
        candidateUserAddedEvent.setProcessDefinitionId("processDefinitionId");
        candidateUserAddedEvent.setProcessDefinitionKey("processDefinitionKey");
        candidateUserAddedEvent.setBusinessKey("businessKey");
        candidateUserAddedEvent.setParentProcessInstanceId("parentProcessInstanceId");
        candidateUserAddedEvent.setMessageId("messageId");
        candidateUserAddedEvent.setSequenceNumber(0);

        return candidateUserAddedEvent;
    }
}
