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
package org.activiti.cloud.services.query.events.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.UUID;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TaskEntityCreatedEventHandlerTest {

    @InjectMocks
    private TaskCreatedEventHandler handler;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityManagerFinder entityManagerFinder;

    @Mock
    private ProcessInstanceRepository processInstanceRepository;

    @Test
    public void handleShouldStoreNewTaskInstance() {
        //given
        ProcessInstanceEntity processInstanceEntity = mock(ProcessInstanceEntity.class);
        when(processInstanceEntity.getProcessDefinitionName()).thenReturn("processDefinitionName");
        when(processInstanceEntity.getTasks()).thenReturn(new LinkedHashSet<>());

        TaskImpl task = new TaskImpl(UUID.randomUUID().toString(), "task", Task.TaskStatus.CREATED);
        task.setCreatedDate(new Date());
        task.setProcessInstanceId(UUID.randomUUID().toString());
        task.setProcessDefinitionId("processDefinitionId");
        task.setTaskDefinitionKey("taskDefinitionKey");

        CloudTaskCreatedEventImpl event = new CloudTaskCreatedEventImpl(task);
        event.setServiceName("runtime-bundle-a");
        event.setProcessDefinitionVersion(10);
        event.setBusinessKey("businessKey");

        when(entityManagerFinder.findProcessInstanceWithTasks(task.getProcessInstanceId()))
            .thenReturn(Optional.of(processInstanceEntity));

        //when
        handler.handle(event);

        //then
        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(entityManager).persist(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Task.TaskStatus.CREATED);
        assertThat(captor.getValue().getLastModified()).isNotNull();
        assertThat(captor.getValue().getProcessInstance()).isEqualTo(processInstanceEntity);
        assertThat(captor.getValue().getServiceName()).isEqualTo(event.getServiceName());
        assertThat(captor.getValue().getProcessInstanceId()).isEqualTo(task.getProcessInstanceId());
        assertThat(captor.getValue().getProcessDefinitionId()).isEqualTo(task.getProcessDefinitionId());
        assertThat(captor.getValue().getProcessDefinitionVersion()).isEqualTo(event.getProcessDefinitionVersion());
        assertThat(captor.getValue().getBusinessKey()).isEqualTo(event.getBusinessKey());
        assertThat(captor.getValue().getTaskDefinitionKey()).isEqualTo(task.getTaskDefinitionKey());
        assertThat(captor.getValue().getProcessDefinitionName()).isEqualTo("processDefinitionName");
        assertThat(processInstanceEntity.getTasks()).containsOnly(captor.getValue());
    }

    @Test
    public void getHandledEventShouldReturnTaskCreatedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(TaskRuntimeEvent.TaskEvents.TASK_CREATED.name());
    }
}
