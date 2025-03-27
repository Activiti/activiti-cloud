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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskCreatedEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TaskCreatedEventHandlerTest {

    @InjectMocks
    private TaskCreatedEventHandler handler;

    @InjectMocks
    private ProcessVariableCreatedEventHandler processVariableCreatedEventHandler;

    @InjectMocks
    private TaskVariableCreatedEventHandler taskVariableCreatedEventHandler;

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityManagerFinder entityManagerFinder;

    @Test
    public void handleShouldCreateAndStoreTask() {
        //given
        CloudTaskCreatedEvent event = new CloudTaskCreatedEventImpl(buildTask());
        var expectedEventEntity = event.getEntity();

        ProcessInstanceEntity processInstanceEntity = buildProcessDefinitionEntity();

        when(entityManagerFinder.findProcessInstanceWithVariables(event.getEntity().getProcessInstanceId()))
            .thenReturn(Optional.of(processInstanceEntity));

        //when
        handler.handle(event);

        //then
        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(entityManager).persist(captor.capture());

        TaskEntity taskEntity = captor.getValue();

        assertThat(taskEntity.getId()).isEqualTo(expectedEventEntity.getId());
        assertThat(taskEntity.getName()).isEqualTo(expectedEventEntity.getName());
        assertThat(taskEntity.getProcessInstanceId()).isEqualTo(expectedEventEntity.getProcessInstanceId());
        assertThat(taskEntity.getRootProcessInstanceId()).isEqualTo(processInstanceEntity.getRootProcessInstanceId());
    }

    private ProcessInstanceEntity buildProcessDefinitionEntity() {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setRootProcessInstanceId("rootProcessInstanceId");
        processInstanceEntity.setProcessDefinitionName("name");
        return processInstanceEntity;
    }

    private static TaskImpl buildTask() {
        TaskImpl task = new TaskImpl("id", "name", Task.TaskStatus.CREATED);
        task.setProcessInstanceId("processInstanceId");
        return task;
    }

    @Test
    public void getHandledEventShouldReturnTaskCreatedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(TaskRuntimeEvent.TaskEvents.TASK_CREATED.name());
    }
}
