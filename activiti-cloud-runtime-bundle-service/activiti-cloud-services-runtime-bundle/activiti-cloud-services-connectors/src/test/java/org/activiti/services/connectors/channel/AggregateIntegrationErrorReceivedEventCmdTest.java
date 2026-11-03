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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.events.CloudIntegrationErrorReceivedEvent;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AggregateIntegrationErrorReceivedEventCmdTest {

    private static final Map<String, Object> OUTBOUND_VARIABLES = Map.of(
        "outboundKey",
        "outboundValue",
        "outboundKey2",
        "outboundValue2"
    );

    private final RuntimeBundleProperties runtimeBundleProperties = new RuntimeBundleProperties();

    @Mock
    private ProcessEngineEventsAggregator processEngineEventsAggregator;

    @Captor
    private ArgumentCaptor<CloudRuntimeEvent<?, ?>> cloudRuntimeEventArgumentCaptor;

    @Test
    void should_aggregateIntegrationErrorReceivedEvent_when_auditEventAreEnabled() {
        //given
        final IntegrationError integrationError = mock(IntegrationError.class);
        final IntegrationContext integrationContext = mock(IntegrationContext.class);
        when(integrationError.getIntegrationContext()).thenReturn(integrationContext);
        when(integrationError.getErrorCode()).thenReturn("myErrorCode");
        when(integrationError.getErrorMessage()).thenReturn("my error message");
        when(integrationError.getErrorClassName()).thenReturn("className");
        final List<StackTraceElement> stackTraceElements = Collections.singletonList(mock(StackTraceElement.class));
        when(integrationError.getStackTraceElements()).thenReturn(stackTraceElements);

        //when
        buildCommand(integrationError, false).execute(mock(CommandContext.class));

        //then
        verify(processEngineEventsAggregator).add(cloudRuntimeEventArgumentCaptor.capture());
        final CloudRuntimeEvent<?, ?> event = cloudRuntimeEventArgumentCaptor.getValue();
        assertThat(event).isInstanceOf(CloudIntegrationErrorReceivedEvent.class);
        CloudIntegrationErrorReceivedEvent errorReceivedEvent = (CloudIntegrationErrorReceivedEvent) event;
        assertThat(errorReceivedEvent.getErrorCode()).isEqualTo("myErrorCode");
        assertThat(errorReceivedEvent.getErrorMessage()).isEqualTo("my error message");
        assertThat(errorReceivedEvent.getErrorClassName()).isEqualTo("className");
        assertThat(errorReceivedEvent.getStackTraceElements()).isEqualTo(stackTraceElements);
    }

    @Test
    void should_clearInBoundVariables_when_integrationAuditEventIsSent() {
        //given
        IntegrationError integrationError = buildIntegrationError(null);

        //when
        buildCommand(integrationError, false).execute(mock(CommandContext.class));

        //then
        assertThat(captureSentContext().getInBoundVariables()).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = { true, false })
    void should_notRetainInboundAndOutboundVariables_when_mappingIsEphemeral_regardlessOfConnectorFlag(
        Boolean connectorEphemeralFlag
    ) {
        //given
        IntegrationError integrationError = buildIntegrationError(connectorEphemeralFlag);

        //when
        buildCommand(integrationError, true).execute(mock(CommandContext.class));

        //then
        IntegrationContext sanitizedContext = captureSentContext();
        assertThat(sanitizedContext.getInBoundVariables()).isEmpty();
        assertThat(sanitizedContext.getOutBoundVariables()).isEmpty();
        assertThat(sanitizedContext.hasEphemeralVariables()).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = { true, false })
    void should_retainOutboundVariables_when_mappingIsNotEphemeral_regardlessOfConnectorFlag(
        Boolean connectorEphemeralFlag
    ) {
        //given
        IntegrationError integrationError = buildIntegrationError(connectorEphemeralFlag);

        //when
        buildCommand(integrationError, false).execute(mock(CommandContext.class));

        //then
        IntegrationContext sanitizedContext = captureSentContext();
        assertThat(sanitizedContext.getInBoundVariables()).isEmpty();
        assertThat(sanitizedContext.getOutBoundVariables()).containsExactlyInAnyOrderEntriesOf(OUTBOUND_VARIABLES);
        assertThat(sanitizedContext.hasEphemeralVariables()).isFalse();
    }

    private AggregateIntegrationErrorReceivedEventCmd buildCommand(
        IntegrationError integrationError,
        boolean ephemeralMapping
    ) {
        return new AggregateIntegrationErrorReceivedEventCmd(
            integrationError,
            runtimeBundleProperties,
            processEngineEventsAggregator,
            ephemeralMapping
        );
    }

    private IntegrationError buildIntegrationError(Boolean connectorEphemeralFlag) {
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setEphemeralVariables(connectorEphemeralFlag);
        integrationContext.addInBoundVariables(Map.of("inboundKey", "inboundValue", "inboundKey2", "inboundValue2"));
        integrationContext.addOutBoundVariables(OUTBOUND_VARIABLES);

        IntegrationError integrationError = mock(IntegrationError.class);
        when(integrationError.getIntegrationContext()).thenReturn(integrationContext);
        return integrationError;
    }

    private IntegrationContext captureSentContext() {
        verify(processEngineEventsAggregator).add(cloudRuntimeEventArgumentCaptor.capture());
        return ((CloudIntegrationErrorReceivedEvent) cloudRuntimeEventArgumentCaptor.getValue()).getEntity();
    }
}
