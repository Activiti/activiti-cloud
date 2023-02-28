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
package org.activiti.services.connectors.channel;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationResultImpl;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.cmd.TriggerCmd;
import org.activiti.engine.impl.cmd.integration.DeleteIntegrationContextCmd;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntityImpl;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.engine.runtime.Execution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ServiceTaskIntegrationResultEventHandlerTest {

    private static final String EXECUTION_ID = "execId";
    private static final String ENTITY_ID = "entityId";
    private static final String PROC_INST_ID = "procInstId";
    private static final String PROC_DEF_ID = "procDefId";
    private static final String CLIENT_ID = "entityId";
    private static final String CLIENT_NAME = "serviceTaskName";
    private static final String CLIENT_TYPE = ServiceTask.class.getSimpleName();

    @InjectMocks
    private ServiceTaskIntegrationResultEventHandler handler;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RuntimeService runtimeService;

    @Mock
    private IntegrationContextService integrationContextService;

    @Mock
    private ManagementService managementService;

    @Test
    public void receive_should_triggerExecutionAndDeleteRelatedIntegrationContext() {
        //given
        IntegrationContextImpl integrationContext = buildIntegrationContext(Collections.singletonMap("var1", "v"));
        IntegrationContextEntityImpl integrationContextEntity = buildIntegrationContextEntity();
        given(integrationContextService.findById(integrationContext.getId())).willReturn(integrationContextEntity);

        List<Execution> executions = Collections.singletonList(buildExecutionEntity());
        when(runtimeService.createExecutionQuery().executionId(integrationContext.getExecutionId()).list()).thenReturn(executions);


        //when
        handler.receive(new IntegrationResultImpl(new IntegrationRequestImpl(), integrationContext));

        //then
        final ArgumentCaptor<CompositeCommand> captor = ArgumentCaptor.forClass(
            CompositeCommand.class);
        verify(managementService).executeCommand(captor.capture());
        final CompositeCommand command = captor.getValue();
        assertThat(command.getCommands()).hasSize(3);
        assertThat(command.getCommands().get(0)).isInstanceOf(DeleteIntegrationContextCmd.class);
        assertThat(command.getCommands().get(1)).isInstanceOf(TriggerCmd.class);
        assertThat(command.getCommands().get(2)).isInstanceOf(AggregateIntegrationResultReceivedEventCmd.class);
    }

    private ExecutionEntity buildExecutionEntity() {
        final ExecutionEntity executionEntity = mock(ExecutionEntity.class);
        when(executionEntity.getActivityId()).thenReturn(CLIENT_ID);
        return executionEntity;
    }

    private IntegrationContextEntityImpl buildIntegrationContextEntity() {
        IntegrationContextEntityImpl integrationContextEntity = new IntegrationContextEntityImpl();
        integrationContextEntity.setExecutionId(EXECUTION_ID);
        integrationContextEntity.setId(ENTITY_ID);
        integrationContextEntity.setProcessInstanceId(PROC_INST_ID);
        integrationContextEntity.setProcessDefinitionId(PROC_DEF_ID);
        return integrationContextEntity;
    }

    @Test
    public void receiveShouldDoNothingWhenIntegrationContextsIsNull() {
        //given
        IntegrationContextImpl integrationContext = buildIntegrationContext(Collections.singletonMap("var1", "v"));
        given(integrationContextService.findById(integrationContext.getId())).willReturn(null);

        //when
        handler.receive(new IntegrationResultImpl(new IntegrationRequestImpl(), integrationContext));

        //then
        verify(managementService, never()).executeCommand(any());
    }

    private IntegrationContextImpl buildIntegrationContext(Map<String, Object> variables) {
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setExecutionId(EXECUTION_ID);
        integrationContext.setId(ENTITY_ID);
        integrationContext.setProcessDefinitionId(PROC_DEF_ID);
        integrationContext.setProcessInstanceId(PROC_INST_ID);
        integrationContext.addOutBoundVariables(variables);
        integrationContext.setClientId(CLIENT_ID);
        integrationContext.setClientName(CLIENT_NAME);
        integrationContext.setClientType(CLIENT_TYPE);
        return integrationContext;
    }

}
