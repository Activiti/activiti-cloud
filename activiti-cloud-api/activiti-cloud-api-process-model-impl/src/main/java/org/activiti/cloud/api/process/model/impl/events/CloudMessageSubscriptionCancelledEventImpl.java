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

import org.activiti.api.process.model.MessageSubscription;
import org.activiti.api.process.model.events.MessageSubscriptionEvent;
import org.activiti.api.process.model.events.MessageSubscriptionEvent.MessageSubscriptionEvents;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudMessageSubscriptionCancelledEvent;

public class CloudMessageSubscriptionCancelledEventImpl
    extends CloudRuntimeEventImpl<MessageSubscription, MessageSubscriptionEvent.MessageSubscriptionEvents>
    implements CloudMessageSubscriptionCancelledEvent {

    private CloudMessageSubscriptionCancelledEventImpl(Builder builder) {
        this(builder.entity);
    }

    public CloudMessageSubscriptionCancelledEventImpl() {}

    public CloudMessageSubscriptionCancelledEventImpl(MessageSubscription entity) {
        super(entity);
        setProcessInstanceId(entity.getProcessInstanceId());
        setProcessDefinitionId(entity.getProcessDefinitionId());

        if (entity != null) {
            setEntityId(entity.getId());
        }
    }

    public CloudMessageSubscriptionCancelledEventImpl(
        String id,
        Long timestamp,
        MessageSubscription entity,
        String processDefinitionId,
        String processInstanceId
    ) {
        super(id, timestamp, entity);
        setProcessDefinitionId(processDefinitionId);
        setProcessInstanceId(processInstanceId);

        if (entity != null) {
            setEntityId(entity.getId());
        }
    }

    @Override
    public MessageSubscriptionEvent.MessageSubscriptionEvents getEventType() {
        return MessageSubscriptionEvents.MESSAGE_SUBSCRIPTION_CANCELLED;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("CloudMessageSubscriptionCancelledEventImpl [getEventType()=")
            .append(getEventType())
            .append(", toString()=")
            .append(super.toString())
            .append("]");
        return builder.toString();
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

    /**
     * Creates a builder to build {@link CloudMessageSubscriptionCancelledEventImpl}.
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder to build {@link CloudMessageSubscriptionCancelledEventImpl} and initialize it with the given object.
     * @param cloudMessageSubscriptionCancelledEventImpl to initialize the builder with
     * @return created builder
     */
    public static Builder builderFrom(
        CloudMessageSubscriptionCancelledEventImpl cloudMessageSubscriptionCancelledEventImpl
    ) {
        return new Builder(cloudMessageSubscriptionCancelledEventImpl);
    }

    /**
     * Builder to build {@link CloudMessageSubscriptionCancelledEventImpl}.
     */
    public static final class Builder {

        private MessageSubscription entity;

        public Builder() {}

        private Builder(CloudMessageSubscriptionCancelledEventImpl cloudMessageSubscriptionCancelledEventImpl) {
            this.entity = cloudMessageSubscriptionCancelledEventImpl.getEntity();
        }

        /**
         * Builder method for entity parameter.
         * @param entity field to set
         * @return builder
         */
        public Builder withEntity(MessageSubscription entity) {
            this.entity = entity;
            return this;
        }

        /**
         * Builder method of the builder.
         * @return built class
         */
        public CloudMessageSubscriptionCancelledEventImpl build() {
            return new CloudMessageSubscriptionCancelledEventImpl(this);
        }
    }
}
