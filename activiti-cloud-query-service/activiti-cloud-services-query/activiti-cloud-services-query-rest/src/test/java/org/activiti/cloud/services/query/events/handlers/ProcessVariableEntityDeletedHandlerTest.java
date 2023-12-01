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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.events.CloudVariableDeletedEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProcessVariableEntityDeletedHandlerTest {

    @InjectMocks
    private ProcessVariableDeletedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityManagerFinder entityManagerFinder;

    @Test
    public void handleRemoveVariableFromProcessAnDeleteIt() {
        //given
        CloudVariableDeletedEvent event = buildVariableDeletedEvent();

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setStatus(ProcessInstanceStatus.CREATED);
        ProcessVariableEntity variableEntity = new ProcessVariableEntity();
        variableEntity.setName("var");
        processInstanceEntity.getVariables().add(variableEntity);

        given(entityManagerFinder.findProcessInstanceWithVariables("procInstId"))
            .willReturn(Optional.of(processInstanceEntity));

        //when
        handler.handle(event);

        //then
        verify(entityManager).remove(variableEntity);
        assertThat(processInstanceEntity.getVariables()).isEmpty();
    }

    private static CloudVariableDeletedEvent buildVariableDeletedEvent() {
        return new CloudVariableDeletedEventImpl(
            new VariableInstanceImpl<>("var", "string", "test", "procInstId", null)
        );
    }
}
