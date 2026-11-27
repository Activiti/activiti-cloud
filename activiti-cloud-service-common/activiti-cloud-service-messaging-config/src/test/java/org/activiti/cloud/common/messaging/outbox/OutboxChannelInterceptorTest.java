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

import static org.activiti.cloud.common.messaging.outbox.OutboxHeaders.OUTBOX_DESTINATION;
import static org.activiti.cloud.common.messaging.outbox.OutboxHeaders.OUTBOX_MESSAGE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.messaging.MessageHeaders.ERROR_CHANNEL;
import static org.springframework.messaging.MessageHeaders.REPLY_CHANNEL;

import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
class OutboxChannelInterceptorTest {

    private enum ExpectedIdBehavior {
        GENERATED_WITH_DESTINATION_PREFIX,
        PRESERVED,
        ABSENT,
    }

    @Mock
    private MessageChannel outboxChannel;

    @Mock
    private MessageChannel sourceChannel;

    @Mock
    private BiFunction<Message<?>, MessageChannel, String> destinationProvider;

    private OutboxChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new OutboxChannelInterceptor(outboxChannel, destinationProvider);
    }

    @Test
    void handleMessage_sendToOutboxChannel() {
        Message<String> message = MessageBuilder.withPayload("payload").build();

        interceptor.handleMessage(message);

        verify(outboxChannel).send(message);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("outboxMessageIdScenarios")
    void shouldHandleOutboxMessageIdCorrectly(
        String scenarioName,
        Message<String> inputMessage,
        String resolvedDestination,
        ExpectedIdBehavior expectedBehavior,
        String expectedExistingId
    ) {
        // Arrange
        lenient().when(destinationProvider.apply(any(), any())).thenReturn(resolvedDestination);

        // Act
        Message<?> result = interceptor.preSend(inputMessage, sourceChannel);

        // Assert
        assertThat(result).isNotNull();

        switch (expectedBehavior) {
            case GENERATED_WITH_DESTINATION_PREFIX -> assertThat(
                result.getHeaders().get(OUTBOX_MESSAGE_ID, String.class)
            )
                .isNotBlank()
                .startsWith(resolvedDestination + ":")
                .satisfies(id -> {
                    String uuidPart = id.substring(resolvedDestination.length() + 1);
                    assertThat(UUID.fromString(uuidPart)).isNotNull();
                });
            case PRESERVED -> assertThat(result.getHeaders().get(OUTBOX_MESSAGE_ID, String.class)).isEqualTo(
                expectedExistingId
            );
            case ABSENT -> assertThat(result.getHeaders()).doesNotContainKey(OUTBOX_MESSAGE_ID);
        }
    }

    private static Stream<Arguments> outboxMessageIdScenarios() {
        String existingId = "existing-topic:11111111-1111-1111-1111-111111111111";

        return Stream.of(
            Arguments.of(
                "Generates '<destination>:<uuid>' when destination is resolved and message ID is absent",
                MessageBuilder.withPayload("payload").build(),
                "target-topic",
                ExpectedIdBehavior.GENERATED_WITH_DESTINATION_PREFIX,
                null
            ),
            Arguments.of(
                "Overwrites existing message ID with '<destination>:<uuid>' when destination is resolved",
                MessageBuilder.withPayload("payload").setHeader(OUTBOX_MESSAGE_ID, existingId).build(),
                "target-topic",
                ExpectedIdBehavior.GENERATED_WITH_DESTINATION_PREFIX,
                existingId
            ),
            Arguments.of(
                "Does not add message ID when destination provider returns null",
                MessageBuilder.withPayload("payload").build(),
                null,
                ExpectedIdBehavior.ABSENT,
                null
            ),
            Arguments.of(
                "Does not add message ID when destination provider returns blank string",
                MessageBuilder.withPayload("payload").build(),
                "   ",
                ExpectedIdBehavior.ABSENT,
                null
            ),
            Arguments.of(
                "Preserves existing message ID when destination provider returns blank string",
                MessageBuilder.withPayload("payload").setHeader(OUTBOX_MESSAGE_ID, existingId).build(),
                "   ",
                ExpectedIdBehavior.PRESERVED,
                existingId
            ),
            Arguments.of(
                "Does not add message ID when OUTBOX_DESTINATION is already present",
                MessageBuilder.withPayload("payload").setHeader(OUTBOX_DESTINATION, "existing-topic").build(),
                "target-topic",
                ExpectedIdBehavior.ABSENT,
                null
            ),
            Arguments.of(
                "Preserves existing message ID when OUTBOX_DESTINATION is already present",
                MessageBuilder.withPayload("payload")
                    .setHeader(OUTBOX_DESTINATION, "existing-topic")
                    .setHeader(OUTBOX_MESSAGE_ID, existingId)
                    .build(),
                "target-topic",
                ExpectedIdBehavior.PRESERVED,
                existingId
            )
        );
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
            assertThat(result.getHeaders()).containsKey(OUTBOX_MESSAGE_ID);
        }
    }

    static Stream<Arguments> passthroughScenarios() {
        Message<String> withExistingHeader = MessageBuilder.withPayload("payload-1")
            .setHeader(OUTBOX_MESSAGE_ID, "existing.id")
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

        if (headersToRemove.length > 0) {
            interceptor.withHeadersToRemove(headersToRemove);
        }

        Message<?> result = interceptor.preSend(originalMessage, sourceChannel);

        assertThat(result).isNotNull();
        assertThat(result.getPayload()).isEqualTo(originalMessage.getPayload());
        assertThat(result.getHeaders())
            .containsEntry(OUTBOX_DESTINATION, resolvedDestination)
            .containsKey(OUTBOX_MESSAGE_ID)
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
