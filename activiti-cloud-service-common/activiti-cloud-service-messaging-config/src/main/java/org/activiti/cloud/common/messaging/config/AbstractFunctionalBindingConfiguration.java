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

import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
import org.springframework.core.env.MapPropertySource;
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

    protected void checkConfiguration(Object bean, String beanName, String inputDefinition, String input, String outputDefinition, String output) {
        if (Supplier.class.isInstance(bean)) {
            Assert.hasText(output, () -> String.format("Missing `output` value for supplier %s", beanName));
        } else if(Function.class.isInstance(bean)){
            Assert.hasText(input, () -> String.format("Missing `input` value for function %s", beanName));
            Assert.hasText(output, () -> String.format("Missing `output` value for function %s", beanName));
            Assert.state(!input.equals(output),
                () -> String.format("Input and output destination matches for %s: %s -> %s", beanName, input, output));
        } else if(Consumer.class.isInstance(bean)){
            Assert.hasText(input, () -> String.format("Missing `input` value for consumer %s", beanName));
        }
    }

    protected String setOutput(String beanOutName, String outputAnnotation, BindingServiceProperties bindingServiceProperties,
        StreamFunctionProperties streamFunctionProperties, ConfigurableEnvironment environment) {

        AtomicReference<String> outputDestination = new AtomicReference<>(null);

        Optional.of(outputAnnotation)
            .filter(StringUtils::hasText)
            .ifPresent(output -> {
                String destination = Optional.ofNullable(bindingServiceProperties.getBindingDestination(output)).orElse(output);
                setOutProperties(streamFunctionProperties, beanOutName,
                    destination, bindingServiceProperties, output);

                if(output.equals(destination)){
                    destination = createFunctionalDestination(bindingServiceProperties, streamFunctionProperties, output);
                }

                outputDestination.set(destination);

                setRabbitProducerProperties(environment, destination, output);
            });

        return outputDestination.get();
    }

    protected String setInput(String beanInName, String inputAnnotation, StreamFunctionProperties streamFunctionProperties,
        BindingServiceProperties bindingServiceProperties) {

        AtomicReference<String> inputDestination = new AtomicReference<>(null);

        Optional.of(inputAnnotation)
            .filter(StringUtils::hasText)
            .ifPresent(input -> {
                streamFunctionProperties.getBindings()
                    .put(beanInName, input);
                setInProperties(streamFunctionProperties, beanInName, input, bindingServiceProperties);
                inputDestination.set(input);
            });

        return inputDestination.get();
    }

    protected void setRabbitProducerProperties(ConfigurableEnvironment environment, String channelName, String outputBinding) {
        Map<String, Object> producerProperties = SPRING_CLOUD_STREAM_RABBIT_PRODUCER_PROPERTIES.stream()
            .filter(property -> environment.containsProperty(String.format("%s.%s.producer.%s", SPRING_CLOUD_STREAM_RABBIT, outputBinding, property)))
            .collect(Collectors.toMap(
                property -> String.format("%s.%s.producer.%s", SPRING_CLOUD_STREAM_RABBIT, channelName, property),
                property -> environment.getProperty(String.format("%s.%s.producer.%s", SPRING_CLOUD_STREAM_RABBIT, outputBinding, property))));

        if (!producerProperties.isEmpty()) {
            if (environment.getPropertySources().contains(this.getClass().getSimpleName())) {
                MapPropertySource existingSource = (MapPropertySource) environment.getPropertySources().get(this.getClass().getSimpleName());
                existingSource.getSource().putAll(producerProperties);
            } else {
                environment.getPropertySources()
                    .addAfter(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        new MapPropertySource(this.getClass().getSimpleName(),
                            producerProperties));
            }
        }
    }

    protected void setOutProperties(StreamFunctionProperties streamFunctionProperties,
        String beanOutName,
        String binding,
        BindingServiceProperties bindingServiceProperties,
        String functionDefinitionOutput) {
        streamFunctionProperties.getBindings().put(beanOutName, binding);
        Optional.ofNullable(bindingServiceProperties.getProducerProperties(functionDefinitionOutput))
            .ifPresent(producerProperties -> {
                bindingServiceProperties.getBindingProperties(beanOutName).setProducer(producerProperties);
                bindingServiceProperties.getBindingProperties(binding).setProducer(producerProperties);
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

    protected String createFunctionalDestination(BindingServiceProperties bindingServiceProperties,
        StreamFunctionProperties streamFunctionProperties, String destination) {

        final BindingProperties destinationProperties = bindingServiceProperties.getBindingProperties(destination);
        Assert.notNull(destinationProperties, () -> String.format("'%s' has no binding properties.", destination));

        final String functionBinding = String.format("%sFunctional", destination);
        if(!bindingServiceProperties.getBindings().containsKey(functionBinding)) {
            final BindingProperties functionBindingProperties = bindingServiceProperties.getBindingProperties(destination);
            functionBindingProperties.setDestination(destinationProperties.getDestination());
            functionBindingProperties.setContentType(destinationProperties.getContentType());
            functionBindingProperties.setGroup(destinationProperties.getGroup());
        } else {
            final BindingProperties functionBindingProperties = bindingServiceProperties.getBindingProperties(functionBinding);
            Assert.state(destination.equals(functionBindingProperties.getDestination()),
                () -> String.format("'%s' binding name clashes with auto-generated functional bindings.", functionBinding));
        }
        return functionBinding;
    }
}
