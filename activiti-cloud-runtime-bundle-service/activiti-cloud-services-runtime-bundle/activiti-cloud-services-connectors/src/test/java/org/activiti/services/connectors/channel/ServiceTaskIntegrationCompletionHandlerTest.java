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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class ServiceTaskIntegrationCompletionHandlerTest {

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
        ServiceTaskIntegrationCompletionHandler handler = new ServiceTaskIntegrationCompletionHandler(
            runtimeBundleProperties,
            managementService,
            processEngineEventsAggregator,
            ephemeralMappingResolver
        );

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

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void handlePropagationFailure_should_useEphemeralFlagFromMapping_andIgnoreConnectorFlag(boolean ephemeralMapping) {
        //given
        ServiceTaskIntegrationCompletionHandler handler = new ServiceTaskIntegrationCompletionHandler(
            runtimeBundleProperties,
            managementService,
            processEngineEventsAggregator,
            ephemeralMappingResolver
        );

        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.addOutBoundVariables(Map.of("restResult", "fromConnector"));
        // the connector reply claims the opposite of the mapping
        integrationContext.setEphemeralVariables(!ephemeralMapping);
        IntegrationErrorImpl error = new IntegrationErrorImpl(
            new IntegrationRequestImpl(integrationContext),
            new RuntimeException("propagation failure")
        );
        IntegrationContextEntity ctxEntity = mock(IntegrationContextEntity.class);

        given(ephemeralMappingResolver.isEphemeral(ctxEntity)).willReturn(ephemeralMapping);
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(true);

        //when
        handler.handlePropagationFailure(error, ctxEntity);

        //then
        ArgumentCaptor<CompositeCommand> commandCaptor = ArgumentCaptor.forClass(CompositeCommand.class);
        verify(managementService).executeCommand(commandCaptor.capture());
        Command<?> aggregateCommand = commandCaptor
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
