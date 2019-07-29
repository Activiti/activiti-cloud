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

package org.activiti.cloud.services.audit.jpa.events;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Lob;

import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.cloud.api.process.model.events.CloudProcessDeployedEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.ProcessDefinitionJpaJsonConverter;

@Entity
@DiscriminatorValue(value = ProcessDeployedAuditEventEntity.PROCESS_DEPLOYED_EVENT)
public class ProcessDeployedAuditEventEntity extends AuditEventEntity {

    protected static final String PROCESS_DEPLOYED_EVENT = "ProcessDeployedEvent";

    @Convert(converter = ProcessDefinitionJpaJsonConverter.class)
    @Lob
    @Column
    private ProcessDefinition processDefinition;

    public ProcessDeployedAuditEventEntity() {
    }

    public ProcessDeployedAuditEventEntity(CloudProcessDeployedEvent cloudEvent) {
        super(cloudEvent);
        setProcessDefinition(cloudEvent.getEntity());
    }

    public ProcessDefinition getProcessDefinition() {
        return processDefinition;
    }

    public void setProcessDefinition(ProcessDefinition processDefinition) {
        this.processDefinition = processDefinition;
    }
}
