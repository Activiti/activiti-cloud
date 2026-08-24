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
package org.activiti.cloud.common.messaging.config;

import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.FUNCTION_DESTINATION;
import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.TARGET_REGISTRATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.Channel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
class FunctionRouterRedeliveryTest {

    private static final String DESTINATION = "engine-events";

    @Mock
    private ObjectProvider<StreamBridge> streamBridgeProvider;

    @Mock
    private StreamBridge streamBridge;

    @Test
    void should_returnAllRegistrations_when_noTargetHeader() {
        var message = MessageBuilder.withPayload("payload").build();

        var result = FunctionRouterConfiguration.filterToTargetRegistrations(message, List.of("audit", "query"));

        assertThat(result).containsExactly("audit", "query");
    }

    @Test
    void should_restrictToPinnedRegistrations_when_targetHeaderPresent() {
        var message = MessageBuilder.withPayload("payload").setHeader(TARGET_REGISTRATIONS, "audit").build();

        var result = FunctionRouterConfiguration.filterToTargetRegistrations(message, List.of("audit", "query"));

        assertThat(result).containsExactly("audit");
    }

    @Test
    void should_returnEmpty_when_pinnedRegistrationsAbsentFromDestination() {
        var message = MessageBuilder.withPayload("payload").setHeader(TARGET_REGISTRATIONS, "audit").build();

        var result = FunctionRouterConfiguration.filterToTargetRegistrations(message, List.of("query"));

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_noRegistrationsForDestination() {
        var message = MessageBuilder.withPayload("payload").build();

        assertThat(FunctionRouterConfiguration.filterToTargetRegistrations(message, null)).isEmpty();
        assertThat(FunctionRouterConfiguration.filterToTargetRegistrations(message, List.of())).isEmpty();
    }

    @Test
    void should_republishOnlyFailedRegistrationsPinned_andStripTransportHeaders() {
        when(streamBridgeProvider.getObject()).thenReturn(streamBridge);
        when(streamBridge.send(eq(DESTINATION), org.mockito.ArgumentMatchers.<Message<?>>any())).thenReturn(true);

        var original = MessageBuilder.withPayload("payload")
            .setHeader(AmqpHeaders.CHANNEL, org.mockito.Mockito.mock(Channel.class))
            .setHeader(AmqpHeaders.DELIVERY_TAG, 7L)
            .setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, new Object())
            .build();

        var sent = FunctionRouterConfiguration.redeliverFailedRegistrations(
            streamBridgeProvider,
            DESTINATION,
            original,
            List.of("audit")
        );

        assertThat(sent).isTrue();

        var captor = ArgumentCaptor.<Message<?>>captor();
        verify(streamBridge).send(eq(DESTINATION), captor.capture());
        assertThat(captor.getValue().getHeaders())
            .containsEntry(TARGET_REGISTRATIONS, "audit")
            .containsEntry(FUNCTION_DESTINATION, DESTINATION)
            .doesNotContainKeys(
                AmqpHeaders.CHANNEL,
                AmqpHeaders.DELIVERY_TAG,
                IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK
            );
    }

    @Test
    void should_returnFalse_when_republishSendThrows() {
        when(streamBridgeProvider.getObject()).thenReturn(streamBridge);
        when(streamBridge.send(eq(DESTINATION), org.mockito.ArgumentMatchers.<Message<?>>any())).thenThrow(
            new RuntimeException("broker down")
        );

        var sent = FunctionRouterConfiguration.redeliverFailedRegistrations(
            streamBridgeProvider,
            DESTINATION,
            MessageBuilder.withPayload("payload").build(),
            List.of("audit")
        );

        assertThat(sent).isFalse();
    }
}
