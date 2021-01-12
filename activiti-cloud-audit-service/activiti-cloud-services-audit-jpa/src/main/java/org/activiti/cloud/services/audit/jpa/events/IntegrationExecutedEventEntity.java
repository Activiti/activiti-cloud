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
package org.activiti.cloud.services.audit.jpa.events;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import org.activiti.cloud.api.process.model.events.CloudIntegrationExecutedEvent;

@Entity(name = IntegrationExecutedEventEntity.INTEGRATION_EXECUTED)
@DiscriminatorValue(value = IntegrationExecutedEventEntity.INTEGRATION_EXECUTED)
public class IntegrationExecutedEventEntity extends IntegrationEventEntity {

    protected static final String INTEGRATION_EXECUTED = "IntegrationExecutedEvent";

    IntegrationExecutedEventEntity() {}

    public IntegrationExecutedEventEntity(CloudIntegrationExecutedEvent event) {
        super(event);
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
        builder.append("IntegrationExecutedEventEntity [toString()=").append(super.toString()).append("]");
        return builder.toString();
    }

}
