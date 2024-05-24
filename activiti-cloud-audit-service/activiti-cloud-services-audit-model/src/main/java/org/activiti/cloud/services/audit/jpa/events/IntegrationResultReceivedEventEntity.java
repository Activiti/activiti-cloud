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

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.activiti.cloud.api.process.model.events.CloudIntegrationResultReceivedEvent;

@Entity(name = IntegrationResultReceivedEventEntity.INTEGRATION_RESULT_RECEIVED_EVENT)
@DiscriminatorValue(value = IntegrationResultReceivedEventEntity.INTEGRATION_RESULT_RECEIVED_EVENT)
public class IntegrationResultReceivedEventEntity extends IntegrationEventEntity {

    protected static final String INTEGRATION_RESULT_RECEIVED_EVENT = "IntegrationResultReceivedEvent";

    protected IntegrationResultReceivedEventEntity() {}

    public IntegrationResultReceivedEventEntity(CloudIntegrationResultReceivedEvent event) {
        super(event);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("IntegrationResultReceivedEventEntity [toString()=").append(super.toString()).append("]");
        return builder.toString();
    }
}
