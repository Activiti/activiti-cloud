/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.services.connectors.behavior;

import java.util.ArrayList;

import org.activiti.bpmn.model.ServiceTask;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntityImpl;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextManager;
import org.activiti.services.connectors.model.IntegrationRequestEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class MQServiceTaskBehaviorTest {

    @Spy
    @InjectMocks
    private MQServiceTaskBehavior behavior;

    @Mock
    private IntegrationContextManager integrationContextManager;

    @Mock
    private IntegrationProducerCommandContextCloseListener contextCloseListener;

    @Mock
    private CommandContext commandContext;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(behavior.getCurrentCommandContext()).thenReturn(commandContext);
    }

    @Test
    public void executeShouldStoreTheIntegrationContextAndSendAMessage() throws Exception {
        //given
        String connectorType = "payment";
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setImplementation(connectorType);

        DelegateExecution execution = mock(DelegateExecution.class);
        given(execution.getId()).willReturn("execId");
        given(execution.getProcessInstanceId()).willReturn("procInstId");
        given(execution.getProcessDefinitionId()).willReturn("procDefId");
        given(execution.getCurrentFlowElement()).willReturn(serviceTask);

        IntegrationContextEntityImpl entity = new IntegrationContextEntityImpl();
        given(integrationContextManager.create()).willReturn(entity);

        ArrayList<Message<IntegrationRequestEvent>> messages = new ArrayList<>();
        given(commandContext.getGenericAttribute(IntegrationProducerCommandContextCloseListener.PROCESS_ENGINE_INTEGRATION_EVENTS))
                .willReturn(messages);

        //when
        behavior.execute(execution);

        //then
        verify(integrationContextManager).insert(entity);
        assertThat(entity.getExecutionId()).isEqualTo("execId");
        assertThat(entity.getProcessDefinitionId()).isEqualTo("procDefId");
        assertThat(entity.getProcessInstanceId()).isEqualTo("procInstId");

        assertThat(messages).hasSize(1);
        Message<IntegrationRequestEvent> message = messages.get(0);
        assertThat(message.getPayload().getExecutionId()).isNotNull();
        assertThat(message.getPayload().getProcessInstanceId()).isEqualTo("procInstId");
        assertThat(message.getPayload().getProcessDefinitionId()).isEqualTo("procDefId");
        assertThat(message.getHeaders().get("connectorType")).isEqualTo(connectorType);
    }

    @Test
    public void executeShouldRegisterCloseListenerWhenAbsent() throws Exception {
        //given
        given(commandContext.hasCloseListener(IntegrationProducerCommandContextCloseListener.class)).willReturn(false);
        IntegrationContextEntityImpl entity = new IntegrationContextEntityImpl();
        given(integrationContextManager.create()).willReturn(entity);

        //when
        behavior.execute(anyExecution());

        //then
        verify(commandContext).addCloseListener(contextCloseListener);
    }

    @Test
    public void executeShouldNotRegisterCloseListenerWhenAlreadyPresent() throws Exception {
        //given

        given(commandContext.hasCloseListener(IntegrationProducerCommandContextCloseListener.class)).willReturn(true);

        IntegrationContextEntityImpl entity = new IntegrationContextEntityImpl();
        given(integrationContextManager.create()).willReturn(entity);

        //when
        behavior.execute(anyExecution());

        //then
        verify(commandContext, never()).addCloseListener(contextCloseListener);
    }

    private DelegateExecution anyExecution() {
        String connectorType = "payment";
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setImplementation(connectorType);

        DelegateExecution execution = mock(DelegateExecution.class);
        given(execution.getId()).willReturn("execId");
        given(execution.getCurrentFlowElement()).willReturn(serviceTask);
        return execution;
    }

    @Test
    public void triggerShouldCallLeave() throws Exception {
        //given
        DelegateExecution execution = mock(DelegateExecution.class);
        doNothing().when(behavior).leave(execution);

        //when
        behavior.trigger(execution, null, null);

        //then
        verify(behavior).leave(execution);
    }
}