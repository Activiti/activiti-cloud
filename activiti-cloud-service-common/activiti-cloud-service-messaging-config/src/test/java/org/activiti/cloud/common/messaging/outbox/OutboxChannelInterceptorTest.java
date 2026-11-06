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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.function.BiFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
class OutboxChannelInterceptorTest {

    @Mock
    private MessageChannel outboxChannel;

    @Mock
    private MessageChannel originalChannel;

    @Mock
    private BiFunction<Message<?>, MessageChannel, String> destinationProvider;

    @Captor
    private ArgumentCaptor<Message<?>> messageCaptor;

    private OutboxChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new OutboxChannelInterceptor(outboxChannel, destinationProvider);
    }

    @Test
    void preSend_ShouldReturnSameMessage_WhenHeaderAlreadyExists() {
        // Arrange
        Message<String> inputMessage = MessageBuilder.withPayload("test-payload")
            .setHeader(OutboxChannelInterceptor.OUTBOX_DESTINATION, "existing-destination")
            .build();

        // Act
        Message<?> result = interceptor.preSend(inputMessage, originalChannel);

        // Assert
        assertThat(result).isSameAs(inputMessage);
        verifyNoInteractions(destinationProvider, outboxChannel);
    }

    @Test
    void preSend_ShouldReturnNull_WhenHeaderIsMissingAndDestinationIsProvidedAndSentSuccessfully() {
        // Arrange
        Message<String> inputMessage = MessageBuilder.withPayload("test-payload").build();
        String targetDestination = "resolved-destination";

        when(destinationProvider.apply(inputMessage, originalChannel)).thenReturn(targetDestination);
        when(outboxChannel.send(any(Message.class))).thenReturn(true);

        // Act
        Message<?> result = interceptor.preSend(inputMessage, originalChannel);

        // Assert
        assertThat(result).isNull();

        verify(outboxChannel).send(messageCaptor.capture());
        Message<?> sentMessage = messageCaptor.getValue();

        assertThat(sentMessage.getPayload()).isEqualTo("test-payload");
        assertThat(sentMessage.getHeaders()).containsEntry(
            OutboxChannelInterceptor.OUTBOX_DESTINATION,
            targetDestination
        );
    }

    @Test
    void preSend_ShouldReturnSameMessage_WhenDestinationProviderReturnsNull() {
        // Arrange
        Message<String> inputMessage = MessageBuilder.withPayload("test-payload").build();

        when(destinationProvider.apply(inputMessage, originalChannel)).thenReturn(null);

        // Act
        Message<?> result = interceptor.preSend(inputMessage, originalChannel);

        // Assert
        assertThat(result).isSameAs(inputMessage);
        verifyNoInteractions(outboxChannel);
    }

    @Test
    void preSend_ShouldReturnNull_WhenOutboxChannelSendMessage() {
        // Arrange
        Message<String> inputMessage = MessageBuilder.withPayload("test-payload").build();
        String targetDestination = "resolved-destination";

        when(destinationProvider.apply(inputMessage, originalChannel)).thenReturn(targetDestination);
        when(outboxChannel.send(any(Message.class))).thenReturn(true);

        // Act
        Message<?> result = interceptor.preSend(inputMessage, originalChannel);

        // Assert
        assertThat(result).isNull();
        verify(outboxChannel).send(any(Message.class));
    }
}
