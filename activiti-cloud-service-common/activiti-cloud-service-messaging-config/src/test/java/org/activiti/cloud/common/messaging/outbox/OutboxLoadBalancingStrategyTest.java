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
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;

class OutboxLoadBalancingStrategyTest {

    private static final MessageHandler OUTBOX_HANDLER = message -> {};
    private static final MessageHandler STANDARD_HANDLER_1 = message -> {};
    private static final MessageHandler STANDARD_HANDLER_2 = message -> {};

    private final OutboxLoadBalancingStrategy strategy = new OutboxLoadBalancingStrategy(OUTBOX_HANDLER);

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideHandlerRoutingScenarios")
    void shouldResolveExpectedHandlers(
        String scenario,
        Message<?> message,
        List<MessageHandler> availableHandlers,
        List<MessageHandler> expectedHandlers
    ) {
        var result = strategy.getHandlerIterator(message, availableHandlers);

        assertThat(result).toIterable().containsExactlyElementsOf(expectedHandlers);
    }

    private static Stream<Arguments> provideHandlerRoutingScenarios() {
        Message<String> messageWithOutboxHeader = MessageBuilder.withPayload("payload")
            .setHeader(OUTBOX_DESTINATION, "target-destination")
            .build();

        Message<String> messageWithoutOutboxHeader = MessageBuilder.withPayload("payload")
            .setHeader("other-header", "value")
            .build();

        return Stream.of(
            Arguments.of(
                "returns only outbox handler when OUTBOX_DESTINATION is set among multiple handlers",
                messageWithOutboxHeader,
                List.of(STANDARD_HANDLER_1, OUTBOX_HANDLER, STANDARD_HANDLER_2),
                List.of(OUTBOX_HANDLER)
            ),
            Arguments.of(
                "returns outbox handler when OUTBOX_DESTINATION is set and outbox handler is not in collection",
                messageWithOutboxHeader,
                List.of(STANDARD_HANDLER_1, STANDARD_HANDLER_2),
                List.of(OUTBOX_HANDLER)
            ),
            Arguments.of(
                "returns outbox handler when OUTBOX_DESTINATION is set and handlers collection is empty",
                messageWithOutboxHeader,
                List.of(),
                List.of(OUTBOX_HANDLER)
            ),
            Arguments.of(
                "excludes outbox handler and returns remaining handlers in order when OUTBOX_DESTINATION is absent",
                messageWithoutOutboxHeader,
                List.of(STANDARD_HANDLER_1, OUTBOX_HANDLER, STANDARD_HANDLER_2),
                List.of(STANDARD_HANDLER_1, STANDARD_HANDLER_2)
            ),
            Arguments.of(
                "returns empty iterator when only outbox handler is present and OUTBOX_DESTINATION is absent",
                messageWithoutOutboxHeader,
                List.of(OUTBOX_HANDLER),
                List.of()
            ),
            Arguments.of(
                "returns empty iterator when handlers collection is empty and OUTBOX_DESTINATION is absent",
                messageWithoutOutboxHeader,
                List.of(),
                List.of()
            )
        );
    }
}
