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
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserAddedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskCandidateUserAddedEventEntity;
import org.activiti.api.task.model.impl.TaskCandidateUserImpl;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TaskCandidateUserAddedEventConverterTest {

    private  TaskCandidateUserAddedEventConverter eventConverter = new TaskCandidateUserAddedEventConverter(new EventContextInfoAppender());

    @Test
    public void checkConvertToEntityTaskCandidateUserAddedEvent() {
        //given
        CloudTaskCandidateUserAddedEventImpl event = createTaskCandidateUserAddedEvent();
           
        //when
        AuditEventEntity auditEventEntity = eventConverter.convertToEntity(event);
     
        //then
        assertThat(auditEventEntity).isNotNull();
        assertThat(((TaskCandidateUserAddedEventEntity)auditEventEntity).getCandidateUser().getTaskId()).isEqualTo(event.getEntity().getTaskId());
        assertThat(((TaskCandidateUserAddedEventEntity)auditEventEntity).getCandidateUser().getUserId()).isEqualTo(event.getEntity().getUserId());
        assertThat(auditEventEntity.getEntityId()).isEqualTo(event.getEntityId());
        assertThat(auditEventEntity.getProcessInstanceId()).isEqualTo(event.getProcessInstanceId());
        assertThat(auditEventEntity.getProcessDefinitionId()).isEqualTo(event.getProcessDefinitionId());
        assertThat(auditEventEntity.getProcessDefinitionKey()).isEqualTo(event.getProcessDefinitionKey());
        assertThat(auditEventEntity.getBusinessKey()).isEqualTo(event.getBusinessKey());
        assertThat(auditEventEntity.getParentProcessInstanceId()).isEqualTo(event.getParentProcessInstanceId());
    }
    
    @Test
    public void checkConvertToAPITaskCandidateUserAddedEvent() {
        //given
        AuditEventEntity auditEventEntity = eventConverter.convertToEntity(createTaskCandidateUserAddedEvent());
        
        //when
        CloudRuntimeEvent cloudEvent= eventConverter.convertToAPI(auditEventEntity);
        assertThat(cloudEvent).isNotNull();
        assertThat(((TaskCandidateUserAddedEventEntity)auditEventEntity).getCandidateUser().getTaskId()).isEqualTo(((CloudTaskCandidateUserAddedEventImpl)cloudEvent).getEntity().getTaskId());
        assertThat(((TaskCandidateUserAddedEventEntity)auditEventEntity).getCandidateUser().getUserId()).isEqualTo(((CloudTaskCandidateUserAddedEventImpl)cloudEvent).getEntity().getUserId());
        assertThat(auditEventEntity.getEntityId()).isEqualTo(cloudEvent.getEntityId());
        assertThat(auditEventEntity.getProcessInstanceId()).isEqualTo(cloudEvent.getProcessInstanceId());
        assertThat(auditEventEntity.getProcessDefinitionId()).isEqualTo(cloudEvent.getProcessDefinitionId());
        assertThat(auditEventEntity.getProcessDefinitionKey()).isEqualTo(cloudEvent.getProcessDefinitionKey());
        assertThat(auditEventEntity.getBusinessKey()).isEqualTo(cloudEvent.getBusinessKey());
        assertThat(auditEventEntity.getParentProcessInstanceId()).isEqualTo(cloudEvent.getParentProcessInstanceId());
    }
    
    private CloudTaskCandidateUserAddedEventImpl createTaskCandidateUserAddedEvent() {
        //given
        TaskCandidateUserImpl taskCandidateUser=new TaskCandidateUserImpl("userId", "1234-abc-5678-def");
        
        CloudTaskCandidateUserAddedEventImpl candidateUserAddedEvent = new CloudTaskCandidateUserAddedEventImpl("TaskCandidateUserAddedEventId",
                                                                                                            System.currentTimeMillis(),
                                                                                                            taskCandidateUser);
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