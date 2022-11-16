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
import org.activiti.cloud.common.messaging.config.ConnectorConfiguration.ConnectorMessageFunction;
import org.activiti.cloud.common.messaging.functional.FunctionDefinition;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Configuration
public class FunctionDefinitionConfiguration {

    @Bean
    public FunctionDefinitionPropertySource functionDefinitionPropertySource(ConfigurableApplicationContext applicationContext) {
        return new FunctionDefinitionPropertySource(applicationContext.getEnvironment());
    }

    @Bean
    public BeanPostProcessor functionDefinitionBeanPostProcessor(DefaultListableBeanFactory beanFactory,
        FunctionDefinitionPropertySource functionDefinitionPropertySource,
        StreamFunctionProperties streamFunctionProperties) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (Supplier.class.isInstance(bean) ||
                    Function.class.isInstance(bean) ||
                    Consumer.class.isInstance(bean)) {

                    Optional.ofNullable(beanFactory.findAnnotationOnBean(beanName, FunctionDefinition.class))
                        .ifPresent(functionDefinition -> {
                            functionDefinitionPropertySource.register(beanName);

                            Optional.of(functionDefinition.output())
                                .filter(StringUtils::hasText)
                                .ifPresent(output -> {
                                    streamFunctionProperties.getBindings()
                                        .put(beanName + "-out-0", output);
                                });

                            Optional.of(functionDefinition.input())
                                .filter(StringUtils::hasText)
                                .ifPresent(input -> {
                                    streamFunctionProperties.getBindings()
                                        .put(beanName + "-in-0", input);
                                });
                        });
                }

                return bean;
            }
        };
    }

}