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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.integration.channel.ChannelDispatcherAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.dispatcher.UnicastingDispatcherAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;

class OutboxChannelConfigurerTest {

    private final OutboxChannelConfigurer configurer = new OutboxChannelConfigurer();

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideChannelDispatchingScenarios")
    void shouldConfigureChannelRoutingAndFailoverBehavior(
        String scenario,
        DirectChannel channel,
        Message<String> message,
        boolean shouldOutboxHandlerFail,
        boolean expectException,
        List<String> expectedHandledBy
    ) {
        List<String> handledBy = new ArrayList<>();

        MessageHandler standardHandler = msg -> handledBy.add("standard");

        OutboxChannelInterceptor outboxInterceptor = new TestOutboxChannelInterceptor(msg -> {
            handledBy.add("outbox");
            if (shouldOutboxHandlerFail) {
                throw new RuntimeException("Outbox failure");
            }
        });

        configurer.accept(outboxInterceptor, channel);

        channel.subscribe(standardHandler);

        UnicastingDispatcher dispatcher = ChannelDispatcherAccessor.getDispatcher(channel);
        final var handlers = UnicastingDispatcherAccessor.getHandlers(dispatcher);

        assertThat(channel.getInterceptors()).contains(outboxInterceptor);
        assertThat(handlers).containsExactly(outboxInterceptor, standardHandler);

        if (expectException) {
            assertThatThrownBy(() -> channel.send(message)).isInstanceOf(MessageDeliveryException.class);
        } else {
            assertThat(channel.send(message)).isTrue();
        }

        assertThat(handledBy).containsExactlyElementsOf(expectedHandledBy);
    }

    private static Stream<Arguments> provideChannelDispatchingScenarios() {
        Message<String> outboxMessage = MessageBuilder.withPayload("payload")
            .setHeader(OUTBOX_DESTINATION, "target-destination")
            .build();

        Message<String> standardMessage = MessageBuilder.withPayload("payload").build();

        return Stream.of(
            Arguments.of(
                "routes message exclusively to outbox interceptor when OUTBOX_DESTINATION is present",
                new DirectChannel(),
                outboxMessage,
                false,
                false,
                List.of("outbox")
            ),
            Arguments.of(
                "routes message to pre-existing standard handler when OUTBOX_DESTINATION is absent",
                new DirectChannel(),
                standardMessage,
                false,
                false,
                List.of("standard")
            ),
            Arguments.of(
                "does not failover to standard handler when outbox interceptor fails (failover=false)",
                new DirectChannel(),
                outboxMessage,
                true,
                true,
                List.of("outbox")
            )
        );
    }

    /**
     * Minimal test subclass of OutboxChannelInterceptor that delegates MessageHandler.handleMessage
     * without requiring mocks or infrastructure dependencies.
     */
    private static class TestOutboxChannelInterceptor extends OutboxChannelInterceptor {

        private final MessageHandler delegate;

        TestOutboxChannelInterceptor(MessageHandler delegate) {
            super(new DirectChannel(), (m, c) -> null);
            this.delegate = delegate;
        }

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel messageChannel) {
            return message;
        }

        @Override
        public void handleMessage(Message<?> message) {
            delegate.handleMessage(message);
        }
    }
}
