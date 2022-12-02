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
import java.util.stream.Collectors;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public abstract class AbstractFunctionalBindingConfiguration {

    private static String SPRING_CLOUD_STREAM_RABBIT = "spring.cloud.stream.rabbit.bindings";
    private static final Set<String> SPRING_CLOUD_STREAM_RABBIT_PRODUCER_PROPERTIES =
        Set.of("exchangeType", "routingKeyExpression", "transacted");

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
                String outputDestination = Optional.ofNullable(bindingServiceProperties.getBindingDestination(output)).orElse(output);
                setOutProperties(streamFunctionProperties, beanOutName,
                    outputDestination, bindingServiceProperties, output);

                if (!output.equals(outputDestination)) {
                    setRabbitProducerProperties(environment, outputDestination, output);
                }
            });
    }

    protected void setInput(String beanInName, String inputAnnotation, StreamFunctionProperties streamFunctionProperties) {

        Optional.of(inputAnnotation)
            .filter(StringUtils::hasText)
            .ifPresent(input -> {
                streamFunctionProperties.getBindings()
                    .put(beanInName, input);
            });
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
}
