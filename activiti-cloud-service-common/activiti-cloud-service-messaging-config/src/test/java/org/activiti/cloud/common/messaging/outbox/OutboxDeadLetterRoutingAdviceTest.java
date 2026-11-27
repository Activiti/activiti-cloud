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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.Ordered;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.util.ExceptionTypeFilter;

class OutboxDeadLetterRoutingAdviceTest {

    private QueueChannel deadLetterChannel;
    private OutboxDeadLetterRoutingAdvice advice;

    @BeforeEach
    void setUp() {
        deadLetterChannel = new QueueChannel();
        var retryableFilter = new ExceptionTypeFilter(List.of(TimeoutException.class), null, true);
        advice = new OutboxDeadLetterRoutingAdvice(retryableFilter, deadLetterChannel);
    }

    private MessageHandler advise(MessageHandler delegate) {
        ProxyFactory proxyFactory = new ProxyFactory(delegate);
        proxyFactory.addAdvice(advice);
        return (MessageHandler) proxyFactory.getProxy();
    }

    @Test
    void shouldImplement_OrderedWithHighestPrecedence() {
        assertThat(advice).isInstanceOfSatisfying(Ordered.class, ordered ->
            assertThat(ordered.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE)
        );
    }

    @Test
    void successfulExecution_invokesHandlerWithoutSendingToDlq() {
        Message<String> input = MessageBuilder.withPayload("order-created").build();
        var handledMessages = new ArrayList<Message<?>>();

        MessageHandler handler = advise(handledMessages::add);
        handler.handleMessage(input);

        assertThat(handledMessages).containsExactly(input);
        assertThat(deadLetterChannel.getQueueSize()).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("retryableExceptions")
    void retryableExceptions_areRethrownAndNeverSentToDlq(String scenario, RuntimeException thrown) {
        Message<String> input = MessageBuilder.withPayload("order-created").build();
        MessageHandler handler = advise(message -> {
            throw thrown;
        });

        assertThatThrownBy(() -> handler.handleMessage(input)).isSameAs(thrown);

        assertThat(deadLetterChannel.getQueueSize()).isZero();
    }

    static Stream<Arguments> retryableExceptions() {
        var wrapped = new MessagingException("wrapper", new TimeoutException("broker timeout"));
        var deepNested = new RuntimeException(
            "outer",
            new IllegalStateException("middle", new TimeoutException("deep timeout"))
        );

        return Stream.of(
            Arguments.of("Wrapped retryable cause", wrapped),
            Arguments.of("Deeply nested retryable cause", deepNested)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("poisonPillExceptions")
    void nonRetryableExceptions_routeToDlqWithRootCauseMetadata(
        String scenario,
        RuntimeException thrown,
        Class<? extends Throwable> expectedErrorClass,
        @Nullable String expectedErrorMessage
    ) {
        Message<String> input = MessageBuilder.withPayload("bad-payload")
            .setHeader("correlation-id", "abc-123")
            .build();

        MessageHandler handler = advise(message -> {
            throw thrown;
        });

        handler.handleMessage(input);

        Message<?> dlqMessage = deadLetterChannel.receive(0);
        assertThat(dlqMessage).isNotNull();
        assertThat(dlqMessage.getPayload()).isEqualTo("bad-payload");
        assertThat(dlqMessage.getHeaders())
            .containsEntry("correlation-id", "abc-123")
            .containsEntry("x-error-class", expectedErrorClass.getName());

        if (expectedErrorMessage != null) {
            assertThat(dlqMessage.getHeaders()).containsEntry("x-error-message", expectedErrorMessage);
        } else {
            assertThat(dlqMessage.getHeaders()).doesNotContainKey("x-error-message");
        }
    }

    static Stream<Arguments> poisonPillExceptions() {
        return Stream.of(
            Arguments.of(
                "Direct non-retryable exception uses its own message",
                new IllegalArgumentException("Unexpected token at position 4"),
                IllegalArgumentException.class,
                "Unexpected token at position 4"
            ),
            Arguments.of(
                "Wrapped root cause with message uses root cause message",
                new MessagingException("handler failed", new NumberFormatException("For input string: \"NaN\"")),
                NumberFormatException.class,
                "For input string: \"NaN\""
            ),
            Arguments.of(
                "Wrapped root cause with null message falls back to outer exception message",
                new MessagingException("outer wrapper message", new NullPointerException()),
                NullPointerException.class,
                "outer wrapper message"
            ),
            Arguments.of(
                "Both root cause and outer exception having null messages omit x-error-message",
                new NullPointerException(),
                NullPointerException.class,
                null
            )
        );
    }
}
