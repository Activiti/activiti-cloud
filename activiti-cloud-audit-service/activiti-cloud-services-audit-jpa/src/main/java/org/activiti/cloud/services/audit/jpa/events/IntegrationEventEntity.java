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

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.MappedSuperclass;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.events.CloudIntegrationEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.IntegrationContextJpaJsonConverter;

@MappedSuperclass
public abstract class IntegrationEventEntity extends AuditEventEntity {

    @Convert(converter = IntegrationContextJpaJsonConverter.class)
    @Column(columnDefinition = "text")
    private IntegrationContext integrationContext;

    IntegrationEventEntity() {}

    public IntegrationEventEntity(CloudIntegrationEvent event) {
        super(event);
        this.integrationContext = event.getEntity();
    }

    public IntegrationContext getIntegrationContext() {
        return integrationContext;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("IntegrationEventEntity [integrationContext=")
            .append(integrationContext)
            .append(", toString()=")
            .append(super.toString())
            .append("]");
        return builder.toString();
    }
}
