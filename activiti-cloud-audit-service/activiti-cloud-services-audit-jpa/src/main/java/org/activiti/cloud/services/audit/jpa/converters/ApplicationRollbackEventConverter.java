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

import org.activiti.api.process.model.events.ApplicationEvent.ApplicationEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudApplicationDeployedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudApplicationDeployedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.ApplicationDeployedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;

public class ApplicationRollbackEventConverter extends BaseEventToEntityConverter {

    public ApplicationRollbackEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return ApplicationEvents.APPLICATION_ROLLBACK.name();
    }

    @Override
    protected ApplicationDeployedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new ApplicationDeployedAuditEventEntity((CloudApplicationDeployedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        ApplicationDeployedAuditEventEntity applicationDeployedAuditEventEntity = (ApplicationDeployedAuditEventEntity) auditEventEntity;
        return new CloudApplicationDeployedEventImpl(
            applicationDeployedAuditEventEntity.getEventId(),
            applicationDeployedAuditEventEntity.getTimestamp(),
            applicationDeployedAuditEventEntity.getDeployment(),
            ApplicationEvents.APPLICATION_ROLLBACK
        );
    }
}
