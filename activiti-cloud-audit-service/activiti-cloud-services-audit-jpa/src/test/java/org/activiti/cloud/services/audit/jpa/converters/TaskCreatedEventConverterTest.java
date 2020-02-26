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

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.impl.TaskImpl;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TaskCreatedEventConverterTest {

    private TaskCreatedEventConverter eventConverter = new TaskCreatedEventConverter(new EventContextInfoAppender());
    
    @Test
    public void checkConvertToEntityTaskCreatedEvent() {
        //given
        CloudTaskCreatedEventImpl event = createTaskCreatedEvent();
           
        //when
        AuditEventEntity auditEventEntity = eventConverter.convertToEntity(event);
     
        //then
        assertThat(auditEventEntity).isNotNull();
        assertThat(auditEventEntity.getEntityId()).isEqualTo(event.getEntityId());
        assertThat(auditEventEntity.getProcessInstanceId()).isEqualTo(event.getProcessInstanceId());
        assertThat(auditEventEntity.getProcessDefinitionId()).isEqualTo(event.getProcessDefinitionId());
        assertThat(auditEventEntity.getProcessDefinitionKey()).isEqualTo(event.getProcessDefinitionKey());
        assertThat(auditEventEntity.getBusinessKey()).isEqualTo(event.getBusinessKey());
        assertThat(auditEventEntity.getParentProcessInstanceId()).isEqualTo(event.getParentProcessInstanceId());
    }
    
    @Test
    public void checkConvertToAPITaskCreatedEvent() {
        //given
        AuditEventEntity auditEventEntity = eventConverter.convertToEntity(createTaskCreatedEvent());
        
        //when
        CloudRuntimeEvent cloudEvent= eventConverter.convertToAPI(auditEventEntity);
        assertThat(cloudEvent).isNotNull();
        assertThat(auditEventEntity.getEntityId()).isEqualTo(cloudEvent.getEntityId());
        assertThat(auditEventEntity.getProcessInstanceId()).isEqualTo(cloudEvent.getProcessInstanceId());
        assertThat(auditEventEntity.getProcessDefinitionId()).isEqualTo(cloudEvent.getProcessDefinitionId());
        assertThat(auditEventEntity.getProcessDefinitionKey()).isEqualTo(cloudEvent.getProcessDefinitionKey());
        assertThat(auditEventEntity.getBusinessKey()).isEqualTo(cloudEvent.getBusinessKey());
        assertThat(auditEventEntity.getParentProcessInstanceId()).isEqualTo(cloudEvent.getParentProcessInstanceId());
    }
    
    private CloudTaskCreatedEventImpl createTaskCreatedEvent() {
        //given
        TaskImpl taskCreated = new TaskImpl("1234-abc-5678-def",
                                            "my task",
                                            Task.TaskStatus.CREATED);
        taskCreated.setTaskDefinitionKey("taskDefinitionKey");
        CloudTaskCreatedEventImpl cloudTaskCreatedEvent = new CloudTaskCreatedEventImpl("TaskCreatedEventId",
                                                                                        System.currentTimeMillis(),
                                                                                        taskCreated);
        cloudTaskCreatedEvent.setEntityId("entityId");
        cloudTaskCreatedEvent.setProcessInstanceId("processInstanceId");
        cloudTaskCreatedEvent.setProcessDefinitionId("processDefinitionId");
        cloudTaskCreatedEvent.setProcessDefinitionKey("processDefinitionKey");
        cloudTaskCreatedEvent.setBusinessKey("businessKey");
        cloudTaskCreatedEvent.setParentProcessInstanceId("parentProcessInstanceId");
        cloudTaskCreatedEvent.setMessageId("messageId");
        cloudTaskCreatedEvent.setSequenceNumber(0);
        
        return cloudTaskCreatedEvent;
    }
}