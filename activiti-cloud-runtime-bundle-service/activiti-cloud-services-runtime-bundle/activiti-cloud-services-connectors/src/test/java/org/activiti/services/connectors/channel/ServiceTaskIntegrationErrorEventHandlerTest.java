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
package org.activiti.services.connectors.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.impl.IntegrationErrorImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationErrorReceivedEventImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.impl.cmd.integration.DeleteIntegrationContextCmd;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntityImpl;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.engine.runtime.ExecutionQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private ArgumentCaptor<CompositeCommand> commandArgumentCaptor;

    @Mock
    private ExecutionQuery executionQuery;

    @Mock
    private ManagementService managementService;

    @Mock
    private EphemeralMappingResolver ephemeralMappingResolver;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RuntimeBundleProperties runtimeBundleProperties;

    @Mock
    private ProcessEngineEventsAggregator processEngineEventsAggregator;

    @BeforeEach
    public void setUp() {
        when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.executionId(EXECUTION_ID)).thenReturn(executionQuery);
    }

    @Test
    public void should_propagateErrorAndAggregateEvent_when_clientIdMatches() {
        //given
        IntegrationContextEntityImpl integrationContextEntity = buildIntegrationContextEntity();
        given(integrationContextService.findById(integrationContextEntity.getId())).willReturn(
            integrationContextEntity
        );

        ExecutionEntity executionEntity = mock(ExecutionEntity.class);
        given(executionEntity.getActivityId()).willReturn(CLIENT_ID);

        when(runtimeService.createExecutionQuery().executionId(EXECUTION_ID).list()).thenReturn(
            Collections.singletonList(executionEntity)
        );

        IntegrationContextImpl integrationContext = buildIntegrationContext();
        IntegrationError integrationErrorEvent = new IntegrationErrorImpl(
            new IntegrationRequestImpl(integrationContext),
            new CloudBpmnError("Test Error")
        );

        //when
        handler.receive(integrationErrorEvent);

        //then
        verify(managementService).executeCommand(commandArgumentCaptor.capture());
        final CompositeCommand compositeCommand = commandArgumentCaptor.getValue();
        assertThat(compositeCommand.getCommands()).hasSize(3);
        assertThat(compositeCommand.getCommands().getFirst()).isInstanceOf(DeleteIntegrationContextCmd.class);
        assertThat(compositeCommand.getCommands().get(1)).isInstanceOf(PropagateCloudBpmnErrorCmd.class);
        assertThat(compositeCommand.getCommands().get(2)).isInstanceOf(
            AggregateIntegrationErrorReceivedClosingEventCmd.class
        );
    }

    @Test
    public void should_AggregateEventButNotPropagateError_when_clientIdDoesNotMatch() {
        //given
        IntegrationContextEntityImpl integrationContextEntity = buildIntegrationContextEntity();
        given(integrationContextService.findById(integrationContextEntity.getId())).willReturn(
            integrationContextEntity
        );

        ExecutionEntity executionEntity = mock(ExecutionEntity.class);
        given(executionEntity.getActivityId()).willReturn("idDifferentFromExpected");

        when(runtimeService.createExecutionQuery().executionId(EXECUTION_ID).list()).thenReturn(
            Collections.singletonList(executionEntity)
        );

        IntegrationContextImpl integrationContext = buildIntegrationContext();
        IntegrationError integrationErrorEvent = new IntegrationErrorImpl(
            new IntegrationRequestImpl(integrationContext),
            new CloudBpmnError("Test Error")
        );

        //when
        handler.receive(integrationErrorEvent);

        //then
        verify(managementService).executeCommand(commandArgumentCaptor.capture());
        final CompositeCommand compositeCommand = commandArgumentCaptor.getValue();
        assertThat(compositeCommand.getCommands()).hasSize(2);
        assertThat(compositeCommand.getCommands().getFirst()).isInstanceOf(DeleteIntegrationContextCmd.class);
        assertThat(compositeCommand.getCommands().get(1)).isInstanceOf(AggregateIntegrationErrorReceivedEventCmd.class);
        // ephemeral flag is resolved from the runtime bundle's own integration context (mapping), not the error payload
        verify(ephemeralMappingResolver).isEphemeral(integrationContextEntity);
    }

    @Test
    public void should_AggregateEventButNotPropagateError_when_errorClassNameDoesNotMatch() {
        //given
        IntegrationContextEntityImpl integrationContextEntity = buildIntegrationContextEntity();
        given(integrationContextService.findById(integrationContextEntity.getId())).willReturn(
            integrationContextEntity
        );

        ExecutionEntity executionEntity = mock(ExecutionEntity.class);

        when(runtimeService.createExecutionQuery().executionId(EXECUTION_ID).list()).thenReturn(
            Collections.singletonList(executionEntity)
        );

        IntegrationContextImpl integrationContext = buildIntegrationContext();
        IntegrationError integrationErrorEvent = new IntegrationErrorImpl(
            new IntegrationRequestImpl(integrationContext),
            new AnotherCloudException()
        );

        //when
        handler.receive(integrationErrorEvent);

        //then
        verify(managementService).executeCommand(commandArgumentCaptor.capture());
        verify(executionEntity, never()).getActivityId();
        final CompositeCommand compositeCommand = commandArgumentCaptor.getValue();
        assertThat(compositeCommand.getCommands()).hasSize(2);
        assertThat(compositeCommand.getCommands().getFirst()).isInstanceOf(DeleteIntegrationContextCmd.class);
        assertThat(compositeCommand.getCommands().get(1)).isInstanceOf(AggregateIntegrationErrorReceivedEventCmd.class);
    }

    @Test
    public void should_throwException_when_propagating_cloudBpmnError() {
        //given
        IntegrationContextEntityImpl integrationContextEntity = buildIntegrationContextEntity();
        given(integrationContextService.findById(integrationContextEntity.getId())).willReturn(
            integrationContextEntity
        );

        ExecutionEntity executionEntity = mock(ExecutionEntity.class);
        given(executionEntity.getActivityId()).willReturn(CLIENT_ID);

        when(runtimeService.createExecutionQuery().executionId(EXECUTION_ID).list()).thenReturn(
            Collections.singletonList(executionEntity)
        );

        IntegrationContextImpl integrationContext = buildIntegrationContext();
        IntegrationError integrationErrorEvent = new IntegrationErrorImpl(
            new IntegrationRequestImpl(integrationContext),
            new CloudBpmnError("Test Error")
        );

        when(managementService.executeCommand(any())).thenAnswer(invocation -> {
            CompositeCommand arg = invocation.getArgument(0);
            if (arg.getCommands().stream().anyMatch(PropagateCloudBpmnErrorCmd.class::isInstance)) {
                throw new BpmnError("some exception");
            }
            return arg;
        });

        //when
        handler.receive(integrationErrorEvent);

        //then
        verify(managementService, times(2)).executeCommand(commandArgumentCaptor.capture());
        final List<CompositeCommand> compositeCommands = commandArgumentCaptor.getAllValues();
        var propagateCloudBpmnErrorCmd = compositeCommands.getFirst();
        assertThat(propagateCloudBpmnErrorCmd.getCommands()).hasSize(3);
        assertThat(propagateCloudBpmnErrorCmd.getCommands().getFirst()).isInstanceOf(DeleteIntegrationContextCmd.class);
        assertThat(propagateCloudBpmnErrorCmd.getCommands().get(1)).isInstanceOf(PropagateCloudBpmnErrorCmd.class);
        assertThat(propagateCloudBpmnErrorCmd.getCommands().get(2)).isInstanceOf(
            AggregateIntegrationErrorReceivedClosingEventCmd.class
        );

        var compositeCommand = compositeCommands.get(1);
        assertThat(compositeCommand.getCommands()).hasSize(2);
        assertThat(compositeCommand.getCommands().getFirst()).isInstanceOf(DeleteIntegrationContextCmd.class);
        assertThat(compositeCommand.getCommands().get(1)).isInstanceOf(AggregateIntegrationErrorReceivedEventCmd.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void should_useEphemeralFlagFromMapping_andIgnoreConnectorFlag(boolean ephemeralMapping) {
        //given
        IntegrationContextEntityImpl integrationContextEntity = buildIntegrationContextEntity();
        given(integrationContextService.findById(integrationContextEntity.getId())).willReturn(
            integrationContextEntity
        );
        given(ephemeralMappingResolver.isEphemeral(integrationContextEntity)).willReturn(ephemeralMapping);
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(true);
        when(executionQuery.list()).thenReturn(List.of());

        IntegrationContextImpl integrationContext = buildIntegrationContext();
        integrationContext.addOutBoundVariables(Map.of("restResult", "fromConnector"));
        // the connector error payload claims the opposite of the mapping
        integrationContext.setEphemeralVariables(!ephemeralMapping);
        IntegrationError integrationErrorEvent = new IntegrationErrorImpl(
            new IntegrationRequestImpl(integrationContext),
            new RuntimeException("connector failure")
        );

        //when
        handler.receive(integrationErrorEvent);

        //then
        verify(managementService).executeCommand(commandArgumentCaptor.capture());
        Command<?> aggregateCommand = commandArgumentCaptor
            .getValue()
            .getCommands()
            .stream()
            .filter(AggregateIntegrationErrorReceivedEventCmd.class::isInstance)
            .findFirst()
            .orElseThrow();
        aggregateCommand.execute(null);

        ArgumentCaptor<CloudIntegrationErrorReceivedEventImpl> eventCaptor = ArgumentCaptor.forClass(
            CloudIntegrationErrorReceivedEventImpl.class
        );
        verify(processEngineEventsAggregator).add(eventCaptor.capture());
        IntegrationContext auditedContext = eventCaptor.getValue().getEntity();
        assertThat(auditedContext.hasEphemeralVariables()).isEqualTo(ephemeralMapping);
        if (ephemeralMapping) {
            assertThat(auditedContext.getOutBoundVariables()).isEmpty();
        } else {
            assertThat(auditedContext.getOutBoundVariables()).containsExactlyEntriesOf(
                Map.of("restResult", "fromConnector")
            );
        }
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

    private static class AnotherCloudException extends RuntimeException {

        @Serial
        private static final long serialVersionUID = 1L;
    }
}
