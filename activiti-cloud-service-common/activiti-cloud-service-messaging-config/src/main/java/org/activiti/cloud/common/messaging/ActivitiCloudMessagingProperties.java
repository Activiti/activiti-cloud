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
package org.activiti.cloud.common.messaging;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Stream;
import org.activiti.cloud.common.messaging.config.InputConverterFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = ActivitiCloudMessagingProperties.ACTIVITI_CLOUD_MESSAGING_PREFIX)
@Validated
public class ActivitiCloudMessagingProperties {

    public static final String ACTIVITI_CLOUD_MESSAGING_PREFIX = "activiti.cloud.messaging";

    public enum MessagingBroker {
        rabbitmq,
        kafka,
        aws,
    }

    /**
     * Configure messaging broker type. Default is rabbitmq
     */
    @NotNull
    private MessagingBroker broker = MessagingBroker.rabbitmq;

    /**
     * Enable partitioned messaging configuration for engine events producer and consumers
     */
    @NotNull
    private Boolean partitioned = false;

    /**
     * Set partition count for partitioned mode. Default is 1
     */
    @NotNull
    @Positive
    private Integer partitionCount = 1;

    /**
     * Configure consumer instance index for partitioned messaging. Default is 0
     */
    @NotNull
    @Min(0)
    private Integer instanceIndex = 0;

    /**
     * Set destination separator to use to build full destinations, i.e. prefix_destination. Default is _
     */
    @NotEmpty
    @Size(min = 1, max = 1)
    private String destinationSeparator = "_";

    /**
     * Set destination prefix to use to build destinations, i.e. prefix_destination. Default is empty string.
     */
    private String destinationPrefix = "";

    /**
     * Enable destination name transformers to apply conversion to all destination name for producers, consumers and connectors
     */
    private boolean destinationTransformersEnabled = false;

    /**
     * Configure list of transformer functions for destination
     */
    private List<String> destinationTransformers = new ArrayList<>();

    /**
     * Configure regex expression to use for replacement of illegal characters in the destination names. Default is [\t\s*#:]
     */
    private String destinationIllegalCharsRegex = "[\\t\\s*#:]";

    /**
     * Configure replacement character for illegal characters in the destination names. Default is -
     */
    private String destinationIllegalCharsReplacement = "-";

    /**
     * Configure functionRouter destinations for a single binding
     */
    @NestedConfigurationProperty
    private FunctionRouterProperties functionRouter = new FunctionRouterProperties();

    /**
     * Configure destination properties to apply customization to producers and consumer channel bindings with matching destination key.
     */
    private Map<String, DestinationProperties> destinations = new LinkedCaseInsensitiveMap<>();

    private Map<String, InputConverterFunction> inputConverters;

    @NestedConfigurationProperty
    private RabbitMqProperties rabbitmq = new RabbitMqProperties();

    public static class RabbitMqProperties {

        private Boolean missingAnonymousQueuesFatal;

        private Boolean missingDurableQueuesFatal;

        private Boolean compress = false;

        private Integer compressionLevel = 1;

        private String prefix;

        public Boolean getMissingAnonymousQueuesFatal() {
            return missingAnonymousQueuesFatal;
        }

        public void setMissingAnonymousQueuesFatal(Boolean missingAnonymousQueuesFatal) {
            this.missingAnonymousQueuesFatal = missingAnonymousQueuesFatal;
        }

        public Boolean getMissingDurableQueuesFatal() {
            return missingDurableQueuesFatal;
        }

        public void setMissingDurableQueuesFatal(Boolean missingDurableQueuesFatal) {
            this.missingDurableQueuesFatal = missingDurableQueuesFatal;
        }

        public Boolean isCompress() {
            return compress;
        }

        public void setCompress(Boolean compress) {
            this.compress = compress;
        }

        public Integer getCompressionLevel() {
            return compressionLevel;
        }

        public void setCompressionLevel(Integer compressionLevel) {
            this.compressionLevel = compressionLevel;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }

    public ActivitiCloudMessagingProperties() {}

    public RabbitMqProperties getRabbitmq() {
        return rabbitmq;
    }

    public void setRabbitmq(RabbitMqProperties rabbitmq) {
        this.rabbitmq = rabbitmq;
    }

    public Boolean isPartitioned() {
        return partitioned;
    }

    public void setPartitioned(boolean partitioned) {
        this.partitioned = partitioned;
    }

