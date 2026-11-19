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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationResultReceivedEventImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AggregateIntegrationResultReceivedEventCmdTest {

    private static final Map<String, Object> OUTBOUND_VARIABLES = Map.of(
        "outboundVar1",
        "value1",
        "outboundVar2",
        "value2"
    );

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RuntimeBundleProperties runtimeBundleProperties;

    @Mock
    private ProcessEngineEventsAggregator processEngineEventsAggregator;

    @Captor
    private ArgumentCaptor<CloudIntegrationResultReceivedEventImpl> messageCaptor;

    @Test
    void receiveShouldSendIntegrationAuditEventWhenIntegrationAuditEventsAreEnabled() {
        //given
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(true);

        //when
        buildCommand(buildIntegrationContext(null), false).execute(null);

        //then
        verify(processEngineEventsAggregator).add(messageCaptor.capture());
        CloudIntegrationResultReceivedEventImpl event = messageCaptor.getValue();
        assertThat(event.getEntity()).isNotNull();
    }

    @Test
    void should_clearInBoundVariables_when_integrationAuditEventIsSent() {
        //given
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(true);

        //when
        buildCommand(buildIntegrationContext(null), false).execute(null);

        //then
        assertThat(captureSentContext().getInBoundVariables()).isEmpty();
    }

    @Test
    void shouldNot_sendAuditEvent_when_integrationAuditEventsAreDisabled() {
        //given
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(false);

        //when
        buildCommand(buildIntegrationContext(null), true).execute(null);

        //then
        verifyNoInteractions(processEngineEventsAggregator);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = { true, false })
    void should_removeInboundAndOutBoundVariables_when_mappingIsEphemeral_regardlessOfConnectorFlag(
        Boolean connectorEphemeralFlag
    ) {
        //given
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(true);

        //when
        buildCommand(buildIntegrationContext(connectorEphemeralFlag), true).execute(null);

        //then
        IntegrationContext sanitizedContext = captureSentContext();
        assertThat(sanitizedContext.getInBoundVariables()).isEmpty();
        assertThat(sanitizedContext.getOutBoundVariables()).isEmpty();
        assertThat(sanitizedContext.hasEphemeralVariables()).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = { true, false })
    void should_retainOutBoundVariables_when_mappingIsNotEphemeral_regardlessOfConnectorFlag(
        Boolean connectorEphemeralFlag
    ) {
        //given
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(true);

        //when
        buildCommand(buildIntegrationContext(connectorEphemeralFlag), false).execute(null);

        //then
        IntegrationContext sanitizedContext = captureSentContext();
        assertThat(sanitizedContext.getInBoundVariables()).isEmpty();
        assertThat(sanitizedContext.getOutBoundVariables()).containsExactlyInAnyOrderEntriesOf(OUTBOUND_VARIABLES);
        assertThat(sanitizedContext.hasEphemeralVariables()).isFalse();
    }

    private AggregateIntegrationResultReceivedEventCmd buildCommand(
        IntegrationContext integrationContext,
        boolean ephemeralMapping
    ) {
        return new AggregateIntegrationResultReceivedEventCmd(
            integrationContext,
            runtimeBundleProperties,
            processEngineEventsAggregator,
            ephemeralMapping
        );
    }

    private IntegrationContextImpl buildIntegrationContext(Boolean connectorEphemeralFlag) {
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setEphemeralVariables(connectorEphemeralFlag);
        integrationContext.addInBoundVariables(Map.of("inboundVar1", "value1", "inboundVar2", "value2"));
        integrationContext.addOutBoundVariables(OUTBOUND_VARIABLES);
        return integrationContext;
    }

    private IntegrationContext captureSentContext() {
        verify(processEngineEventsAggregator).add(messageCaptor.capture());
        return messageCaptor.getValue().getEntity();
    }
}
