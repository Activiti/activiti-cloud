package org.activiti.cloud.services.audit.api.converters;

import org.activiti.cloud.services.audit.events.AuditEventEntity;
import org.activiti.cloud.services.audit.events.VariableDeletedEventEntity;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.CloudVariableDeletedEvent;
import org.activiti.runtime.api.event.VariableEvent;
import org.activiti.runtime.api.event.impl.CloudVariableDeletedEventImpl;
import org.activiti.runtime.api.model.VariableInstance;
import org.springframework.stereotype.Component;

@Component
public class VariableDeletedEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return VariableEvent.VariableEvents.VARIABLE_DELETED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudVariableDeletedEvent cloudVariableDeletedEvent = (CloudVariableDeletedEvent) cloudRuntimeEvent;
        VariableDeletedEventEntity variableDeletedEventEntity = new VariableDeletedEventEntity(cloudVariableDeletedEvent.getId(),
                                                                                               cloudVariableDeletedEvent.getTimestamp(),
                                                                                               cloudVariableDeletedEvent.getAppName(),
                                                                                               cloudVariableDeletedEvent.getAppVersion(),
                                                                                               cloudVariableDeletedEvent.getServiceFullName(),
                                                                                               cloudVariableDeletedEvent.getServiceName(),
                                                                                               cloudVariableDeletedEvent.getServiceType(),
                                                                                               cloudVariableDeletedEvent.getServiceVersion(),
                                                                                               cloudVariableDeletedEvent.getEntity());
        return variableDeletedEventEntity;
    }

    @Override
    public CloudRuntimeEvent<VariableInstance, VariableEvent.VariableEvents> convertToAPI(AuditEventEntity auditEventEntity) {
        VariableDeletedEventEntity variableDeletedEventEntity = (VariableDeletedEventEntity) auditEventEntity;

        CloudVariableDeletedEventImpl variableDeletedEvent = new CloudVariableDeletedEventImpl(variableDeletedEventEntity.getEventId(),
                                                                                               variableDeletedEventEntity.getTimestamp(),
                                                                                               variableDeletedEventEntity.getVariableInstance());
        variableDeletedEvent.setAppName(variableDeletedEventEntity.getAppName());
        variableDeletedEvent.setAppVersion(variableDeletedEventEntity.getAppVersion());
        variableDeletedEvent.setServiceFullName(variableDeletedEventEntity.getServiceFullName());
        variableDeletedEvent.setServiceName(variableDeletedEventEntity.getServiceName());
        variableDeletedEvent.setServiceType(variableDeletedEventEntity.getServiceType());
        variableDeletedEvent.setServiceVersion(variableDeletedEventEntity.getServiceVersion());

        return variableDeletedEvent;
    }
}
