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
package org.activiti.cloud.services.audit.jpa.events;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessRuntimeEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.ProcessInstanceJpaJsonConverter;

@MappedSuperclass
public abstract class ProcessAuditEventEntity extends AuditEventEntity {

    @Convert(converter = ProcessInstanceJpaJsonConverter.class)
    @Column(columnDefinition = "text")
    private ProcessInstance processInstance;

    public ProcessAuditEventEntity() {}

    public ProcessAuditEventEntity(CloudProcessRuntimeEvent cloudEvent) {
        super(cloudEvent);
        setProcessInstance(cloudEvent.getEntity());
    }

    public ProcessAuditEventEntity(CloudRuntimeEvent<?, ?> cloudEvent) {
        super(cloudEvent);
    }

    public ProcessInstance getProcessInstance() {
        return processInstance;
    }

    public void setProcessInstance(ProcessInstance processInstance) {
        this.processInstance = processInstance;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("ProcessAuditEventEntity [processInstance=")
            .append(processInstance)
            .append(", toString()=")
            .append(super.toString())
            .append("]");
        return builder.toString();
    }
}