    public Integer getPartitionCount() {
        return partitionCount;
    }

    public void setPartitionCount(Integer partitionCount) {
        this.partitionCount = partitionCount;
    }

    public Integer getInstanceIndex() {
        return instanceIndex;
    }

    public void setInstanceIndex(Integer instanceIndex) {
        this.instanceIndex = instanceIndex;
    }

    public String getDestinationSeparator() {
        return destinationSeparator;
    }

    public void setDestinationSeparator(String destinationSeparator) {
        this.destinationSeparator = destinationSeparator;
    }

    public String getDestinationPrefix() {
        return destinationPrefix;
    }

    public void setDestinationPrefix(String destinationPrefix) {
        this.destinationPrefix = destinationPrefix;
    }

    public Map<String, DestinationProperties> getDestinations() {
        return destinations;
    }

    public void setDestinations(Map<String, DestinationProperties> destinations) {
        this.destinations = destinations;
    }

    public boolean isDestinationTransformersEnabled() {
        return destinationTransformersEnabled;
    }

    public void setDestinationTransformersEnabled(boolean destinationTransformersEnabled) {
        this.destinationTransformersEnabled = destinationTransformersEnabled;
    }

    public String getDestinationIllegalCharsRegex() {
        return destinationIllegalCharsRegex;
    }

    public void setDestinationIllegalCharsRegex(String destinationIllegalCharsRegex) {
        this.destinationIllegalCharsRegex = destinationIllegalCharsRegex;
    }

    public String getDestinationIllegalCharsReplacement() {
        return destinationIllegalCharsReplacement;
    }

    public void setDestinationIllegalCharsReplacement(String destinationIllegalCharsReplacement) {
        this.destinationIllegalCharsReplacement = destinationIllegalCharsReplacement;
    }

    public FunctionRouterProperties getFunctionRouter() {
        return functionRouter;
    }

    public void setFunctionRouter(FunctionRouterProperties functionRouter) {
        this.functionRouter = functionRouter;
    }

    public Function<String, String> transformDestination() {
        return input -> {
            InputConverter<String> converter = new InputConverter<>(input);

            for (String it : destinationTransformers) {
                InputConverterFunction func = Optional.ofNullable(inputConverters.get(it)).orElseThrow();
                converter = converter.convertBy(func);
            }

            return converter.pack();
        };
    }

    @Autowired
    public void configureInputConverters(@Lazy Map<String, InputConverterFunction> inputConverters) {
        this.inputConverters = new LinkedHashMap<>(inputConverters);
    }

    public List<String> getDestinationTransformers() {
        return destinationTransformers;
    }

    public void setDestinationTransformers(List<String> destinationTransformers) {
        this.destinationTransformers = destinationTransformers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivitiCloudMessagingProperties that = (ActivitiCloudMessagingProperties) o;
        return (
            broker == that.broker &&
            Objects.equals(partitioned, that.partitioned) &&
            Objects.equals(partitionCount, that.partitionCount) &&
            Objects.equals(instanceIndex, that.instanceIndex) &&
            Objects.equals(destinationSeparator, that.destinationSeparator) &&
            Objects.equals(destinationPrefix, that.destinationPrefix) &&
            Objects.equals(destinations, that.destinations) &&
            Objects.equals(functionRouter, that.functionRouter)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            broker,
            partitioned,
            partitionCount,
            instanceIndex,
            destinationSeparator,
            destinationPrefix,
            destinations
        );
    }

    @Override
    public String toString() {
        return (
            "ActivitiCloudMessagingProperties{" +
            "broker=" +
            broker +
            ", partitioned=" +
            partitioned +
            ", partitionCount=" +
            partitionCount +
            ", instanceIndex=" +
            instanceIndex +
            ", destinationSeparator='" +
            destinationSeparator +
            '\'' +
            ", destinationPrefix='" +
            destinationPrefix +
            '\'' +
            ", destinations=" +
            destinations +
            ", destinationIllegalCharsReplacement=" +
            destinationIllegalCharsReplacement +
            ", destinationIllegalCharsRegex=" +
            destinationIllegalCharsRegex +
            '}'
        );
    }

    @Validated
    public static class DestinationProperties {

        /**
         * Destination name to apply for matching channel binding destinations. If empty the key is used as name. Default is empty string.
         */
        private String name = "";

