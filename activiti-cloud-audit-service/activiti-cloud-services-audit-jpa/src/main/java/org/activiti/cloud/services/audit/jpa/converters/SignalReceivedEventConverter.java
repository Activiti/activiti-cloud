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

import org.activiti.api.process.model.events.BPMNSignalEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNSignalReceivedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNSignalReceivedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.SignalReceivedAuditEventEntity;

public class SignalReceivedEventConverter extends BaseEventToEntityConverter {

    public SignalReceivedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return BPMNSignalEvent.SignalEvents.SIGNAL_RECEIVED.name();
    }

    @Override
    protected SignalReceivedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new SignalReceivedAuditEventEntity((CloudBPMNSignalReceivedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        SignalReceivedAuditEventEntity signalReceivedAuditEventEntity = (SignalReceivedAuditEventEntity) auditEventEntity;

        return new CloudBPMNSignalReceivedEventImpl(
            signalReceivedAuditEventEntity.getEventId(),
            signalReceivedAuditEventEntity.getTimestamp(),
            signalReceivedAuditEventEntity.getSignal(),
            signalReceivedAuditEventEntity.getProcessDefinitionId(),
            signalReceivedAuditEventEntity.getProcessInstanceId()
        );
    }
}
