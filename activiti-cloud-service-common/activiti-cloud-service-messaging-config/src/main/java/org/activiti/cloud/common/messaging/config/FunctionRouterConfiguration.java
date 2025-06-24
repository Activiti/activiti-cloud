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

import java.util.ArrayList;
import java.util.Optional;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;

@AutoConfiguration(
    before = InputBindingConfiguration.class,
    after = { BinderFactoryAutoConfiguration.class, ActivitiMessagingDestinationsAutoConfiguration.class }
)
@ConditionalOnProperty("activiti.cloud.messaging.function-router.enabled")
public class FunctionRouterConfiguration {

    public static final String FUNCTION_DESTINATION = "spring.cloud.function.destination";
    public static final String FUNCTION_ROUTER_INPUT = "functionRouterInput";
    public static final String FUNCTION_ROUTER_OUTPUT = "functionRouterOutput";

    @Configuration
    static class FunctionRouterChannels {

        @InputBinding(FUNCTION_ROUTER_INPUT)
        SubscribableChannel functionRouterInput() {
            return MessageChannels.publishSubscribe(FUNCTION_ROUTER_INPUT).getObject();
        }

        @OutputBinding(FUNCTION_ROUTER_OUTPUT)
        SubscribableChannel functionRouterOutput() {
            return MessageChannels.direct(FUNCTION_ROUTER_OUTPUT).getObject();
        }
    }

    @Bean
    MessageRoutingCallback functionRouterMessageRoutingCallback(ActivitiCloudMessagingProperties messagingProperties) {
        return new MessageRoutingCallback() {
            @Override
            public String routingResult(Message<?> message) {
                var destination = (String) message.getHeaders().get(FUNCTION_DESTINATION);

                var registrations = messagingProperties
                    .getFunctionRouter()
                    .getRegistrations()
                    .getOrDefault(destination, new ArrayList<>());

                return String.join(",", registrations);
            }
        };
    }

    @Bean
    public BeanPostProcessor outputBindingChannelPostProcessor(
        @Autowired DefaultListableBeanFactory beanFactory,
        @Autowired BindingServiceProperties bindingServiceProperties
    ) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DirectChannel messageChannel) {
                    Optional
                        .ofNullable(beanFactory.findAnnotationOnBean(beanName, OutputBinding.class))
                        .ifPresent(outputBinding -> {
                            messageChannel.addInterceptor(
                                new ChannelInterceptor() {
                                    @Override
                                    public Message<?> preSend(Message<?> message, MessageChannel channel) {
                                        var messageToUse = Optional
                                            .ofNullable(bindingServiceProperties.getBindings().get(beanName))
                                            .map(binding ->
                                                MessageBuilder
                                                    .fromMessage(message)
                                                    .setHeader(FUNCTION_DESTINATION, binding.getDestination())
                                                    .build()
                                            )
                                            .orElse(null);

                                        return messageToUse != null ? messageToUse : message;
                                    }
                                }
                            );
                        });
                }
                return bean;
            }
        };
    }
}