        /**
         * Destination scope to add to destination name, i.e. name.scope. Default is null
         */
        private String scope;

        /**
         * Destination prefix to override common destination prefix. Default is null
         */
        private String prefix;

        /**
         * Destination separator to override common destination separator. Default is null
         */
        private String separator;

        DestinationProperties() {}

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getSeparator() {
            return separator;
        }

        public void setSeparator(String separator) {
            this.separator = separator;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", DestinationProperties.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("scope='" + scope + "'")
                .add("prefix='" + prefix + "'")
                .add("separator='" + separator + "'")
                .toString();
        }
    }

    @Validated
    public static class FunctionRouterProperties {

        private boolean enabled;

        private final Map<String, BindingFunctionRouterProperties> routes = new LinkedCaseInsensitiveMap<>();

        private final Map<String, Map<String, String>> destinations = new LinkedCaseInsensitiveMap<>();

        private final Map<String, Map<String, List<String>>> registrations = new LinkedCaseInsensitiveMap<>();

        private final Map<String, String> registrationBindings = new LinkedCaseInsensitiveMap<>();

        @NotEmpty
        private String group = "function-router";

        private String errorHandlerDefinition;

        private int maxRetries = 3;

        private Duration retryInterval = Duration.ofMillis(10);

        private Duration requestTimeout = Duration.ofSeconds(15);

        @NestedConfigurationProperty
        private final FunctionRouterAnonymousProperties anonymous = new FunctionRouterAnonymousProperties();

        @NestedConfigurationProperty
        private ConsumerProperties consumer = new ConsumerProperties();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Map<String, String> destinations() {
            Map<String, String> result = new LinkedCaseInsensitiveMap<>();

            destinations.forEach((key, value) -> result.putAll(value));

            return result;
        }

        public Map<String, String> destinations(String routingContext) {
            return destinations.computeIfAbsent(routingContext, key -> new LinkedCaseInsensitiveMap<>());
        }

        public Map<String, List<String>> registrations() {
            Map<String, List<String>> result = new LinkedCaseInsensitiveMap<>();

            registrations
                .values()
                .forEach(it -> {
                    it.forEach((key, value) ->
                        result.compute(key, (k, v) ->
                            v == null ? value : Stream.concat(v.stream(), value.stream()).distinct().toList()
                        )
                    );
                });

            return result;
        }

        public Map<String, List<String>> registrations(String routingContext) {
            return registrations.computeIfAbsent(routingContext, key -> new LinkedCaseInsensitiveMap<>());
        }

        public Map<String, BindingFunctionRouterProperties> getRoutes() {
            return routes;
        }

        public boolean isFunctionRoute(String bindingName) {
            return routes.containsKey(bindingName) && routes.get(bindingName).isEnabled();
        }

        public List<String> getFunctionRoutes() {
            return routes.keySet().stream().filter(this::isFunctionRoute).toList();
        }

        public String groupPrefix() {
            return group.concat(".");
        }

        public void setGroupPrefix(String group) {}

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public ConsumerProperties getConsumer() {
            return consumer;
        }

        public void setConsumer(ConsumerProperties consumer) {
            this.consumer = consumer;
        }

        public boolean isExcludeRequiredProducerGroup(String bindingName) {
            return Optional.ofNullable(routes.get(bindingName))
                .map(BindingFunctionRouterProperties::isExcludeRequiredProducerGroups)
                .orElse(false);
        }

