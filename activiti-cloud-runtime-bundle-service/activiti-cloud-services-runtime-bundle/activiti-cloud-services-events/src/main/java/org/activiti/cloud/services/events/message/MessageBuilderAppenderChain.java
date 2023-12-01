/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.services.events.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;

public class MessageBuilderAppenderChain {

    private static final String ROUTING_KEY = "routingKey";

    private static final String MESSAGE_PAYLOAD_TYPE = "messagePayloadType";

    private final List<MessageBuilderAppender> appenders = new ArrayList<>();

    // Noop routing key resolver that resolves routing key for message payload type header
    private RoutingKeyResolver<Map<String, Object>> routingKeyResolver = new DefaultRoutingKeyResolver();

    public MessageBuilderAppenderChain() {
        // Silence is golden
    }

    public <P> MessageBuilder<P> withPayload(P payload) {
        Assert.notNull(payload, "payload must not be null");

        // So we can access headers later
        MessageHeaderAccessor accessor = new MessageHeaderAccessor();
        accessor.setLeaveMutable(true);

        MessageBuilder<P> messageBuilder = MessageBuilder.withPayload(payload).setHeaders(accessor);
        // Let's resolve payload class name
        messageBuilder.setHeader(MESSAGE_PAYLOAD_TYPE, payload.getClass().getName());

        for (MessageBuilderAppender appender : appenders) {
            appender.apply(messageBuilder);
        }

        // Let's resolve and set routingKey in the message headers if present
        resolveRoutingKey(accessor.getMessageHeaders())
            .ifPresent(routingKey -> accessor.setHeader(ROUTING_KEY, routingKey));

        return messageBuilder;
    }

    protected Optional<String> resolveRoutingKey(MessageHeaders messageHeaders) {
        return Optional.ofNullable(routingKeyResolver.resolve(messageHeaders));
    }

    public MessageBuilderAppenderChain chain(MessageBuilderAppender filter) {
        Assert.notNull(filter, "filter must not be null");

        appenders.add(filter);

        return this;
    }

    public MessageBuilderAppenderChain routingKeyResolver(RoutingKeyResolver<Map<String, Object>> routingKeyResolver) {
        Assert.notNull(routingKeyResolver, "routingKeyResolver must not be null");

        this.routingKeyResolver = routingKeyResolver;

        return this;
    }

    // Default implementation
    static class DefaultRoutingKeyResolver extends AbstractMessageHeadersRoutingKeyResolver {

        private static final String EVENT_MESSAGE = "eventMessage";

        @Override
        public String resolve(Map<String, Object> headers) {
            return build(headers, MESSAGE_PAYLOAD_TYPE);
        }

        @Override
        public String getPrefix() {
            return EVENT_MESSAGE;
        }
    }
}
