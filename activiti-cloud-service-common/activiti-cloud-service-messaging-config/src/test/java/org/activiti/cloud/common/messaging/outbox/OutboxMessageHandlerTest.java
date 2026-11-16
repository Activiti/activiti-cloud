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

package org.activiti.cloud.common.messaging.outbox;

import static org.activiti.cloud.common.messaging.outbox.OutboxChannelInterceptor.OUTBOX_DESTINATION;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
class OutboxMessageHandlerTest {

    private static final String PARTITION_KEY = "partition-0";

    @Mock
    private ObjectProvider<StreamBridge> streamBridgeProvider;

    @Mock
    private StreamBridge streamBridge;

    private OutboxMessageHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OutboxMessageHandler(PARTITION_KEY, streamBridgeProvider);
    }

    @Test
    void shouldSendMessageWhenOutboxDestinationHeaderIsPresent() {
        // Given
        String destination = "my-outbox-binding";
        Message<String> message = MessageBuilder.withPayload("test-payload")
            .setHeader(OUTBOX_DESTINATION, destination)
            .build();

        when(streamBridgeProvider.getObject()).thenReturn(streamBridge);
        when(streamBridge.send(destination, message)).thenReturn(true);

        // When / Then
        assertThatCode(() -> handler.handleMessage(message)).doesNotThrowAnyException();

        verify(streamBridge).send(destination, message);
    }

    @Test
    void shouldNotThrowExceptionWhenStreamBridgeDropsMessage() {
        // Given
        String destination = "my-outbox-binding";
        Message<String> message = MessageBuilder.withPayload("test-payload")
            .setHeader(OUTBOX_DESTINATION, destination)
            .build();

        when(streamBridgeProvider.getObject()).thenReturn(streamBridge);
        when(streamBridge.send(destination, message)).thenReturn(false);

        // When / Then
        assertThatCode(() -> handler.handleMessage(message)).doesNotThrowAnyException();

        verify(streamBridge).send(destination, message);
    }

    @Test
    void shouldThrowDestinationResolutionExceptionWhenOutboxDestinationHeaderIsMissing() {
        // Given
        Message<String> message = MessageBuilder.withPayload("test-payload").build();

        // When / Then
        assertThatThrownBy(() -> handler.handleMessage(message))
            .isInstanceOf(DestinationResolutionException.class)
            .hasMessage("Cannot resolve null message header outbox %s partition".formatted(PARTITION_KEY));

        verifyNoInteractions(streamBridgeProvider, streamBridge);
    }

    @Test
    void shouldThrowDestinationResolutionExceptionWhenOutboxDestinationHeaderIsEmpty() {
        // Given
        Message<String> message = MessageBuilder.withPayload("test-payload").setHeader(OUTBOX_DESTINATION, "").build();

        // When / Then
        assertThatThrownBy(() -> handler.handleMessage(message))
            .isInstanceOf(DestinationResolutionException.class)
            .hasMessage("Cannot resolve  message header outbox %s partition".formatted(PARTITION_KEY));

        verifyNoInteractions(streamBridgeProvider, streamBridge);
    }
}
