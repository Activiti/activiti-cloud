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
package org.activiti.cloud.common.messaging.config;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.functional.ConditionalFunctionDefinition;
import org.activiti.cloud.common.messaging.functional.FunctionDefinition;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Configuration
public class ConditionalFunctionDefinitionConfiguration {

    @Bean
    public BeanPostProcessor conditionalFunctionDefinitionBeanPostProcessor(DefaultListableBeanFactory beanFactory,
        FunctionDefinitionPropertySource functionDefinitionPropertySource,
        StreamFunctionProperties streamFunctionProperties,
        IntegrationFlowContext integrationFlowContext) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (Consumer.class.isInstance(bean) ||
                    Function.class.isInstance(bean)) {

                    Optional.ofNullable(beanFactory.findAnnotationOnBean(beanName, ConditionalFunctionDefinition.class))
                        .ifPresent(functionDefinition -> {
                            String listenerName = beanName + "Listener";

                            functionDefinitionPropertySource.register(listenerName);

                            Optional.of(functionDefinition.output())
                                .filter(StringUtils::hasText)
                                .ifPresent(output -> {
                                    streamFunctionProperties.getBindings()
                                        .put(listenerName + "-out-0", output);
                                });

                            Optional.of(functionDefinition.input())
                                .filter(StringUtils::hasText)
                                .ifPresent(input -> {
                                    streamFunctionProperties.getBindings()
                                        .put(listenerName + "-in-0", input);
                                });

                            Optional.of(functionDefinition.condition())
                                .filter(StringUtils::hasText)
                                .ifPresent(condition -> {
                                    IntegrationFlow flow = IntegrationFlows.from(MessageFunctionGateway.class,
                                            (gateway) -> gateway.beanName(listenerName)
                                                .replyTimeout(0L))
                                        .filter(condition)
                                        .handle(bean)
                                        .log(LoggingHandler.Level.INFO, listenerName + "- Result")
                                        .bridge()
                                        .get();

                                    integrationFlowContext.registration(flow)
                                        .register();
                                });
                        });
                }

                return bean;
            }
        };
    }

    private class ConsumerMessageHandler implements MessageHandler {

        private Consumer consumer;

        public ConsumerMessageHandler(Object bean) {
            this.consumer = (Consumer) bean;
        }

        @Override
        public void handleMessage(Message<?> message) throws MessagingException {
            consumer.accept(message);
        }
    }

    public interface MessageFunctionGateway extends Consumer<Message<?>> {

        @Override
        void accept(Message<?> message) throws MessagingException;
    }

}
