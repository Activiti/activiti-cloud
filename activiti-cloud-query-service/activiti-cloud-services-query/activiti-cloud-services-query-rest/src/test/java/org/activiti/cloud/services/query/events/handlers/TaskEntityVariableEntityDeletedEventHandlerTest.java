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
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.api.task.model.Task.TaskStatus;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TaskEntityVariableEntityDeletedEventHandlerTest {

    @InjectMocks
    private TaskVariableDeletedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityManagerFinder entityManagerFinder;

    @Test
    public void handleShouldDeleteIt() {
        //given
        VariableInstanceImpl<String> variableInstance = new VariableInstanceImpl<>(
            "var",
            "string",
            "v1",
            "procInstId",
            "taskId"
        );
        CloudVariableDeletedEventImpl event = new CloudVariableDeletedEventImpl(variableInstance);

        TaskVariableEntity variableEntity = new TaskVariableEntity();
        variableEntity.setName("var");
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setStatus(TaskStatus.CREATED);
        taskEntity.getVariables().add(variableEntity);

        when(entityManagerFinder.findTaskWithVariables("taskId")).thenReturn(Optional.of(taskEntity));

        //when
        handler.handle(event);

        //then
        verify(entityManager).remove(variableEntity);
        assertThat(taskEntity.getVariables()).isEmpty();
    }
}
