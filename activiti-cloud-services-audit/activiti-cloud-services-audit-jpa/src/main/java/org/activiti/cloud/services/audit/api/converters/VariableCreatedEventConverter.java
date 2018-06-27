package org.activiti.cloud.services.audit.api.converters;

import org.activiti.cloud.services.audit.events.AuditEventEntity;
import org.activiti.cloud.services.audit.events.VariableCreatedEventEntity;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.CloudVariableCreatedEvent;
import org.activiti.runtime.api.event.VariableEvent;
import org.activiti.runtime.api.event.impl.CloudVariableCreatedEventImpl;
import org.activiti.runtime.api.model.VariableInstance;
import org.springframework.stereotype.Component;

@Component
public class VariableCreatedEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return VariableEvent.VariableEvents.VARIABLE_CREATED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudVariableCreatedEvent cloudVariableCreatedEvent = (CloudVariableCreatedEvent) cloudRuntimeEvent;
        VariableCreatedEventEntity variableCreatedEventEntity = new VariableCreatedEventEntity(cloudVariableCreatedEvent.getId(),
                                                                                               cloudVariableCreatedEvent.getTimestamp(),
                                                                                               cloudVariableCreatedEvent.getAppName(),
                                                                                               cloudVariableCreatedEvent.getAppVersion(),
                                                                                               cloudVariableCreatedEvent.getServiceFullName(),
                                                                                               cloudVariableCreatedEvent.getServiceName(),
                                                                                               cloudVariableCreatedEvent.getServiceType(),
                                                                                               cloudVariableCreatedEvent.getServiceVersion(),
                                                                                               cloudVariableCreatedEvent.getEntity());
        return variableCreatedEventEntity;
    }

    @Override
    public CloudRuntimeEvent<VariableInstance, VariableEvent.VariableEvents> convertToAPI(AuditEventEntity auditEventEntity) {
        VariableCreatedEventEntity variableCreatedEventEntity = (VariableCreatedEventEntity) auditEventEntity;

        CloudVariableCreatedEventImpl cloudVariableCreatedEvent = new CloudVariableCreatedEventImpl(variableCreatedEventEntity.getEventId(),
                                                                                                    variableCreatedEventEntity.getTimestamp(),
                                                                                                    variableCreatedEventEntity.getVariableInstance());
        cloudVariableCreatedEvent.setAppName(variableCreatedEventEntity.getAppName());
        cloudVariableCreatedEvent.setAppVersion(variableCreatedEventEntity.getAppVersion());
        cloudVariableCreatedEvent.setServiceFullName(variableCreatedEventEntity.getServiceFullName());
        cloudVariableCreatedEvent.setServiceName(variableCreatedEventEntity.getServiceName());
        cloudVariableCreatedEvent.setServiceType(variableCreatedEventEntity.getServiceType());
        cloudVariableCreatedEvent.setServiceVersion(variableCreatedEventEntity.getServiceVersion());

        return cloudVariableCreatedEvent;
    }
}
