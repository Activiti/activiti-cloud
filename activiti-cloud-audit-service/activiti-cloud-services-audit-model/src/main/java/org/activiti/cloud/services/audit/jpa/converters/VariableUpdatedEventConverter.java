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
package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableUpdatedEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableUpdatedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableUpdatedEventEntity;

public class VariableUpdatedEventConverter extends BaseEventToEntityConverter {

    public VariableUpdatedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return VariableEvent.VariableEvents.VARIABLE_UPDATED.name();
    }

    @Override
    protected VariableUpdatedEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new VariableUpdatedEventEntity((CloudVariableUpdatedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        VariableUpdatedEventEntity variableUpdatedEventEntity = (VariableUpdatedEventEntity) auditEventEntity;

        String eventId = variableUpdatedEventEntity.getEventId();
        Long timestamp = variableUpdatedEventEntity.getTimestamp();
        VariableInstance variableInstance = variableUpdatedEventEntity.getVariableInstance();
        Object previousValue = variableUpdatedEventEntity.getPreviousValue() != null
            ? variableUpdatedEventEntity.getPreviousValue().getValue()
            : null;
        return new CloudVariableUpdatedEventImpl<>(eventId, timestamp, variableInstance, previousValue);
    }
}
