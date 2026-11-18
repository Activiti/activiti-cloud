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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.messaging.MessageHeaders.ERROR_CHANNEL;
import static org.springframework.messaging.MessageHeaders.REPLY_CHANNEL;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
    private MessageChannel sourceChannel;

    @Mock
    private BiFunction<Message<?>, MessageChannel, String> destinationProvider;

    @Captor
    private ArgumentCaptor<Message<?>> messageCaptor;

    private OutboxChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new OutboxChannelInterceptor(outboxChannel, destinationProvider);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("passthroughScenarios")
    void preSend_whenPassthroughConditionMet_returnsOriginalMessage(
        String scenario,
        Message<String> originalMessage,
        String providerReturnValue,
        boolean expectProviderInvocation
    ) {
        if (expectProviderInvocation) {
            when(destinationProvider.apply(originalMessage, sourceChannel)).thenReturn(providerReturnValue);
        }

        Message<?> result = interceptor.preSend(originalMessage, sourceChannel);

        assertThat(result).isNotNull().extracting(Message::getPayload).isSameAs(originalMessage.getPayload());
        assertThat(result.getHeaders()).doesNotContainKey(OUTBOX_DESTINATION);

        verify(outboxChannel, never()).send(any());

        if (expectProviderInvocation) {
            verify(destinationProvider).apply(originalMessage, sourceChannel);
        } else {
            verify(destinationProvider, never()).apply(any(), any());
        }
    }

    static Stream<Arguments> passthroughScenarios() {
        Message<String> withExistingHeader = MessageBuilder.withPayload("payload-1")
            .setHeader(OUTBOX_DESTINATION, "existing.destination")
            .build();

        Message<String> withNullHeaderValue = MessageBuilder.withPayload("payload-2")
            .setHeader(OUTBOX_DESTINATION, null)
            .build();

        Message<String> withoutHeader = MessageBuilder.withPayload("payload-3")
            .setHeader("customHeader", "value")
            .build();

        return Stream.of(
            Arguments.of("Header already present with value", withExistingHeader, "ignored", false),
            Arguments.of("Header already present with null value", withNullHeaderValue, null, true),
            Arguments.of("Header absent and destination provider returns null", withoutHeader, null, true)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rerouteScenarios")
    void preSend_whenDestinationResolved_reroutesToOutboxAndStripsRoutingHeaders(
        String scenario,
        Message<String> originalMessage,
        String resolvedDestination,
        String[] headersToRemove,
        Map<String, Object> expectedRetainedHeaders
    ) {
        when(destinationProvider.apply(originalMessage, sourceChannel)).thenReturn(resolvedDestination);
        when(outboxChannel.send(any())).thenReturn(true);

        if (headersToRemove.length > 0) {
            interceptor.withHeadersToRemove(headersToRemove);
        }

        Message<?> result = interceptor.preSend(originalMessage, sourceChannel);

        assertThat(result).isNull();

        verify(outboxChannel).send(messageCaptor.capture());
        Message<?> sentMessage = messageCaptor.getValue();

        assertThat(sentMessage.getPayload()).isEqualTo(originalMessage.getPayload());
        assertThat(sentMessage.getHeaders())
            .containsEntry(OUTBOX_DESTINATION, resolvedDestination)
            .containsAllEntriesOf(expectedRetainedHeaders)
            .doesNotContainKeys(headersToRemove);
    }

    static Stream<Arguments> rerouteScenarios() {
        MessageChannel dummyReply = mock(MessageChannel.class);
        MessageChannel dummyError = mock(MessageChannel.class);

        Message<String> withReplyAndErrorChannels = MessageBuilder.withPayload("order-created")
            .setHeader("tenantId", "tenant-123")
            .setHeader(REPLY_CHANNEL, dummyReply)
            .setHeader(ERROR_CHANNEL, dummyError)
            .build();

        Message<String> withStringChannelNames = MessageBuilder.withPayload("payment-processed")
            .setHeader("correlationId", "corr-456")
            .setHeader(REPLY_CHANNEL, "replyChannelName")
            .setHeader(ERROR_CHANNEL, "errorChannelName")
            .build();

        Message<String> minimalHeaders = MessageBuilder.withPayload("user-updated").build();

        String[] emptyHeadersToRemove = {};
        String[] headersToRemove = { REPLY_CHANNEL, ERROR_CHANNEL };

        return Stream.of(
            Arguments.of(
                "Strips MessageChannel instances for reply/error headers",
                withReplyAndErrorChannels,
                "orders.outbox",
                headersToRemove,
                Map.of("tenantId", "tenant-123")
            ),
            Arguments.of(
                "Strips String channel names for reply/error headers",
                withStringChannelNames,
                "payments.outbox",
                headersToRemove,
                Map.of("correlationId", "corr-456")
            ),
            Arguments.of(
                "Works when no reply/error or custom headers exist",
                minimalHeaders,
                "users.outbox",
                emptyHeadersToRemove,
                Map.of()
            )
        );
    }
}
