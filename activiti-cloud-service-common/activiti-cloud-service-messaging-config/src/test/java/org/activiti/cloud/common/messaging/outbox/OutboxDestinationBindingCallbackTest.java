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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.InterceptableChannel;

class OutboxDestinationBindingCallbackTest {

    private ChannelInterceptor outboxChannelInterceptor;
    private OutboxDestinationBindingCallback callback;

    @BeforeEach
    void setUp() {
        outboxChannelInterceptor = mock(ChannelInterceptor.class);
        callback = new OutboxDestinationBindingCallback(outboxChannelInterceptor);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("interceptableChannelsProvider")
    @DisplayName("Should add interceptor to any InterceptableChannel implementation")
    void shouldAddInterceptorToInterceptableChannels(
        String description,
        MessageChannel channel,
        String channelName,
        ProducerProperties producerProperties,
        Object extendedProperties
    ) {
        // Act
        callback.configure(channelName, channel, producerProperties, extendedProperties);

        // Assert
        assertThat(channel).isInstanceOfSatisfying(InterceptableChannel.class, interceptable ->
            assertThat(interceptable.getInterceptors()).hasSize(1).containsExactly(outboxChannelInterceptor)
        );
    }

    @ParameterizedTest(name = "[{index}] Non-interceptable channel: {0}")
    @NullSource
    @MethodSource("nonInterceptableChannelsProvider")
    @DisplayName("Should safely ignore null or non-interceptable channels")
    void shouldIgnoreNonInterceptableOrNullChannels(MessageChannel channel) {
        // Act & Assert
        assertThatCode(() ->
            callback.configure("outbox-destination", channel, new ProducerProperties(), null)
        ).doesNotThrowAnyException();

        if (channel != null) {
            verifyNoInteractions(channel);
        }
        verifyNoInteractions(outboxChannelInterceptor);
    }

    static Stream<Arguments> interceptableChannelsProvider() {
        return Stream.of(
            Arguments.of(
                "DirectChannel with full properties",
                new DirectChannel(),
                "orders-outbox",
                new ProducerProperties(),
                new Object()
            ),
            Arguments.of("PublishSubscribeChannel with null metadata", new PublishSubscribeChannel(), null, null, null),
            Arguments.of(
                "ExecutorSubscribableChannel with empty name",
                new ExecutorSubscribableChannel(),
                "",
                new ProducerProperties(),
                null
            )
        );
    }

    static Stream<MessageChannel> nonInterceptableChannelsProvider() {
        return Stream.of(mock(MessageChannel.class), spy(new TestChannel()));
    }

    static class TestChannel implements MessageChannel {

        @Override
        public boolean send(Message<?> message, long timeout) {
            return true;
        }
    }
}
