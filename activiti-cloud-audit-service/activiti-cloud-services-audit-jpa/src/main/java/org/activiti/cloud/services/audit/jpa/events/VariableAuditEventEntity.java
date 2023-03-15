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

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.MappedSuperclass;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.cloud.api.model.shared.events.CloudVariableEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.VariableJpaJsonConverter;

@MappedSuperclass
public abstract class VariableAuditEventEntity extends AuditEventEntity {

    private String variableName;
    private String variableType;
    private String taskId;

    @Convert(converter = VariableJpaJsonConverter.class)
    @Column(columnDefinition = "text")
    private VariableInstance variableInstance;

    public VariableAuditEventEntity() {}

    public VariableAuditEventEntity(CloudVariableEvent cloudEvent) {
        super(cloudEvent);
        setVariableInstance(cloudEvent.getEntity());
    }

    public String getVariableName() {
        return variableName;
    }

    public String getVariableType() {
        return variableType;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setVariableType(String variableType) {
        this.variableType = variableType;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public VariableInstance getVariableInstance() {
        return variableInstance;
    }

    public void setVariableInstance(VariableInstance variableInstance) {
        this.variableInstance = variableInstance;
        if (variableInstance != null) {
            this.variableName = variableInstance.getName();
            this.variableType = variableInstance.getType();
            this.taskId = variableInstance.getTaskId();
            setEntityId(variableInstance.getName());
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("VariableAuditEventEntity [variableName=")
            .append(variableName)
            .append(", variableType=")
            .append(variableType)
            .append(", taskId=")
            .append(taskId)
            .append(", variableInstance=")
            .append(variableInstance)
            .append(", toString()=")
            .append(super.toString())
            .append("]");
        return builder.toString();
    }
}
