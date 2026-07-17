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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AggregateIntegrationErrorReceivedEventCmdTest {

    private final RuntimeBundleProperties runtimeBundleProperties = new RuntimeBundleProperties();

    @Mock
    private ProcessEngineEventsAggregator processEngineEventsAggregator;

    @Captor
    private ArgumentCaptor<CloudRuntimeEvent<?, ?>> cloudRuntimeEventArgumentCaptor;

    @Test
    public void should_aggregateIntegrationErrorReceivedEvent_when_auditEventAreEnabled() {
        //given
        final IntegrationError integrationError = mock(IntegrationError.class);
        final IntegrationContext integrationContext = mock(IntegrationContext.class);
        when(integrationError.getIntegrationContext()).thenReturn(integrationContext);
        when(integrationError.getErrorCode()).thenReturn("myErrorCode");
        when(integrationError.getErrorMessage()).thenReturn("my error message");
        when(integrationError.getErrorClassName()).thenReturn("className");
        final List<StackTraceElement> stackTraceElements = Collections.singletonList(mock(StackTraceElement.class));
        when(integrationError.getStackTraceElements()).thenReturn(stackTraceElements);
        final AggregateIntegrationErrorReceivedEventCmd command = new AggregateIntegrationErrorReceivedEventCmd(
            integrationError,
            runtimeBundleProperties,
            processEngineEventsAggregator
        );

        //when
        command.execute(mock(CommandContext.class));

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
    void should_notRetainInboundAndOutboundVariables_when_integrationContextHasEphemeralVariables() {
        //given
        final IntegrationError integrationError = mock(IntegrationError.class);

        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setEphemeralVariables(true);
        integrationContext.addInBoundVariables(Map.of("inboundKey", "inboundValue", "inboundKey2", "inboundValue2"));
        integrationContext.addOutBoundVariables(
            Map.of("outboundKey", "outboundValue", "outboundKey2", "outboundValue2")
        );

        when(integrationError.getIntegrationContext()).thenReturn(integrationContext);

        final AggregateIntegrationErrorReceivedEventCmd command = new AggregateIntegrationErrorReceivedEventCmd(
            integrationError,
            runtimeBundleProperties,
            processEngineEventsAggregator
        );

        //when
        command.execute(mock(CommandContext.class));

        verify(processEngineEventsAggregator).add(cloudRuntimeEventArgumentCaptor.capture());
        final CloudRuntimeEvent<?, ?> event = cloudRuntimeEventArgumentCaptor.getValue();
        IntegrationContext sanitizedContext = ((CloudIntegrationErrorReceivedEvent) event).getEntity();
        assertThat(sanitizedContext.getInBoundVariables()).isEmpty();
        assertThat(sanitizedContext.getOutBoundVariables()).isEmpty();
    }

    @Test
    void should_retainInboundAndOutboundVariables_when_integrationContextHasEphemeralVariables() {
        //given
        final IntegrationError integrationError = mock(IntegrationError.class);

        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setEphemeralVariables(false);
        integrationContext.addInBoundVariables(Map.of("inboundKey", "inboundValue", "inboundKey2", "inboundValue2"));
        integrationContext.addOutBoundVariables(
            Map.of("outboundKey", "outboundValue", "outboundKey2", "outboundValue2")
        );

        when(integrationError.getIntegrationContext()).thenReturn(integrationContext);

        final AggregateIntegrationErrorReceivedEventCmd command = new AggregateIntegrationErrorReceivedEventCmd(
            integrationError,
            runtimeBundleProperties,
            processEngineEventsAggregator
        );

        //when
        command.execute(mock(CommandContext.class));

        verify(processEngineEventsAggregator).add(cloudRuntimeEventArgumentCaptor.capture());
        final CloudRuntimeEvent<?, ?> event = cloudRuntimeEventArgumentCaptor.getValue();
        IntegrationContext sanitizedContext = ((CloudIntegrationErrorReceivedEvent) event).getEntity();
        assertThat(sanitizedContext.getInBoundVariables()).containsExactlyInAnyOrderEntriesOf(
            Map.of("inboundKey", "inboundValue", "inboundKey2", "inboundValue2")
        );
        assertThat(sanitizedContext.getOutBoundVariables()).containsExactlyInAnyOrderEntriesOf(
            Map.of("outboundKey", "outboundValue", "outboundKey2", "outboundValue2")
        );
    }
}
