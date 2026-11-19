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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.Map;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationErrorImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationErrorReceivedEventImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.engine.ManagementService;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class ServiceTaskIntegrationCompletionHandlerTest {

    private static final Map<String, Object> CONNECTOR_OUTBOUND_VARIABLES = Map.of("restResult", "fromConnector");

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RuntimeBundleProperties runtimeBundleProperties;

    @Mock
    private ManagementService managementService;

    @Mock
    private ProcessEngineEventsAggregator processEngineEventsAggregator;

    @Mock
    private EphemeralMappingResolver ephemeralMappingResolver;

    @Test
    void handlePropagationFailure_shouldExecuteCompositeCommand() {
        ServiceTaskIntegrationCompletionHandler handler = buildHandler();

        IntegrationErrorImpl error = mock(IntegrationErrorImpl.class);
        IntegrationContextEntity ctxEntity = mock(IntegrationContextEntity.class);

        handler.handlePropagationFailure(error, ctxEntity);

        verify(ephemeralMappingResolver).isEphemeral(ctxEntity);

        ArgumentCaptor<Command<?>> cmdCaptor = ArgumentCaptor.forClass(Command.class);
        verify(managementService, times(1)).executeCommand(cmdCaptor.capture());
        Command<?> passed = cmdCaptor.getValue();
        assertNotNull(passed, "Expected a Command to be passed to ManagementService");
        // Best-effort assertion on composite nature (class name should contain CompositeCommand)
        assertTrue(
            passed.getClass().getSimpleName().toLowerCase().contains("composite"),
            "Expected a CompositeCommand but got: " + passed.getClass().getName()
        );
    }

    @Test
    void handlePropagationFailure_should_removeOutboundVariablesFromAuditEvent_when_mappingIsEphemeral() {
        //given
        IntegrationContextEntity ctxEntity = givenIntegrationContextWithMapping(true);
        // the connector reply claims the opposite of the mapping
        IntegrationErrorImpl error = buildIntegrationError(false);

        //when
        buildHandler().handlePropagationFailure(error, ctxEntity);

        //then
        IntegrationContext auditedContext = executeAuditCommandAndCaptureContext();
        assertThat(auditedContext.hasEphemeralVariables()).isTrue();
        assertThat(auditedContext.getOutBoundVariables()).isEmpty();
    }

    @Test
    void handlePropagationFailure_should_keepOutboundVariablesInAuditEvent_when_mappingIsNotEphemeral() {
        //given
        IntegrationContextEntity ctxEntity = givenIntegrationContextWithMapping(false);
        // the connector reply claims the opposite of the mapping
        IntegrationErrorImpl error = buildIntegrationError(true);

        //when
        buildHandler().handlePropagationFailure(error, ctxEntity);

        //then
        IntegrationContext auditedContext = executeAuditCommandAndCaptureContext();
        assertThat(auditedContext.hasEphemeralVariables()).isFalse();
        assertThat(auditedContext.getOutBoundVariables()).containsExactlyEntriesOf(CONNECTOR_OUTBOUND_VARIABLES);
    }

    private ServiceTaskIntegrationCompletionHandler buildHandler() {
        return new ServiceTaskIntegrationCompletionHandler(
            managementService,
            new AggregateIntegrationEventCmdFactory(
                runtimeBundleProperties,
                processEngineEventsAggregator,
                ephemeralMappingResolver
            )
        );
    }

    private IntegrationContextEntity givenIntegrationContextWithMapping(boolean ephemeralMapping) {
        IntegrationContextEntity ctxEntity = mock(IntegrationContextEntity.class);
        given(ephemeralMappingResolver.isEphemeral(ctxEntity)).willReturn(ephemeralMapping);
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(true);
        return ctxEntity;
    }

    private IntegrationErrorImpl buildIntegrationError(boolean connectorEphemeralFlag) {
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.addOutBoundVariables(CONNECTOR_OUTBOUND_VARIABLES);
        integrationContext.setEphemeralVariables(connectorEphemeralFlag);
        return new IntegrationErrorImpl(
            new IntegrationRequestImpl(integrationContext),
            new RuntimeException("propagation failure")
        );
    }

    private IntegrationContext executeAuditCommandAndCaptureContext() {
        ArgumentCaptor<CompositeCommand> commandCaptor = ArgumentCaptor.forClass(CompositeCommand.class);
        verify(managementService).executeCommand(commandCaptor.capture());
        commandCaptor
            .getValue()
            .getCommands()
            .stream()
            .filter(AggregateIntegrationErrorReceivedEventCmd.class::isInstance)
            .findFirst()
            .orElseThrow()
            .execute(null);

        ArgumentCaptor<CloudIntegrationErrorReceivedEventImpl> eventCaptor = ArgumentCaptor.forClass(
            CloudIntegrationErrorReceivedEventImpl.class
        );
        verify(processEngineEventsAggregator).add(eventCaptor.capture());
        return eventCaptor.getValue().getEntity();
    }

    @Test
    void handlePropagationFailure_shouldBeTransactionalRequiresNew() throws Exception {
        Method m = ServiceTaskIntegrationCompletionHandler.class.getMethod(
            "handlePropagationFailure",
            IntegrationErrorImpl.class,
            IntegrationContextEntity.class
        );
        Transactional tx = m.getAnnotation(Transactional.class);
        assertNotNull(tx, "Transactional annotation missing");
        assertEquals(Propagation.REQUIRES_NEW, tx.propagation(), "Propagation should be REQUIRES_NEW");
    }
}
