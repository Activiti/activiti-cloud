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
package org.activiti.cloud.common.messaging.function.router;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.StatefulRetryOperationsInterceptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.retry.MessageKeyGenerator;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.NewMessageIdentifier;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ListenerContainerStatefulRetryConfiguration {

    @Bean
    public ListenerContainerCustomizer<MessageListenerContainer> containerCustomizer(
        StatefulRetryOperationsInterceptor statefulRetryInterceptor
    ) {
        return (messageListenerContainer, destination, group) -> {
            if (messageListenerContainer instanceof AbstractMessageListenerContainer abstractMessageListenerContainer) {
                abstractMessageListenerContainer.setAdviceChain(statefulRetryInterceptor);
            }
        };
    }

    @Bean
    public StatefulRetryOperationsInterceptor statefulRetryInterceptor(
        MessageKeyGenerator messageKeyGenerator,
        NewMessageIdentifier newMessageIdentifier,
        MessageRecoverer republishMessageRecoverer
    ) {
        return RetryInterceptorBuilder.stateful()
            .maxRetries(0)
            .messageKeyGenerator(messageKeyGenerator)
            .newMessageIdentifier(newMessageIdentifier)
            .recoverer(republishMessageRecoverer)
            .build();
    }

    @Bean
    MessageKeyGenerator messageKeyGenerator() {
        return message -> message.getMessageProperties().getMessageId();
    }

    @Bean
    NewMessageIdentifier newMessageIdentifier() {
        return message -> Boolean.FALSE.equals(message.getMessageProperties().isRedelivered());
    }

    @Bean
    MessageRecoverer republishMessageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RequeueMessageRecoverer(
            rabbitTemplate,
            new RepublishMessageRecoverer(rabbitTemplate, dlqBinding().getExchange(), dlqBinding().getRoutingKey())
        );
    }

    @Bean
    Queue dlq() {
        return new Queue("errors.dlq");
    }

    @Bean
    DirectExchange dlx() {
        return new DirectExchange("dlx");
    }

    @Bean
    Binding dlqBinding() {
        return BindingBuilder.bind(dlq()).to(dlx()).with("#");
    }
}
