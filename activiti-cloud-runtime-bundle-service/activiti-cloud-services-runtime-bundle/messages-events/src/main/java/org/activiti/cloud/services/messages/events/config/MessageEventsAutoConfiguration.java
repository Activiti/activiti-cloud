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
import org.activiti.cloud.services.messages.events.channels.MessageEventsSource;
import org.activiti.cloud.services.messages.events.producer.BpmnMessageReceivedEventMessageProducer;
import org.activiti.cloud.services.messages.events.producer.BpmnMessageSentEventMessageProducer;
import org.activiti.cloud.services.messages.events.producer.BpmnMessageWaitingEventMessageProducer;
import org.activiti.cloud.services.messages.events.producer.MessageSubscriptionCancelledEventMessageProducer;
import org.activiti.cloud.services.messages.events.producer.StartMessageDeployedEventMessageProducer;
import org.activiti.cloud.services.messages.events.support.BpmnMessageEventMessageBuilderFactory;
import org.activiti.cloud.services.messages.events.support.MessageEventsDispatcher;
import org.activiti.cloud.services.messages.events.support.MessageSubscriptionEventMessageBuilderFactory;
import org.activiti.cloud.services.messages.events.support.StartMessageDeployedEventMessageBuilderFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@Configuration(proxyBeanMethods = true)
@PropertySource("classpath:config/messages-events-channels.properties")
@Import(MessageEventsSourceConfiguration.class)
public class MessageEventsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MessageEventsDispatcher messageEventsDispatcher(
        BindingServiceProperties bindingServiceProperties,
        MessageEventsSource messageEventsSource
    ) {
        return new MessageEventsDispatcher(messageEventsSource.messageEventsOutput(), bindingServiceProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public BpmnMessageEventMessageBuilderFactory messageEventPayloadMessageBuilderFactory(
        RuntimeBundleProperties properties
    ) {
        return new BpmnMessageEventMessageBuilderFactory(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public StartMessageDeployedEventMessageBuilderFactory messageDeployedEventMessageBuilderFactory(
        RuntimeBundleProperties properties
    ) {
        return new StartMessageDeployedEventMessageBuilderFactory(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageSubscriptionEventMessageBuilderFactory messageSubscriptionEventMessageBuilderFactory(
        RuntimeBundleProperties properties
    ) {
        return new MessageSubscriptionEventMessageBuilderFactory(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public BpmnMessageReceivedEventMessageProducer throwMessageReceivedEventListener(
        MessageEventsDispatcher messageEventsDispatcher,
        BpmnMessageEventMessageBuilderFactory messageBuilderFactory
    ) {
        return new BpmnMessageReceivedEventMessageProducer(messageEventsDispatcher, messageBuilderFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public BpmnMessageWaitingEventMessageProducer throwMessageWaitingEventMessageProducer(
        MessageEventsDispatcher messageEventsDispatcher,
        BpmnMessageEventMessageBuilderFactory messageBuilderFactory
    ) {
        return new BpmnMessageWaitingEventMessageProducer(messageEventsDispatcher, messageBuilderFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public BpmnMessageSentEventMessageProducer bpmnMessageSentEventProducer(
        MessageEventsDispatcher messageEventsDispatcher,
        BpmnMessageEventMessageBuilderFactory messageBuilderFactory
    ) {
        return new BpmnMessageSentEventMessageProducer(messageEventsDispatcher, messageBuilderFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public StartMessageDeployedEventMessageProducer MessageDeployedEventMessageProducer(
        MessageEventsDispatcher messageEventsDispatcher,
        StartMessageDeployedEventMessageBuilderFactory messageBuilderFactory
    ) {
        return new StartMessageDeployedEventMessageProducer(messageEventsDispatcher, messageBuilderFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageSubscriptionCancelledEventMessageProducer messageSubscriptionCancelledEventMessageProducer(
        MessageEventsDispatcher messageEventsDispatcher,
        MessageSubscriptionEventMessageBuilderFactory messageBuilderFactory
    ) {
        return new MessageSubscriptionCancelledEventMessageProducer(messageEventsDispatcher, messageBuilderFactory);
    }
}
