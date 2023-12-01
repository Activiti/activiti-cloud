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

import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityCompletedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCompletedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.ActivityCompletedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;

public class ActivityCompletedEventConverter extends BaseEventToEntityConverter {

    public ActivityCompletedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED.name();
    }

    @Override
    protected ActivityCompletedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new ActivityCompletedAuditEventEntity((CloudBPMNActivityCompletedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        ActivityCompletedAuditEventEntity activityCompletedAuditEventEntity = (ActivityCompletedAuditEventEntity) auditEventEntity;

        return new CloudBPMNActivityCompletedEventImpl(
            activityCompletedAuditEventEntity.getEventId(),
            activityCompletedAuditEventEntity.getTimestamp(),
            activityCompletedAuditEventEntity.getBpmnActivity(),
            activityCompletedAuditEventEntity.getProcessDefinitionId(),
            activityCompletedAuditEventEntity.getProcessInstanceId()
        );
    }
}
