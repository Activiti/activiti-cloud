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
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AggregateIntegrationResultReceivedEventCmdTest {

    @InjectMocks
    private AggregateIntegrationResultReceivedEventCmd command;

    @Mock
    private IntegrationContext integrationContext;

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
        command.execute(null);

        //then
        verify(processEngineEventsAggregator).add(messageCaptor.capture());
        CloudIntegrationResultReceivedEventImpl event = messageCaptor.getValue();
        assertThat(event.getEntity()).isNotNull();
    }

    @Test
    void should_clearInBoundVariables_when_integrationAuditEventIsSent() {
        //given
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(true);

        IntegrationContextImpl testIntegrationContext = new IntegrationContextImpl();
        testIntegrationContext.addInBoundVariables(Map.of("inboundVar1", "value1", "inboundVar2", "value2"));
        testIntegrationContext.addOutBoundVariables(Map.of("outboundVar1", "value1", "outboundVar2", "value2"));

        AggregateIntegrationResultReceivedEventCmd aggregateIntegrationResultReceivedEventCmd =
            new AggregateIntegrationResultReceivedEventCmd(
                testIntegrationContext,
                runtimeBundleProperties,
                processEngineEventsAggregator
            );

        //when
        aggregateIntegrationResultReceivedEventCmd.execute(null);

        //then
        verify(processEngineEventsAggregator).add(messageCaptor.capture());
        CloudIntegrationResultReceivedEventImpl event = messageCaptor.getValue();
        IntegrationContext sanitizedContext = event.getEntity();
        assertThat(sanitizedContext.getInBoundVariables()).isEmpty();
    }

    @Test
    void shouldNot_sentAuditEvent_when_integrationAuditEventsAreDisabled() {
        //given
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(false);

        //when
        command.execute(null);

        //then
        verifyNoInteractions(processEngineEventsAggregator);
    }

    @Test
    void should_removeInboundOutBoundVariables_when_integrationContextHasEphemeralVariables() {
        //given
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(true);

        IntegrationContextImpl testIntegrationContext = new IntegrationContextImpl();
        testIntegrationContext.setEphemeralVariables(true);
        testIntegrationContext.addInBoundVariables(Map.of("inboundVar1", "value1", "inboundVar2", "value2"));
        testIntegrationContext.addOutBoundVariables(Map.of("outboundVar1", "value1", "outboundVar2", "value2"));

        AggregateIntegrationResultReceivedEventCmd aggregateIntegrationResultReceivedEventCmd =
            new AggregateIntegrationResultReceivedEventCmd(
                testIntegrationContext,
                runtimeBundleProperties,
                processEngineEventsAggregator
            );

        //when
        aggregateIntegrationResultReceivedEventCmd.execute(null);

        //then
        verify(processEngineEventsAggregator).add(messageCaptor.capture());
        CloudIntegrationResultReceivedEventImpl event = messageCaptor.getValue();
        IntegrationContext sanitizedContext = event.getEntity();
        assertThat(sanitizedContext.getInBoundVariables()).isEmpty();
        assertThat(sanitizedContext.getOutBoundVariables()).isEmpty();
    }

    @Test
    void should_retainOutBoundVariables_when_integrationContextHasNoEphemeralVariables() {
        //given
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(true);

        IntegrationContextImpl testIntegrationContext = new IntegrationContextImpl();
        testIntegrationContext.setEphemeralVariables(false);
        testIntegrationContext.addInBoundVariables(Map.of("inboundVar1", "value1", "inboundVar2", "value2"));
        testIntegrationContext.addOutBoundVariables(Map.of("outboundVar1", "value1", "outboundVar2", "value2"));

        AggregateIntegrationResultReceivedEventCmd aggregateIntegrationResultReceivedEventCmd =
            new AggregateIntegrationResultReceivedEventCmd(
                testIntegrationContext,
                runtimeBundleProperties,
                processEngineEventsAggregator
            );

        //when
        aggregateIntegrationResultReceivedEventCmd.execute(null);

        //then
        verify(processEngineEventsAggregator).add(messageCaptor.capture());
        CloudIntegrationResultReceivedEventImpl event = messageCaptor.getValue();
        IntegrationContext sanitizedContext = event.getEntity();
        assertThat(sanitizedContext.getInBoundVariables()).isEmpty();
        assertThat(sanitizedContext.getOutBoundVariables()).containsExactlyInAnyOrderEntriesOf(
            Map.of("outboundVar1", "value1", "outboundVar2", "value2")
        );
    }
}
