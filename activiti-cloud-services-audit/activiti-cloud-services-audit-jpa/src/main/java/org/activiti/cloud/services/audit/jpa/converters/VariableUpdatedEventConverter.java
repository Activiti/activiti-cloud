package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableUpdatedEventEntity;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.CloudVariableUpdatedEvent;
import org.activiti.runtime.api.event.VariableEvent;
import org.activiti.runtime.api.event.impl.CloudVariableUpdatedEventImpl;
import org.springframework.stereotype.Component;

@Component
public class VariableUpdatedEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return VariableEvent.VariableEvents.VARIABLE_UPDATED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudVariableUpdatedEvent cloudVariableUpdatedEvent = (CloudVariableUpdatedEvent) cloudRuntimeEvent;
        VariableUpdatedEventEntity variableUpdatedEventEntity = new VariableUpdatedEventEntity(cloudVariableUpdatedEvent.getId(),
                                                                                               cloudVariableUpdatedEvent.getTimestamp(),
                                                                                               cloudVariableUpdatedEvent.getAppName(),
                                                                                               cloudVariableUpdatedEvent.getAppVersion(),
                                                                                               cloudVariableUpdatedEvent.getServiceFullName(),
                                                                                               cloudVariableUpdatedEvent.getServiceName(),
                                                                                               cloudVariableUpdatedEvent.getServiceType(),
                                                                                               cloudVariableUpdatedEvent.getServiceVersion(),
                                                                                               cloudVariableUpdatedEvent.getEntity());
        return variableUpdatedEventEntity;
    }

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
        VariableUpdatedEventEntity variableUpdatedEventEntity = (VariableUpdatedEventEntity) auditEventEntity;

        CloudVariableUpdatedEventImpl variableUpdatedEvent = new CloudVariableUpdatedEventImpl(variableUpdatedEventEntity.getEventId(),
                                                                                               variableUpdatedEventEntity.getTimestamp(),
                                                                                               variableUpdatedEventEntity.getVariableInstance());
        variableUpdatedEvent.setAppName(variableUpdatedEventEntity.getAppName());
        variableUpdatedEvent.setAppVersion(variableUpdatedEventEntity.getAppVersion());
        variableUpdatedEvent.setServiceFullName(variableUpdatedEventEntity.getServiceFullName());
        variableUpdatedEvent.setServiceName(variableUpdatedEventEntity.getServiceName());
        variableUpdatedEvent.setServiceType(variableUpdatedEventEntity.getServiceType());
        variableUpdatedEvent.setServiceVersion(variableUpdatedEventEntity.getServiceVersion());

        return variableUpdatedEvent;
    }
}
