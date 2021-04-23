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

import java.util.Objects;

@ConfigurationProperties(prefix = ActivitiCloudMessagingProperties.ACTIVITI_CLOUD_MESSAGING_PREFIX)
public class ActivitiCloudMessagingProperties {
    public static final String ACTIVITI_CLOUD_MESSAGING_PREFIX = "activiti.cloud.messaging";

    public enum MessagingBroker {
        rabbitmq, kafka
    }

    private MessagingBroker broker = MessagingBroker.rabbitmq;
    private boolean partitioned = false;
    private int partitionCount = 1;
    private int instanceIndex = 0;

    ActivitiCloudMessagingProperties() { }

    public boolean isPartitioned() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ActivitiCloudMessagingProperties)) {
            return false;
        }
        ActivitiCloudMessagingProperties that = (ActivitiCloudMessagingProperties) o;
        return partitioned == that.partitioned &&
            Objects.equals(partitionCount, that.partitionCount) &&
            Objects.equals(instanceIndex, that.instanceIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partitioned, partitionCount, instanceIndex);
    }

    @Override
    public String toString() {
        return "ActivitiMessagingBrokerProperties{" +
            "partitioned=" + partitioned +
            ", partitionCount=" + partitionCount +
            ", instanceIndex=" + instanceIndex +
            '}';
    }
}
