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

import static org.activiti.cloud.common.messaging.utilities.InternalChannelHelper.INTERNAL_CHANNEL_PREFIX;

import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import reactor.core.publisher.Flux;

@Configuration
@PropertySource("classpath:config/messages-events-channels.properties")
public class MessageEventsAutoConfiguration implements MessageEventsSource {

    private static final String INTERNAL_MESSAGE_EVENTS_OUTPUT = INTERNAL_CHANNEL_PREFIX + MESSAGE_EVENTS_OUTPUT;

    @Bean(INTERNAL_MESSAGE_EVENTS_OUTPUT)
    @ConditionalOnMissingBean(name = INTERNAL_MESSAGE_EVENTS_OUTPUT)
    @Override
    public MessageChannel messageEventsOutput() {
        return MessageChannels.direct(INTERNAL_MESSAGE_EVENTS_OUTPUT)
            .get();
    }

    @FunctionBinding(output = MessageEventsSource.MESSAGE_EVENTS_OUTPUT)
    @ConditionalOnMissingBean(name = "messageEventsOutputSupplier")
    @Bean
    public Supplier<Flux<Message<?>>> messageEventsOutputSupplier() {
        return () -> Flux.from(IntegrationFlows.from(messageEventsOutput())
            .log(LoggingHandler.Level.INFO,"messageEventsOutputSupplier")
            .toReactivePublisher());
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageEventsDispatcher messageEventsDispatcher(BindingServiceProperties bindingServiceProperties) {

        return new MessageEventsDispatcher(messageEventsOutput(),
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
    public BpmnMessageReceivedEventMessageProducer throwMessageReceivedEventListener(MessageEventsDispatcher messageEventsDispatcher,
        BpmnMessageEventMessageBuilderFactory messageBuilderFactory) {
        return new BpmnMessageReceivedEventMessageProducer(messageEventsDispatcher,
            messageBuilderFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public BpmnMessageWaitingEventMessageProducer throwMessageWaitingEventMessageProducer(MessageEventsDispatcher messageEventsDispatcher,
        BpmnMessageEventMessageBuilderFactory messageBuilderFactory) {
        return new BpmnMessageWaitingEventMessageProducer(messageEventsDispatcher,
            messageBuilderFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public BpmnMessageSentEventMessageProducer bpmnMessageSentEventProducer(MessageEventsDispatcher messageEventsDispatcher,
        BpmnMessageEventMessageBuilderFactory messageBuilderFactory) {
        return new BpmnMessageSentEventMessageProducer(messageEventsDispatcher,
            messageBuilderFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public StartMessageDeployedEventMessageProducer MessageDeployedEventMessageProducer(MessageEventsDispatcher messageEventsDispatcher,
        StartMessageDeployedEventMessageBuilderFactory messageBuilderFactory) {
        return new StartMessageDeployedEventMessageProducer(messageEventsDispatcher,
            messageBuilderFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageSubscriptionCancelledEventMessageProducer messageSubscriptionCancelledEventMessageProducer(MessageEventsDispatcher messageEventsDispatcher,
        MessageSubscriptionEventMessageBuilderFactory messageBuilderFactory) {
        return new MessageSubscriptionCancelledEventMessageProducer(messageEventsDispatcher,
            messageBuilderFactory);
    }

}
