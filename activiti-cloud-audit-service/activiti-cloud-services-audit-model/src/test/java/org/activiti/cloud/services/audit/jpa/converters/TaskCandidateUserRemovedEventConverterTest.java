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
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserRemovedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskCandidateUserRemovedEventEntity;
import org.junit.jupiter.api.Test;

class TaskCandidateUserRemovedEventConverterTest {

    private final TaskCandidateUserRemovedEventConverter eventConverter = new TaskCandidateUserRemovedEventConverter(
        new EventContextInfoAppender()
    );

    @Test
    void should_returnTaskCandidateUserRemoved_when_getSupportedEvent() {
        assertThat(eventConverter.getSupportedEvent()).isEqualTo(
            TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_REMOVED.name()
        );
    }

    @Test
    void should_buildEntityWithCandidateUserAndProcessContext_when_convertToEntity() {
        //given
        CloudTaskCandidateUserRemovedEventImpl event = createTaskCandidateUserRemovedEvent();

        //when
        AuditEventEntity auditEventEntity = eventConverter.convertToEntity(event);

        //then
        assertThat(auditEventEntity)
            .isNotNull()
            .isInstanceOf(TaskCandidateUserRemovedEventEntity.class)
            .returns(event.getEntity().getTaskId(), e ->
                ((TaskCandidateUserRemovedEventEntity) e).getCandidateUser().getTaskId()
            )
            .returns(event.getEntity().getUserId(), e ->
                ((TaskCandidateUserRemovedEventEntity) e).getCandidateUser().getUserId()
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
        AuditEventEntity auditEventEntity = eventConverter.convertToEntity(createTaskCandidateUserRemovedEvent());
        TaskCandidateUserRemovedEventEntity entity = (TaskCandidateUserRemovedEventEntity) auditEventEntity;

        //when
        CloudRuntimeEvent cloudEvent = eventConverter.convertToAPI(auditEventEntity);

        //then
        assertThat(cloudEvent)
            .isNotNull()
            .isInstanceOf(CloudTaskCandidateUserRemovedEventImpl.class)
            .returns(entity.getCandidateUser().getTaskId(), e ->
                ((CloudTaskCandidateUserRemovedEventImpl) e).getEntity().getTaskId()
            )
            .returns(entity.getCandidateUser().getUserId(), e ->
                ((CloudTaskCandidateUserRemovedEventImpl) e).getEntity().getUserId()
            )
            .returns(entity.getEntityId(), CloudRuntimeEvent::getEntityId)
            .returns(entity.getProcessInstanceId(), CloudRuntimeEvent::getProcessInstanceId)
            .returns(entity.getProcessDefinitionId(), CloudRuntimeEvent::getProcessDefinitionId)
            .returns(entity.getProcessDefinitionKey(), CloudRuntimeEvent::getProcessDefinitionKey)
            .returns(entity.getBusinessKey(), CloudRuntimeEvent::getBusinessKey)
            .returns(entity.getParentProcessInstanceId(), CloudRuntimeEvent::getParentProcessInstanceId);
    }

    private CloudTaskCandidateUserRemovedEventImpl createTaskCandidateUserRemovedEvent() {
        TaskCandidateUserImpl taskCandidateUser = new TaskCandidateUserImpl("userId", "1234-abc-5678-def");

        CloudTaskCandidateUserRemovedEventImpl candidateUserRemovedEvent = new CloudTaskCandidateUserRemovedEventImpl(
            "TaskCandidateUserRemovedEventId",
            System.currentTimeMillis(),
            taskCandidateUser
        );
        candidateUserRemovedEvent.setEntityId("entityId");
        candidateUserRemovedEvent.setProcessInstanceId("processInstanceId");
        candidateUserRemovedEvent.setProcessDefinitionId("processDefinitionId");
        candidateUserRemovedEvent.setProcessDefinitionKey("processDefinitionKey");
        candidateUserRemovedEvent.setBusinessKey("businessKey");
        candidateUserRemovedEvent.setParentProcessInstanceId("parentProcessInstanceId");
        candidateUserRemovedEvent.setMessageId("messageId");
        candidateUserRemovedEvent.setSequenceNumber(0);

        return candidateUserRemovedEvent;
    }
}
