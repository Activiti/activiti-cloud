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

package org.activiti.cloud.services.messages.core.router;

import org.springframework.integration.mapping.MessageMappingException;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static org.activiti.cloud.services.messages.core.integration.MessageEventHeaders.MESSAGE_EVENT_OUTPUT_DESTINATION;

public class CommandConsumerMessageRouter extends AbstractMessageRouter {

    private final DestinationResolver<MessageChannel> destinationResolver;

    public CommandConsumerMessageRouter(DestinationResolver<MessageChannel> destinationResolver) {
        this.destinationResolver = destinationResolver;
    }

    @Override
    protected Collection<MessageChannel> determineTargetChannels(Message<?> message) {
        Optional<String> destination = getHeader(message, MESSAGE_EVENT_OUTPUT_DESTINATION);

        MessageChannel messageChannel = destination.map(destinationResolver::resolveDestination)
            .orElseThrow(() -> new MessageMappingException(message,
                "Unable to determine target channel for message"));
        return Arrays.asList(messageChannel);
    }

    private Optional<String> getHeader(Message<?> message, String key) {
        return Optional.ofNullable(message.getHeaders()
            .get(key, String.class));
    }
}
