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

import org.activiti.api.process.model.events.IntegrationEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudIntegrationResultReceivedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationResultReceivedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.IntegrationResultReceivedEventEntity;

public class IntegrationResultReceivedEventConverter extends BaseEventToEntityConverter {

    public IntegrationResultReceivedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return IntegrationEvent.IntegrationEvents.INTEGRATION_RESULT_RECEIVED.toString();
    }

    @Override
    protected IntegrationResultReceivedEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new IntegrationResultReceivedEventEntity(
            CloudIntegrationResultReceivedEvent.class.cast(cloudRuntimeEvent)
        );
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        IntegrationResultReceivedEventEntity entity = IntegrationResultReceivedEventEntity.class.cast(auditEventEntity);

        return new CloudIntegrationResultReceivedEventImpl(entity.getEventId(), entity.getTimestamp(), entity.getIntegrationContext());
    }
}
