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

import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.impl.IntegrationErrorImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntityImpl;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.engine.runtime.ExecutionQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ServiceTaskIntegrationErrorEventHandlerTest {

    private static final String EXECUTION_ID = "execId";
    private static final String ENTITY_ID = "entityId";
    private static final String CLIENT_ID = "entityId";

    @InjectMocks
    private ServiceTaskIntegrationErrorEventHandler handler;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private IntegrationContextService integrationContextService;

    @Captor
    private ArgumentCaptor<Command<?>> commandArgumentCaptor;

    @Mock
    private ExecutionQuery executionQuery;

    @Mock
    private ManagementService managementService;

    @BeforeEach
    public void setUp() {
        when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.executionId(EXECUTION_ID)).thenReturn(executionQuery);

    }

    @Test
    public void should_propagateErrorAndAggregateEvent_when_clientIdMatches() {
        //given
        IntegrationContextEntityImpl integrationContextEntity = buildIntegrationContextEntity();
        given(integrationContextService.findById(integrationContextEntity.getId())).willReturn(integrationContextEntity);

        ExecutionEntity executionEntity = mock(ExecutionEntity.class);
        given(executionEntity.getActivityId()).willReturn(CLIENT_ID);

        when(runtimeService.createExecutionQuery()
            .executionId(EXECUTION_ID)
            .list()).thenReturn(Collections.singletonList(executionEntity));

        IntegrationContextImpl integrationContext = buildIntegrationContext();
        IntegrationError integrationErrorEvent = new IntegrationErrorImpl(new IntegrationRequestImpl(integrationContext),
            new CloudBpmnError("Test Error"));

        //when
        handler.accept(integrationErrorEvent);

        //then
        verify(integrationContextService).deleteIntegrationContext(integrationContextEntity);
        verify(managementService).executeCommand(commandArgumentCaptor.capture());
        final Command<?> command = commandArgumentCaptor.getValue();
        assertThat(command).isExactlyInstanceOf(CompositeCommand.class);
        CompositeCommand compositeCommand = (CompositeCommand) command;
        assertThat(compositeCommand.getCommands().get(0)).isInstanceOf(PropagateCloudBpmnErrorCmd.class);
        assertThat(compositeCommand.getCommands().get(1)).isInstanceOf(AggregateIntegrationErrorReceivedClosingEventCmd.class);
    }

    @Test
    public void should_AggregateEventButNotPropagateError_when_clientIdDoesNotMatch() {
        //given
        IntegrationContextEntityImpl integrationContextEntity = buildIntegrationContextEntity();
        given(integrationContextService.findById(integrationContextEntity.getId())).willReturn(integrationContextEntity);

        ExecutionEntity executionEntity = mock(ExecutionEntity.class);
        given(executionEntity.getActivityId()).willReturn("idDifferentFromExpected");

        when(runtimeService.createExecutionQuery()
            .executionId(EXECUTION_ID)
            .list()).thenReturn(Collections.singletonList(executionEntity));

        IntegrationContextImpl integrationContext = buildIntegrationContext();
        IntegrationError integrationErrorEvent = new IntegrationErrorImpl(new IntegrationRequestImpl(integrationContext),
            new CloudBpmnError("Test Error"));

        //when
        handler.accept(integrationErrorEvent);

        //then
        verify(integrationContextService).deleteIntegrationContext(integrationContextEntity);
        verify(managementService).executeCommand(commandArgumentCaptor.capture());
        final Command<?> command = commandArgumentCaptor.getValue();
        assertThat(command).isExactlyInstanceOf(AggregateIntegrationErrorReceivedEventCmd.class);
    }

    private IntegrationContextEntityImpl buildIntegrationContextEntity() {
        IntegrationContextEntityImpl integrationContextEntity = new IntegrationContextEntityImpl();
        integrationContextEntity.setExecutionId(EXECUTION_ID);
        integrationContextEntity.setId(ENTITY_ID);
        return integrationContextEntity;
    }

    private IntegrationContextImpl buildIntegrationContext() {
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setId(ENTITY_ID);
        integrationContext.setClientId(CLIENT_ID);

        return integrationContext;
    }

}
