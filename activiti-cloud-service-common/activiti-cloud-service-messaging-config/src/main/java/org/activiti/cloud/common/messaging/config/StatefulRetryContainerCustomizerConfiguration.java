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
package org.activiti.cloud.common.messaging.config;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.StatefulRetryOperationsInterceptor;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.retry.ImmediateRequeueMessageRecoverer;
import org.springframework.amqp.rabbit.retry.MessageKeyGenerator;
import org.springframework.amqp.rabbit.retry.NewMessageIdentifier;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.binder.rabbit.RabbitMessageChannelBinder;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.ErrorMessage;

@AutoConfiguration
@ConditionalOnClass(RabbitMessageChannelBinder.class)
@ConditionalOnProperty("activiti.cloud.messaging.rabbitmq.stateful-retry-interceptor.enabled")
public class StatefulRetryContainerCustomizerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ImmediateRequeueMessageRecoverer immediateRequeueMessageRecoverer() {
        return new ImmediateRequeueMessageRecoverer();
    }

    @Bean
    @ConditionalOnMissingBean
    RejectAndDontRequeueRecoverer rejectAndDontRequeueRecoverer() {
        return new RejectAndDontRequeueRecoverer();
    }

    @Bean
    @ConditionalOnMissingBean
    Consumer<ErrorMessage> statefulRetryErrorMessageHandler(
        ImmediateRequeueMessageRecoverer immediateRequeueMessageRecoverer
    ) {
        return new ErrorHandlerDefinitionMessageRecoverer(immediateRequeueMessageRecoverer).withRecoverableExceptions(
            Map.of(MessageHandlingException.class, true)
        );
    }

    @Bean
    public ListenerContainerCustomizer<MessageListenerContainer> statefulRetryOperationsInterceptorContainerCustomizer(
        StatefulRetryOperationsInterceptor statefulRetryOperationsInterceptor,
        ListenerExecutionFailedExceptionInterceptor listenerExecutionFailedExceptionInterceptor
    ) {
        return (messageListenerContainer, destination, group) -> {
            if (messageListenerContainer instanceof AbstractMessageListenerContainer abstractMessageListenerContainer) {
                abstractMessageListenerContainer.setAdviceChain(
                    statefulRetryOperationsInterceptor,
                    listenerExecutionFailedExceptionInterceptor
                );
                abstractMessageListenerContainer.setStatefulRetryFatalWithNullMessageId(false);
            }
        };
    }

    @Bean
    public StatefulRetryOperationsInterceptor statefulRetryOperationsInterceptor(
        MessageKeyGenerator messageKeyGenerator,
        NewMessageIdentifier newMessageIdentifier,
        RejectAndDontRequeueRecoverer rejectAndDontRequeueRecoverer,
        @Value("${activiti.cloud.messaging.rabbitmq.stateful-retry-interceptor.max-retries:3}") Integer maxRetries,
        @Value("${activiti.cloud.messaging.rabbitmq.stateful-retry-interceptor.delay:10ms}") Duration delay
    ) {
        return RetryInterceptorBuilder.stateful()
            .maxRetries(maxRetries)
            .configureRetryPolicy(retryPolicy -> retryPolicy.delay(delay))
            .messageKeyGenerator(messageKeyGenerator)
            .newMessageIdentifier(newMessageIdentifier)
            .recoverer(rejectAndDontRequeueRecoverer)
            .build();
    }

    @Bean
    MessageKeyGenerator messageKeyGenerator() {
        return message -> {
            final var messageProperties = message.getMessageProperties();
            final var messageId = messageProperties.getMessageId();
            if (messageId == null && Boolean.TRUE.equals(messageProperties.isRedelivered())) {
                messageProperties.setFinalRetryForMessageWithNoId(true);
            }
            return messageId;
        };
    }

    @Bean
    NewMessageIdentifier newMessageIdentifier() {
        return message -> Boolean.FALSE.equals(message.getMessageProperties().isRedelivered());
    }

    @Bean
    ListenerExecutionFailedExceptionInterceptor listenerExecutionFailedExceptionInterceptor() {
        return new ListenerExecutionFailedExceptionInterceptor();
    }

    public static class ListenerExecutionFailedExceptionInterceptor implements MethodInterceptor {

        @Override
        public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {
            try {
                return invocation.proceed();
            } catch (ListenerExecutionFailedException exception) {
                throw new ListenerExecutionFailedException(
                    exception.getMessage(),
                    NestedExceptionUtils.getMostSpecificCause(exception)
                );
            }
        }
    }
}
