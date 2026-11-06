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

import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.support.GenericMessage;

class OutboxDestinationProviderTest {

    @Test
    void apply_whenChannelMatchesExcludePattern_returnsNull() {
        // Arrange
        OutboxProperties properties = outboxProperties("excluded.*");
        OutboxDestinationProvider provider = new OutboxDestinationProvider(properties);

        Message<String> message = new GenericMessage<>("payload");

        DirectChannel channel = directChannel("excludedChannel");

        // Act
        String result = provider.apply(message, channel);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void apply_whenChannelDoesNotMatchExcludePattern_returnsBeanName() {
        // Arrange
        OutboxProperties properties = outboxProperties("excluded.*");
        OutboxDestinationProvider provider = new OutboxDestinationProvider(properties);

        Message<String> message = new GenericMessage<>("payload");

        DirectChannel channel = directChannel("regularChannel");

        // Act
        String result = provider.apply(message, channel);

        // Assert
        assertThat(result).isEqualTo("regularChannel");
    }

    @Test
    void apply_whenChannelDoesNotMatchEmptyExcludePattern_returnsBeanName() {
        // Arrange
        OutboxProperties properties = outboxProperties("^$");
        OutboxDestinationProvider provider = new OutboxDestinationProvider(properties);

        Message<String> message = new GenericMessage<>("payload");

        DirectChannel channel = directChannel("regularChannel");

        // Act
        String result = provider.apply(message, channel);

        // Assert
        assertThat(result).isEqualTo("regularChannel");
    }

    @Test
    void apply_whenChannelIsNotNamedComponent_throwsDestinationResolutionException() {
        // Arrange
        OutboxProperties properties = outboxProperties(".*");
        OutboxDestinationProvider provider = new OutboxDestinationProvider(properties);

        Message<String> message = new GenericMessage<>("payload");

        // Use an anonymous MessageChannel implementation that does NOT implement NamedComponent
        MessageChannel unnamedChannel = new MessageChannel() {
            @Override
            public boolean send(Message<?> message) {
                return true;
            }

            @Override
            public boolean send(Message<?> message, long timeout) {
                return true;
            }

            @Override
            public String toString() {
                return "AnonymousUnnamedChannel";
            }
        };

        // Act & Assert
        assertThatThrownBy(() -> provider.apply(message, unnamedChannel))
            .isInstanceOf(DestinationResolutionException.class)
            .hasMessageContaining("Cannot resolve outbox destination for message")
            .hasMessageContaining(message.toString())
            .hasMessageContaining(unnamedChannel.toString());
    }

    // --- Helper Methods ---

    private DirectChannel directChannel(String beanName) {
        return new DirectChannel() {
            {
                setBeanName(beanName);
            }
        };
    }

    private OutboxProperties outboxProperties(String... regexes) {
        return new OutboxProperties() {
            {
                setExcludes(
                    Stream.of(regexes)
                        .map(Pattern::compile)
                        .toArray(Pattern[]::new)
                );
            }
        };
    }
}
