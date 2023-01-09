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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.activiti.cloud.common.messaging.functional.Connector;
import org.activiti.cloud.common.messaging.functional.ConnectorBinding;
import org.activiti.cloud.common.messaging.functional.ConnectorGateway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry.FunctionInvocationWrapper;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

@Configuration
@AutoConfigureBefore(BinderFactoryAutoConfiguration.class)
@AutoConfigureAfter(FunctionBindingConfiguration.class)
public class ConnectorConfiguration extends AbstractFunctionalBindingConfiguration {

    @Bean(name = "connectorBindingPostProcessor")
    public BeanPostProcessor connectorBindingPostProcessor(DefaultListableBeanFactory beanFactory,
        IntegrationFlowContext integrationFlowContext,
        StreamFunctionProperties streamFunctionProperties,
        BindingServiceProperties bindingServiceProperties,
        FunctionBindingPropertySource functionBindingPropertySource,
        Function<String, String> resolveExpression,
        ConfigurableEnvironment environment) {

        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (Connector.class.isInstance(bean)) {
                    String connectorName = beanName;
                    String functionName = connectorName + "Connector";

                    final AtomicReference<String> responseDestination = new AtomicReference<>();

                    Optional.ofNullable(beanFactory.findAnnotationOnBean(beanName, ConnectorBinding.class))
                        .ifPresent(functionDefinition -> {

                            functionBindingPropertySource.register(beanName);
                            functionBindingPropertySource.register(functionName);

                            responseDestination.set(functionDefinition.outputHeader());

                            final String beanInName = getInBinding(functionName);
                            final String beanOutName = getOutBinding(functionName);

                            setOutput(beanOutName, functionDefinition.output(), bindingServiceProperties, streamFunctionProperties, environment);
                            setInput(beanInName, functionDefinition.input(), streamFunctionProperties, bindingServiceProperties);

                        });

                    GenericHandler<Message> handler = (message, headers) -> {
                        FunctionInvocationWrapper function = functionFromDefinition(beanName);
                        Object result = function.apply(message);

                        Message<?> response = null;
                        if(result != null) {
                            response = MessageBuilder.withPayload(result)
                                .build();
                            String destination = headers.get(responseDestination.get(), String.class);

                            if (StringUtils.hasText(destination)) {
                                getStreamBridge().send(destination, response);
                                return null;
                            }
                        }

                        return response;
                    };

                    GenericSelector<Message<?>> selector = Optional.ofNullable(beanFactory.findAnnotationOnBean(beanName, ConnectorBinding.class))
                        .map(ConnectorBinding::condition)
                        .filter(StringUtils::hasText)
                        .map(resolveExpression)
                        .map(ExpressionEvaluatingSelector::new)
                        .orElseGet(() -> new ExpressionEvaluatingSelector("true"));

                    IntegrationFlow connectorFlow = IntegrationFlows.from(ConnectorGateway.class,
                            (gateway) -> gateway.beanName(functionName)
                                .replyTimeout(0L))
                        .log(LoggingHandler.Level.INFO, functionName + ".integrationRequest")
                        .filter(selector)
                        .handle(Message.class, handler)
                        .log(LoggingHandler.Level.INFO, functionName + ".integrationResult")
                        .bridge()
                        .get();

                    integrationFlowContext.registration(connectorFlow)
                        .register();
                }
                return bean;
            }
        };
    }

}
