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

import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.events.CloudVariableDeletedEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessVariableEntityDeletedHandlerTest {

    @InjectMocks
    private ProcessVariableDeletedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void handleRemoveVariableFromProcessAnDeleteIt() {
        //given
        CloudVariableDeletedEvent event = buildVariableDeletedEvent();

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setStatus(ProcessInstanceStatus.CREATED);
        ProcessVariableEntity variableEntity = new ProcessVariableEntity();
        variableEntity.setName("var");
        processInstanceEntity.getVariables().add(variableEntity);

        when(entityManager.createEntityGraph(ProcessInstanceEntity.class)).thenReturn(mock(EntityGraph.class));
        given(entityManager.find(eq(ProcessInstanceEntity.class),
                                 eq("procInstId"),
                                 any(Map.class))).willReturn(processInstanceEntity);

        //when
        handler.handle(event);

        //then
        verify(entityManager).remove(variableEntity);
        assertThat(processInstanceEntity.getVariables()).isEmpty();

    }

    private static CloudVariableDeletedEvent buildVariableDeletedEvent() {
        return new CloudVariableDeletedEventImpl(new VariableInstanceImpl<>("var", "string", "test", "procInstId", null));
    }

}
