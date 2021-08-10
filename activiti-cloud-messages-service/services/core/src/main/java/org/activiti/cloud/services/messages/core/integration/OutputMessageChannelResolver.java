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

package org.activiti.cloud.services.messages.core.integration;

import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.MessageChannel;

import java.util.Map;
import java.util.Optional;

public class OutputMessageChannelResolver {

    private final BinderAwareChannelResolver binderAwareChannelResolver;
    private final BindingService bindingService;
    private final ApplicationContext applicationContext;

    public OutputMessageChannelResolver(ApplicationContext applicationContext,
                                        BinderAwareChannelResolver binderAwareChannelResolver,
                                        BindingService bindingService) {
        this.applicationContext = applicationContext;
        this.binderAwareChannelResolver = binderAwareChannelResolver;
        this.bindingService = bindingService;
    }

    public MessageChannel resolve(String serviceFullName) {
        String destination = toDestination(serviceFullName);

        return getChannelBindingName(destination).filter(applicationContext::containsBean)
                                                 .map(channelName -> applicationContext.getBean(channelName,
                                                                                                MessageChannel.class))
                                                 .orElseGet(() -> binderAwareChannelResolver.resolveDestination(destination));
    }

    protected Optional<String> getChannelBindingName(String destination) {
        BindingServiceProperties bindingProperties = bindingService.getBindingServiceProperties();

        return bindingProperties.getBindings()
                                .entrySet()
                                .stream()
                                .filter(entry -> entry.getValue()
                                                      .getDestination()
                                                      .equals(destination))
                                .map(Map.Entry::getKey)
                                .findFirst();
    }

    protected String toDestination(String serviceFullName) {
        return new StringBuilder("commandConsumer").append("_")
                                                   .append(serviceFullName)
                                                   .toString();
    }
}