        public boolean isOverrideRequiredProducerGroup(String bindingName) {
            return Optional.ofNullable(routes.get(bindingName))
                .map(BindingFunctionRouterProperties::getOverrideRequiredProducerGroups)
                .map(it -> !it.isEmpty())
                .orElse(false);
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public Duration getRetryInterval() {
            return retryInterval;
        }

        public void setRetryInterval(Duration retryInterval) {
            this.retryInterval = retryInterval;
        }

        public void register(String bindingName, String functionBeanName) {
            destinations
                .keySet()
                .forEach(routingContext ->
                    Optional.ofNullable(destinations.get(routingContext))
                        .map(it -> it.get(bindingName))
                        .ifPresent(destination -> {
                            registrations(routingContext)
                                .computeIfAbsent(destination, key -> new ArrayList<>())
                                .add(functionBeanName);
                            registrationBindings.put(functionBeanName, bindingName);
                        })
                );
        }

        public void register(String bindingName, String functionBeanName, String connectorType) {
            destinations
                .keySet()
                .forEach(routingContext ->
                    Optional.ofNullable(destinations.get(routingContext))
                        .map(it -> it.get(bindingName))
                        .ifPresent(destinations ->
                            Stream.of(destinations.split(","))
                                .filter(connectorType::equals)
                                .findFirst()
                                .ifPresent(destination -> {
                                    registrations(routingContext)
                                        .computeIfAbsent(destination, key -> new ArrayList<>())
                                        .add(functionBeanName);
                                    registrationBindings.put(functionBeanName, bindingName);
                                })
                        )
                );
        }

        public Optional<String> bindingNameFor(String functionBeanName) {
            return Optional.ofNullable(registrationBindings.get(functionBeanName));
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FunctionRouterProperties that)) return false;
            return (
                enabled == that.enabled &&
                maxRetries == that.maxRetries &&
                Objects.equals(routes, that.routes) &&
                Objects.equals(destinations, that.destinations) &&
                Objects.equals(registrations, that.registrations) &&
                Objects.equals(group, that.group) &&
                Objects.equals(retryInterval, that.retryInterval) &&
                Objects.equals(consumer, that.consumer) &&
                Objects.equals(anonymous, that.anonymous)
            );
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                enabled,
                routes,
                destinations,
                registrations,
                group,
                maxRetries,
                retryInterval,
                consumer,
                anonymous,
                errorHandlerDefinition,
                requestTimeout
            );
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", FunctionRouterProperties.class.getSimpleName() + "[", "]")
                .add("enabled=" + enabled)
                .add("routes=" + routes)
                .add("destinations=" + destinations)
                .add("registrations=" + registrations)
                .add("group='" + group + "'")
                .add("maxRetries=" + maxRetries)
                .add("retryInterval=" + retryInterval)
                .add("consumer=" + consumer)
                .add("anonymous=" + anonymous)
                .add("errorHandlerDefinition=" + errorHandlerDefinition)
                .add("requestTimeout=" + requestTimeout)
                .toString();
        }

        public FunctionRouterAnonymousProperties getAnonymous() {
            return anonymous;
        }

        public String getErrorHandlerDefinition() {
            return errorHandlerDefinition;
        }

        public void setErrorHandlerDefinition(String errorHandlerDefinition) {
            this.errorHandlerDefinition = errorHandlerDefinition;
        }

        public Duration getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
        }
    }

    public static class BindingFunctionRouterProperties {

        private boolean enabled;
        private boolean excludeRequiredProducerGroups = true;
        private List<String> overrideRequiredProducerGroups = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isExcludeRequiredProducerGroups() {
            return excludeRequiredProducerGroups;
        }

        public void setExcludeRequiredProducerGroups(boolean excludeRequiredProducerGroups) {
            this.excludeRequiredProducerGroups = excludeRequiredProducerGroups;
        }

        public List<String> getOverrideRequiredProducerGroups() {
            return overrideRequiredProducerGroups;
        }

        public void setOverrideRequiredProducerGroups(List<String> overrideRequiredProducerGroups) {
            this.overrideRequiredProducerGroups = overrideRequiredProducerGroups;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", BindingFunctionRouterProperties.class.getSimpleName() + "[", "]")
                .add("enabled=" + enabled)
                .add("excludeRequiredProducerGroups=" + excludeRequiredProducerGroups)
                .add("overrideRequiredProducerGroups=" + overrideRequiredProducerGroups)
                .toString();
        }
    }

    public static class FunctionRouterAnonymousProperties {

        @NestedConfigurationProperty
        private final ConsumerProperties consumer = new ConsumerProperties();

        public ConsumerProperties getConsumer() {
            return consumer;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FunctionRouterAnonymousProperties that)) return false;
            return Objects.equals(consumer, that.consumer);
        }

        @Override
        public int hashCode() {
            return Objects.hash(consumer);
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("FunctionRouterAnonymousProperties{");
            sb.append("consumer=").append(consumer);
            sb.append('}');
            return sb.toString();
        }
    }

    static class InputConverter<T> {

        private final T data;

        public InputConverter(T data) {
            this.data = data;
        }

        public <U> InputConverter<U> convertBy(Function<T, U> function) {
            return new InputConverter<>(function.apply(data));
        }

        public T pack() {
            return data;
        }
    }
}
