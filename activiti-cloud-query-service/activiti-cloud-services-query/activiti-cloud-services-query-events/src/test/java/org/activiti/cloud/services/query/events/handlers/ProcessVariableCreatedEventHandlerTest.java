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
package org.activiti.cloud.services.query.events.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.Set;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessVariableCreatedEventHandlerTest {

    @InjectMocks
    private ProcessVariableCreatedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityManagerFinder entityManagerFinder;

    @Test
    void should_persistVariable_when_handleCalled() {
        var variable = new VariableInstanceImpl<>("var", "string", "value", "procInstId", null);
        var event = new CloudVariableCreatedEventImpl("eventId", System.currentTimeMillis(), variable);
        event.setMessageId("msg-003");
        event.setSequenceNumber(7);
        event.setCommandId("cmd-abc");

        var processInstanceEntity = new ProcessInstanceEntity();
        when(entityManagerFinder.findProcessInstanceWithVariables("procInstId")).thenReturn(
            Optional.of(processInstanceEntity)
        );
        when(entityManagerFinder.findTasksWithProcessVariables("procInstId")).thenReturn(Set.of());

        handler.handle(event);

        var captor = ArgumentCaptor.forClass(ProcessVariableEntity.class);
        verify(entityManager).persist(captor.capture());
        assertThat(captor.getValue())
            .extracting(
                ProcessVariableEntity::getName,
                ProcessVariableEntity::getType,
                ProcessVariableEntity::getValue,
                ProcessVariableEntity::getProcessInstanceId,
                ProcessVariableEntity::getProcessInstance
            )
            .containsExactly("var", "string", "value", "procInstId", processInstanceEntity);
    }
}
