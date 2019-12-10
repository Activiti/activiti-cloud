package org.activiti.cloud.services.audit.jpa.events;

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.cloud.api.model.shared.events.CloudVariableEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.VariableJpaJsonConverter;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class VariableAuditEventEntity extends AuditEventEntity {

    private String variableName;
    private String variableType;
    private String taskId;

    @Convert(converter = VariableJpaJsonConverter.class)
    @Column(columnDefinition = "text")
    private VariableInstance variableInstance;

    public VariableAuditEventEntity() {
    }

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
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(taskId, variableInstance, variableName, variableType);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        VariableAuditEventEntity other = (VariableAuditEventEntity) obj;
        return Objects.equals(taskId, other.taskId) 
                && Objects.equals(variableInstance, other.variableInstance) 
                && Objects.equals(variableName, other.variableName) 
                && Objects.equals(variableType, other.variableType);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("VariableAuditEventEntity [variableName=")
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
