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
package org.activiti.cloud.services.audit.jpa.events;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.IncidentContext;
import org.activiti.cloud.api.process.model.IncidentEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.IncidentContextJpaJsonConverter;

@MappedSuperclass
public abstract class IncidentAuditEventEntity extends AuditEventEntity {

    @Convert(converter = IncidentContextJpaJsonConverter.class)
    @Column(columnDefinition = "text")
    private IncidentContext incidentContext;

    public IncidentAuditEventEntity() {}

    public IncidentAuditEventEntity(IncidentEvent cloudEvent) {
        super(cloudEvent);
        setIncidentContext(cloudEvent.getEntity());
    }

    public IncidentAuditEventEntity(CloudRuntimeEvent<?, ?> cloudEvent) {
        super(cloudEvent);
    }

    public IncidentContext getIncidentContext() {
        return incidentContext;
    }

    public void setIncidentContext(IncidentContext incidentContext) {
        this.incidentContext = incidentContext;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("IncidentAuditEventEntity [IncidentAuditEventEntity=")
            .append(incidentContext)
            .append(", toString()=")
            .append(super.toString())
            .append("]");
        return builder.toString();
    }
}
