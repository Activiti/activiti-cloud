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
package org.activiti.cloud.services.messages.events.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.MessageBuilder;

class MessageSenderTransactionSynchronizationTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideBeforeCommitScenarios")
    void shouldHandleBeforeCommitSendingScenarios(
        String scenario,
        boolean readOnly,
        Supplier<Boolean> channelBehavior,
        Class<? extends Throwable> expectedExceptionType
    ) {
        Message<String> message = MessageBuilder.withPayload("bpmn-event-payload").build();
        List<Message<?>> sentMessages = new ArrayList<>();

        MessageChannel channel = (msg, timeout) -> {
            sentMessages.add(msg);
            return channelBehavior.get();
        };

        var synchronization = new MessageSenderTransactionSynchronization(message, channel);

        if (expectedExceptionType == null) {
            assertThatCode(() -> synchronization.beforeCommit(readOnly)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> synchronization.beforeCommit(readOnly))
                .isInstanceOf(expectedExceptionType)
                .satisfies(throwable -> {
                    if (throwable instanceof MessageDispatchingException dispatchingEx) {
                        assertThat(dispatchingEx.getFailedMessage()).isSameAs(message);
                    }
                });
        }

        assertThat(sentMessages).containsExactly(message);
    }

    private static Stream<Arguments> provideBeforeCommitScenarios() {
        return Stream.of(
            Arguments.of(
                "sends message successfully when readOnly is false",
                false,
                (ChannelBehavior) () -> true,
                null
            ),
            Arguments.of("sends message successfully when readOnly is true", true, (ChannelBehavior) () -> true, null),
            Arguments.of(
                "throws MessageDispatchingException with failed message when channel.send returns false",
                false,
                (ChannelBehavior) () -> false,
                MessageDispatchingException.class
            ),
            Arguments.of(
                "rethrows underlying exception when channel.send throws",
                false,
                (ChannelBehavior) () -> {
                    throw new MessageDeliveryException("Channel closed");
                },
                MessageDeliveryException.class
            )
        );
    }

    @FunctionalInterface
    private interface ChannelBehavior extends Supplier<Boolean> {}
}
