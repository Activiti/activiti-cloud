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
import java.util.Set;
import java.util.function.Function;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry.FunctionInvocationWrapper;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public abstract class AbstractFunctionalBindingConfiguration implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    private static String SPRING_CLOUD_STREAM_RABBIT = "spring.cloud.stream.rabbit.bindings";
    private static final Set<String> SPRING_CLOUD_STREAM_RABBIT_PRODUCER_PROPERTIES =
        Set.of("exchangeType", "routingKeyExpression", "transacted");

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Assert.notNull(applicationContext,
            this.getClass().getSimpleName() + " can not process beans because the application context is null");
        this.applicationContext = applicationContext;
    }

    protected StreamBridge getStreamBridge() {
        return this.applicationContext.getBean(StreamBridge.class);
    }

    public static String getOutBinding(String bindingName) {
        return getOutBinding(bindingName, 0);
    }

    public static String getOutBinding(String bindingName, int arity) {
        return String.format("%s-out-%d", bindingName, arity);
    }

    public static String getInBinding(String bindingName) {
        return getInBinding(bindingName, 0);
    }

    public static String getInBinding(String bindingName, int arity) {
        return String.format("%s-in-%d", bindingName, arity);
    }

    protected void setOutput(String beanOutName, String outputAnnotation, BindingServiceProperties bindingServiceProperties,
        StreamFunctionProperties streamFunctionProperties, ConfigurableEnvironment environment) {

        Optional.of(outputAnnotation)
            .filter(StringUtils::hasText)
            .ifPresent(output -> {
                setOutProperties(streamFunctionProperties, beanOutName,
                    output, bindingServiceProperties);
            });
    }

    protected void setInput(String beanInName, String inputAnnotation, StreamFunctionProperties streamFunctionProperties,
        BindingServiceProperties bindingServiceProperties) {

        Optional.of(inputAnnotation)
            .filter(StringUtils::hasText)
            .ifPresent(input -> {
                streamFunctionProperties.getBindings()
                    .put(beanInName, input);
                setInProperties(streamFunctionProperties, beanInName, input, bindingServiceProperties);
            });
    }

    protected void setOutProperties(StreamFunctionProperties streamFunctionProperties,
        String beanOutName,
        String binding,
        BindingServiceProperties bindingServiceProperties) {
        streamFunctionProperties.getBindings().put(beanOutName, binding);
        Optional.ofNullable(bindingServiceProperties.getProducerProperties(binding))
            .ifPresent(producerProperties -> {
                bindingServiceProperties.getBindingProperties(beanOutName).setProducer(producerProperties);
            });
    }

    protected void setInProperties(StreamFunctionProperties streamFunctionProperties,
        String beanInName,
        String binding,
        BindingServiceProperties bindingServiceProperties) {

        Optional.ofNullable(bindingServiceProperties.getBindingProperties(binding))
            .ifPresent(bindingProperties -> {
                bindingServiceProperties.getBindings().putIfAbsent(beanInName, new BindingProperties());
                bindingServiceProperties.getBindingProperties(beanInName).setDestination(binding);
                bindingServiceProperties.getBindingProperties(beanInName).setContentType(bindingProperties.getContentType());
            });
    }

    @Bean("resolveExpression")
    @ConditionalOnMissingBean(name = "resolveExpression")
    public Function<String, String> resolveExpression(ConfigurableApplicationContext applicationContext) {
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

    protected FunctionInvocationWrapper functionFromDefinition(String definition) {
        FunctionRegistry functionRegistry = applicationContext.getBean(FunctionRegistry.class);
        FunctionInvocationWrapper function = functionRegistry.lookup(definition);
        Assert.notNull(function, "Failed to lookup function '" + definition + "'");
        return function;
    }
}
