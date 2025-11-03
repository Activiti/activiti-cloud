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
package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.IncidentEvent;
import org.activiti.cloud.api.process.model.IncidentEvent.IncidentEventType;
import org.activiti.cloud.api.process.model.impl.events.CloudIncidentCreatedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.IncidentCreatedEventEntity;

public class IncidentCreatedEventConverter extends BaseEventToEntityConverter {

    public IncidentCreatedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return IncidentEventType.INCIDENT_CREATED.toString();
    }

    @Override
    protected IncidentCreatedEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new IncidentCreatedEventEntity(IncidentEvent.class.cast(cloudRuntimeEvent));
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        var incidentCreatedEventEntity = IncidentCreatedEventEntity.class.cast(auditEventEntity);

        return new CloudIncidentCreatedEventImpl(
            incidentCreatedEventEntity.getEventId(),
            incidentCreatedEventEntity.getTimestamp(),
            incidentCreatedEventEntity.getProcessInstance(),
            incidentCreatedEventEntity.getErrorClassName(),
            incidentCreatedEventEntity.getErrorCode(),
            incidentCreatedEventEntity.getErrorMessage(),
            incidentCreatedEventEntity.getStackTraceElements()
        );
    }
}
