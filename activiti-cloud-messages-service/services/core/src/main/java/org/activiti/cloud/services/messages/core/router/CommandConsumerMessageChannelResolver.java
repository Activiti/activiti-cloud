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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.activiti.cloud.services.messages.core.channels.MessageConnectorSource;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;

public class CommandConsumerMessageChannelResolver implements DestinationResolver<MessageChannel> {

    private final Map<String, MessageChannel> bindingChannelCache = new ConcurrentHashMap<>();
    private final BindingService bindingService;
    private final Function<String, String> destinationMapper;
    private final StreamBridge streamBridge;

    public CommandConsumerMessageChannelResolver(
        Function<String, String> destinationMapper,
        BindingService bindingService,
        StreamBridge streamBridge
    ) {
        this.destinationMapper = destinationMapper;
        this.streamBridge = streamBridge;
        this.bindingService = bindingService;
    }

    @Override
    public MessageChannel resolveDestination(String destination) throws DestinationResolutionException {
        String channelName = getChannelName(destination).orElse(MessageConnectorSource.OUTPUT);

        return this.bindingChannelCache.computeIfAbsent(channelName, this::createMessageChannelProxyForBinding);
    }

    protected Optional<String> getChannelName(String destination) {
        BindingServiceProperties bindingProperties = bindingService.getBindingServiceProperties();

        return bindingProperties
            .getBindings()
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().getDestination().equals(destination))
            .map(Map.Entry::getKey)
            .findFirst();
    }

    private MessageChannel createMessageChannelProxyForBinding(String bindingName) {
        return (message, timeout) -> this.streamBridge.send(bindingName, message);
    }
}
