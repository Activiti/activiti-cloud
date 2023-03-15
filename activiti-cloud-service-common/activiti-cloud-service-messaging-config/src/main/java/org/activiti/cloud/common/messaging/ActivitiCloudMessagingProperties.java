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

package org.activiti.cloud.common.messaging;

import java.util.*;
import java.util.function.Function;
import javax.validation.constraints.*;
import org.activiti.cloud.common.messaging.config.InputConverterFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
     * Configure destination properties to apply customization to producers and consumer channel bindings with matching destination key.
     */
    private Map<String, DestinationProperties> destinations = new LinkedCaseInsensitiveMap<>();

    private Map<String, InputConverterFunction> inputConverters;

    ActivitiCloudMessagingProperties() {}

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
            Objects.equals(destinations, that.destinations)
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
            return (
                "DestinationProperties{" +
                ", name='" +
                name +
                '\'' +
                ", scope='" +
                scope +
                '\'' +
                ", prefix='" +
                prefix +
                '\'' +
                ", separator='" +
                separator +
                '\'' +
                '}'
            );
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
