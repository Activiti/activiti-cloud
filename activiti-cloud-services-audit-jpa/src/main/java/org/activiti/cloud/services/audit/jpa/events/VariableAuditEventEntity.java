package org.activiti.cloud.services.audit.jpa.events;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;

import org.activiti.cloud.services.audit.jpa.converters.json.VariableJpaJsonConverter;
import org.activiti.api.model.shared.model.VariableInstance;

@MappedSuperclass
public abstract class VariableAuditEventEntity extends AuditEventEntity {

    private String variableName;
    private String variableType;
    private String taskId;

    @Convert(converter = VariableJpaJsonConverter.class)
    @Lob
    @Column
    private VariableInstance variableInstance;

    public VariableAuditEventEntity() {
    }

    public VariableAuditEventEntity(String eventId,
                                    Long timestamp,
                                    String eventType) {
        super(eventId,
              timestamp,
              eventType);
    }

    public VariableAuditEventEntity(String eventId,
                                    Long timestamp,
                                    String eventType,
                                    String appName,
                                    String appVersion,
                                    String serviceName,
                                    String serviceFullName,
                                    String serviceType,
                                    String serviceVersion,
                                    VariableInstance variableInstance) {
        super(eventId,
              timestamp,
              eventType);
        setAppName(appName);
        setAppVersion(appVersion);
        setServiceName(serviceName);
        setServiceFullName(serviceFullName);
        setServiceType(serviceType);
        setServiceVersion(serviceVersion);
        setProcessInstanceId((variableInstance != null) ? variableInstance.getProcessInstanceId() : null);
        this.variableInstance = variableInstance;
        if (variableInstance != null) {
            this.variableName = variableInstance.getName();
            this.variableType = variableInstance.getType();
            this.taskId = variableInstance.getTaskId();
            setEntityId(variableInstance.getName());
        }
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
    }
}
