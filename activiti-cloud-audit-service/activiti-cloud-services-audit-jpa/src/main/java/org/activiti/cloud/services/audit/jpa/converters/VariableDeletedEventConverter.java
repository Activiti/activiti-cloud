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
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableDeletedEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableDeletedEventEntity;

public class VariableDeletedEventConverter extends BaseEventToEntityConverter {

    public VariableDeletedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return VariableEvent.VariableEvents.VARIABLE_DELETED.name();
    }

    @Override
    protected VariableDeletedEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new VariableDeletedEventEntity((CloudVariableDeletedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        VariableDeletedEventEntity variableDeletedEventEntity = (VariableDeletedEventEntity) auditEventEntity;

        return new CloudVariableDeletedEventImpl(
            variableDeletedEventEntity.getEventId(),
            variableDeletedEventEntity.getTimestamp(),
            variableDeletedEventEntity.getVariableInstance()
        );
    }
}
