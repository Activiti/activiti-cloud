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
import java.util.function.Function;
import org.activiti.cloud.common.messaging.functional.Connector;
import org.activiti.cloud.common.messaging.functional.ConnectorBinding;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
public class ConnectorConfiguration {

    @Bean
    public BeanPostProcessor connectorBeanPostProcessor(DefaultListableBeanFactory beanFactory,
        IntegrationFlowContext integrationFlowContext,
        StreamFunctionProperties streamFunctionProperties,
        StreamBridge streamBridge,
        FunctionBindingPropertySource functionBindingPropertySource,
        Function<String, String> resolveExpression,
        BindingServiceProperties bindingServiceProperties) {

        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (Connector.class.isInstance(bean)) {
                    String connectorName = beanName;
                    String functionName = connectorName + "Connector";
                    Connector connector = Connector.class.cast(bean);

                    functionBindingPropertySource.register(functionName);

                    Optional.ofNullable(beanFactory.findAnnotationOnBean(beanName, ConnectorBinding.class))
                        .ifPresent(functionDefinition -> {
                            Optional.of(functionDefinition.output())
                                .filter(StringUtils::hasText)
                                .ifPresent(output -> {
                                    Optional.ofNullable(bindingServiceProperties.getBindingDestination(output))
                                        .ifPresentOrElse(
                                            binding -> streamFunctionProperties.getBindings()
                                                .put(beanName + "-out-0", binding),
                                            () -> streamFunctionProperties.getBindings()
                                                .put(beanName + "-out-0", output)
                                        );
                                });

                            Optional.of(functionDefinition.input())
                                .filter(StringUtils::hasText)
                                .ifPresent(input -> {
                                    streamFunctionProperties.getBindings()
                                        .put(functionName + "-in-0", input);
                                });
                        });

                    GenericHandler<String> handler = (request, headers) -> {
                        String result = connector.apply(request);

                        Message<String> response = MessageBuilder.withPayload(result)
                            .build();
                        String destination = headers.get("resultDestination", String.class);

                        if (StringUtils.hasText(destination)) {
                            streamBridge.send(destination,
                                response);
                            return null;
                        }

                        return response;
                    };

                    GenericSelector<Message<?>> selector = Optional.ofNullable(beanFactory.findAnnotationOnBean(beanName, ConnectorBinding.class))
                        .map(ConnectorBinding::condition)
                        .filter(StringUtils::hasText)
                        .map(resolveExpression)
                        .map(ExpressionEvaluatingSelector::new)
                        .orElseGet(() -> new ExpressionEvaluatingSelector("true"));

                    IntegrationFlow connectorFlow = IntegrationFlows.from(ConnectorMessageFunction.class,
                            (gateway) -> gateway.beanName(functionName)
                                .replyTimeout(0L))
                        .log(LoggingHandler.Level.INFO,functionName + ".integrationRequest")
                        .filter(selector)
                        .handle(String.class, handler)
                        .log(LoggingHandler.Level.INFO,functionName + ".integrationResult")
                        .bridge()
                        .get();

                    String inputChannel = streamFunctionProperties.getInputBindings(functionName)
                        .stream()
                        .findFirst()
                        .orElse(functionName);

                    IntegrationFlow inputChannelFlow = IntegrationFlows.from(inputChannel)
                        .gateway(connectorFlow, spec -> spec.replyTimeout(0L))
                        .get();

                    integrationFlowContext.registration(inputChannelFlow)
                        .register();
                }
                return bean;
            }
        };
    }

    @Bean
    Function<String, String> resolveExpression(ConfigurableApplicationContext applicationContext) {
        return value -> {
            BeanExpressionResolver resolver = applicationContext.getBeanFactory()
                .getBeanExpressionResolver();
            BeanExpressionContext expressionContext = new BeanExpressionContext(applicationContext.getBeanFactory(),
                null);

            String resolvedValue = applicationContext.getBeanFactory()
                .resolveEmbeddedValue(value);
            if (resolvedValue.startsWith("#{") && value.endsWith("}")) {
                resolvedValue = (String) resolver.evaluate(resolvedValue,
                    expressionContext);
            }
            return resolvedValue;
        };
    }

    public interface ConnectorMessageFunction extends Function<Message<String>,Message<String>> {}
}
