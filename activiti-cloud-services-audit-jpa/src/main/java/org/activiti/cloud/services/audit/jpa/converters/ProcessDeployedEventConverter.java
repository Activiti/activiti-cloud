/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.ProcessDeployedEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessDeployedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessDeployedAuditEventEntity;

public class ProcessDeployedEventConverter extends BaseEventToEntityConverter {

    public ProcessDeployedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return ProcessDeployedEvent.ProcessDefinitionEvents.PROCESS_DEPLOYED.name();
    }

    @Override
    protected ProcessDeployedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudProcessDeployedEvent cloudProcessDeployed = (CloudProcessDeployedEvent) cloudRuntimeEvent;
        return new ProcessDeployedAuditEventEntity(cloudProcessDeployed.getId(),
                                                   cloudProcessDeployed.getTimestamp(),
                                                   cloudProcessDeployed.getAppName(),
                                                   cloudProcessDeployed.getAppVersion(),
                                                   cloudProcessDeployed.getServiceName(),
                                                   cloudProcessDeployed.getServiceFullName(),
                                                   cloudProcessDeployed.getServiceType(),
                                                   cloudProcessDeployed.getServiceVersion(),
                                                   cloudProcessDeployed.getEntity());
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        ProcessDeployedAuditEventEntity processDeployedAuditEventEntity = (ProcessDeployedAuditEventEntity) auditEventEntity;
        CloudProcessDeployedEventImpl processDeployedEvent = new CloudProcessDeployedEventImpl(processDeployedAuditEventEntity.getEventId(),
                                                                                               processDeployedAuditEventEntity.getTimestamp(),
                                                                                               processDeployedAuditEventEntity.getProcessDefinition());
        processDeployedEvent.setAppName(processDeployedAuditEventEntity.getAppName());
        processDeployedEvent.setAppVersion(processDeployedAuditEventEntity.getAppVersion());
        processDeployedEvent.setServiceFullName(processDeployedAuditEventEntity.getServiceFullName());
        processDeployedEvent.setServiceName(processDeployedAuditEventEntity.getServiceName());
        processDeployedEvent.setServiceType(processDeployedAuditEventEntity.getServiceType());
        processDeployedEvent.setServiceVersion(processDeployedAuditEventEntity.getServiceVersion());
        return processDeployedEvent;
    }
}
