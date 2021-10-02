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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.*;
import java.util.Objects;

@ConfigurationProperties(prefix = ActivitiCloudMessagingProperties.ACTIVITI_CLOUD_MESSAGING_PREFIX)
@Validated
public class ActivitiCloudMessagingProperties {
    public static final String ACTIVITI_CLOUD_MESSAGING_PREFIX = "activiti.cloud.messaging";

    public enum MessagingBroker {
        rabbitmq, kafka
    }

    @NotNull
    private MessagingBroker broker = MessagingBroker.rabbitmq;

    @NotNull
    private Boolean partitioned = false;

    @NotNull
    @Positive
    private Integer partitionCount = 1;

    @NotNull
    @Min(0)
    private Integer instanceIndex = 0;

    @NotEmpty
    @Size(min = 1, max = 1)
    private String destinationSeparator = "_";

    private String destinationPrefix = "";
    
    ActivitiCloudMessagingProperties() { }

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
        if (this == o) {
            return true;
    }

    public void setDestinationPrefix(String destinationPrefix) {
        this.destinationPrefix = destinationPrefix;
    }
        ActivitiCloudMessagingProperties that = (ActivitiCloudMessagingProperties) o;
        return broker == that.broker && Objects.equals(partitioned, that.partitioned) && Objects.equals(partitionCount, that.partitionCount) && Objects.equals(instanceIndex, that.instanceIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(broker, partitioned, partitionCount, instanceIndex);
    }

    @Override
    public String toString() {
        return "ActivitiCloudMessagingProperties{" +
            "broker=" + broker +
            ", partitioned=" + partitioned +
            ", partitionCount=" + partitionCount +
            ", instanceIndex=" + instanceIndex +
            ", destinationSeparator='" + destinationSeparator + '\'' +
            ", destinationPrefix='" + destinationPrefix + '\'' +
            '}';
    }

}
