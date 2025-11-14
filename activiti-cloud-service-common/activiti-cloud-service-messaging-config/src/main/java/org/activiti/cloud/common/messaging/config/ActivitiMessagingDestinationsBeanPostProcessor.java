/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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

import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.FUNCTION_ROUTER_ANONYMOUS_INPUT;
import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.FUNCTION_ROUTER_INPUT;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Base64UrlNamingStrategy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamFunctionConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

public class ActivitiMessagingDestinationsBeanPostProcessor implements BeanPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ActivitiMessagingDestinationsBeanPostProcessor.class);

    private final ActivitiMessagingDestinationTransformer destinationTransformer;
    private final ActivitiCloudMessagingProperties messagingProperties;
    private final FunctionBindingPropertySource functionBindingPropertySource;
    private final StreamFunctionConfigurationProperties streamFunctionProperties;

    public ActivitiMessagingDestinationsBeanPostProcessor(
        ActivitiMessagingDestinationTransformer destinationTransformer,
        ActivitiCloudMessagingProperties messagingProperties,
        FunctionBindingPropertySource functionBindingPropertySource,
        StreamFunctionConfigurationProperties streamFunctionProperties
    ) {
        this.destinationTransformer = destinationTransformer;
        this.messagingProperties = messagingProperties;
        this.functionBindingPropertySource = functionBindingPropertySource;
        this.streamFunctionProperties = streamFunctionProperties;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof BindingServiceProperties bindingServiceProperties) {
            log.info("Post-processing messaging destinations for bean {} with name {}", bean, beanName);
            final var functionRouter = messagingProperties.getFunctionRouter();

            // See https://github.com/spring-cloud/spring-cloud-stream/commit/afc8fa29da421f41fa70cedc4acc3b6c07addcf0
            Optional
                .ofNullable(ReflectionUtils.findField(BindingServiceProperties.class, "bindings", Map.class))
                .map(field -> Map.entry(field, bindingServiceProperties.getBindings()))
                .ifPresent(entry -> {
                    ReflectionUtils.makeAccessible(entry.getKey());
                    ReflectionUtils.setField(
                        entry.getKey(),
                        bindingServiceProperties,
                        new ConcurrentSkipListMap<String, BindingProperties>(String.CASE_INSENSITIVE_ORDER)
                    );

                    bindingServiceProperties.setBindings(entry.getValue());
                });

            bindingServiceProperties
                .getBindings()
                .forEach((bindingName, bindingProperties) -> {
                    String source = Optional.ofNullable(bindingProperties.getDestination()).orElse(bindingName);

                    String destination = destinationTransformer.apply(source);

                    bindingProperties.setDestination(destination);

                    log.warn("Configured destination '{}' for binding '{}'", destination, bindingName);
                });

            if (functionRouter.isEnabled()) {
                functionRouter
                    .getFunctionRoutes()
                    .stream()
                    .filter(bindingServiceProperties.getBindings()::containsKey)
                    .forEach(bindingName -> {
                        var value = bindingServiceProperties.getBindings().remove(bindingName);

                        Optional
                            .ofNullable(value.getGroup())
                            .filter(StringUtils::hasText)
                            .ifPresentOrElse(
                                it -> {
                                    functionRouter
                                        .destinations(FUNCTION_ROUTER_INPUT)
                                        .put(bindingName, value.getDestination());
                                },
                                () -> {
                                    functionRouter
                                        .destinations(FUNCTION_ROUTER_ANONYMOUS_INPUT)
                                        .put(bindingName, value.getDestination());
                                }
                            );

                        log.warn(
                            "Configured function route '{}' for destination '{}'",
                            bindingName,
                            value.getDestination()
                        );

                        Optional
                            .ofNullable(value.getGroup())
                            .filter(it -> functionRouter.isExcludeRequiredProducerGroup(bindingName))
                            .ifPresent(group -> {
                                bindingServiceProperties
                                    .getBindings()
                                    .entrySet()
                                    .stream()
                                    .filter(entry -> entry.getValue().getProducer() != null)
                                    .filter(entry ->
                                        Objects.equals(entry.getValue().getDestination(), value.getDestination())
                                    )
                                    .forEach(entry -> {
                                        var producer = entry.getValue().getProducer();
                                        var excludedGroups = value.getGroup();
                                        var producerGroups = producer.getRequiredGroups();
                                        var requiredGroups = Stream
                                            .of(producerGroups)
                                            .filter(Predicate.not(excludedGroups::equals))
                                            .toList();

                                        producer.setRequiredGroups(requiredGroups.toArray(new String[] {}));

                                        log.warn(
                                            "Excluded producer required groups '{}' for binding '{}'",
                                            excludedGroups,
                                            entry.getKey()
                                        );
                                    });
                            });
                    });

                functionRouter
                    .getRoutes()
                    .keySet()
                    .stream()
                    .filter(Predicate.not(functionRouter::isFunctionRoute))
                    .filter(functionRouter::isOverrideRequiredProducerGroup)
                    .forEach(bindingName -> {
                        Optional
                            .ofNullable(bindingServiceProperties.getBindings().get(bindingName))
                            .map(BindingProperties::getProducer)
                            .ifPresent(producer -> {
                                var overrideGroups = functionRouter
                                    .getRoutes()
                                    .get(bindingName)
                                    .getOverrideRequiredProducerGroups()
                                    .stream()
                                    .map(it ->
                                        Optional
                                            .ofNullable(messagingProperties.getRabbitmq().getPrefix())
                                            .map(prefix -> prefix.concat(it))
                                            .orElse(it)
                                    )
                                    .toList();

                                producer.setRequiredGroups(overrideGroups.toArray(new String[] {}));

                                log.warn(
                                    "Override producer required groups '{}' for binding '{}'",
                                    overrideGroups,
                                    bindingName
                                );
                            });
                    });

                if (!functionRouter.destinations(FUNCTION_ROUTER_ANONYMOUS_INPUT).isEmpty()) {
                    final var bindingProperties = new BindingProperties();
                    final var groupPrefix = functionRouter.groupPrefix();

                    final var destination = String.join(
                        ",",
                        Set.copyOf(functionRouter.destinations(FUNCTION_ROUTER_ANONYMOUS_INPUT).values())
                    );

                    bindingProperties.setDestination(destination);
                    bindingProperties.setGroup(new Base64UrlNamingStrategy(groupPrefix).generateName());
                    bindingProperties.setConsumer(functionRouter.getAnonymous().getConsumer());
                    bindingProperties.setErrorHandlerDefinition(functionRouter.getErrorHandlerDefinition());

                    bindingServiceProperties.getBindings().put(FUNCTION_ROUTER_ANONYMOUS_INPUT, bindingProperties);

                    log.warn("Configured anonymous function router binding '{}'", bindingProperties);
                } else {
                    log.warn("Skipping anonymous function router configuration with empty destinations");
                }

                if (!functionRouter.destinations(FUNCTION_ROUTER_INPUT).isEmpty()) {
                    final var bindingProperties = new BindingProperties();
                    final var group = functionRouter.getGroup();
                    final var destination = String.join(
                        ",",
                        Set.copyOf(functionRouter.destinations(FUNCTION_ROUTER_INPUT).values())
                    );

                    bindingProperties.setDestination(destination);
                    bindingProperties.setGroup(group);
                    bindingProperties.setConsumer(functionRouter.getConsumer());
                    bindingProperties.setErrorHandlerDefinition(functionRouter.getErrorHandlerDefinition());

                    bindingServiceProperties.getBindings().put(FUNCTION_ROUTER_INPUT, bindingProperties);

                    log.warn("Configured function router binding '{}'", bindingProperties);
                } else {
                    log.warn("Skipping function router configuration with empty destinations");
                }
            }
        }

        return bean;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
