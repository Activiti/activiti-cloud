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
package org.activiti.cloud.services.messages.events.config;

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.messages.events.producer.BpmnMessageReceivedEventMessageProducer;
import org.activiti.cloud.services.messages.events.support.BpmnMessageEventMessageBuilderFactory;
import org.activiti.cloud.services.messages.events.support.MessageEventsBridgeDispatcher;
import org.activiti.cloud.services.messages.events.support.MessageSubscriptionEventMessageBuilderFactory;
import org.activiti.cloud.services.messages.events.support.StartMessageDeployedEventMessageBuilderFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config/messages-events-channels.properties")
public class MessageEventsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MessageEventsBridgeDispatcher messageEventsDispatcher(StreamBridge streamBridge,
            BindingServiceProperties bindingServiceProperties) {
        return new MessageEventsBridgeDispatcher(streamBridge,
                bindingServiceProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public BpmnMessageEventMessageBuilderFactory messageEventPayloadMessageBuilderFactory(RuntimeBundleProperties properties) {
        return new BpmnMessageEventMessageBuilderFactory(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public StartMessageDeployedEventMessageBuilderFactory messageDeployedEventMessageBuilderFactory(RuntimeBundleProperties properties) {
        return new StartMessageDeployedEventMessageBuilderFactory(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageSubscriptionEventMessageBuilderFactory messageSubscriptionEventMessageBuilderFactory(RuntimeBundleProperties properties) {
        return new MessageSubscriptionEventMessageBuilderFactory(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "activiti.miprueba", matchIfMissing = false)
    public BpmnMessageReceivedEventMessageProducer throwMessageReceivedEventListener(MessageEventsBridgeDispatcher messageEventsDispatcher,
            BpmnMessageEventMessageBuilderFactory messageBuilderFactory) {
        return new BpmnMessageReceivedEventMessageProducer(messageEventsDispatcher,
                messageBuilderFactory);
    }
}
