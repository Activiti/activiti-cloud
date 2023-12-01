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
package org.activiti.cloud.api.process.model.impl.events;

import org.activiti.api.process.model.StartMessageDeploymentDefinition;
import org.activiti.api.process.model.events.MessageDefinitionEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudStartMessageDeployedEvent;

public class CloudStartMessageDeployedEventImpl
    extends CloudRuntimeEventImpl<StartMessageDeploymentDefinition, MessageDefinitionEvent.MessageDefinitionEvents>
    implements CloudStartMessageDeployedEvent {

    private CloudStartMessageDeployedEventImpl(Builder builder) {
        super.setEntity(builder.entity);
    }

    public CloudStartMessageDeployedEventImpl() {}

    public CloudStartMessageDeployedEventImpl(StartMessageDeploymentDefinition entity) {
        super(entity);
        setEntityId(entity.getMessageSubscription().getId());

        setProcessDefinitionId(entity.getProcessDefinition().getId());
        setProcessDefinitionKey(entity.getProcessDefinition().getKey());
        setProcessDefinitionVersion(entity.getProcessDefinition().getVersion());
    }

    public CloudStartMessageDeployedEventImpl(String id, Long timestamp, StartMessageDeploymentDefinition entity) {
        super(id, timestamp, entity);
        if (getEntity() != null) {
            setEntityId(getEntity().getMessageSubscription().getId());
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CloudStartMessageDeployedEventImpl []");
        return builder.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder to build {@link CloudStartMessageDeployedEventImpl} and initialize it with the given object.
     * @param cloudStartMessageDeployedEventImpl to initialize the builder with
     * @return created builder
     */
    public static Builder builderFrom(CloudStartMessageDeployedEventImpl cloudStartMessageDeployedEventImpl) {
        return new Builder(cloudStartMessageDeployedEventImpl);
    }

    /**
     * Builder to build {@link CloudStartMessageDeployedEventImpl}.
     */
    public static final class Builder {

        private StartMessageDeploymentDefinition entity;

        public Builder() {}

        private Builder(CloudStartMessageDeployedEventImpl cloudStartMessageDeployedEventImpl) {
            this.entity = cloudStartMessageDeployedEventImpl.getEntity();
        }

        /**
         * Builder method for entity parameter.
         * @param entity field to set
         * @return builder
         */
        public Builder withEntity(StartMessageDeploymentDefinition entity) {
            this.entity = entity;
            return this;
        }

        /**
         * Builder method of the builder.
         * @return built class
         */
        public CloudStartMessageDeployedEventImpl build() {
            return new CloudStartMessageDeployedEventImpl(this);
        }
    }
}
