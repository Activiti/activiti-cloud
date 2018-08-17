package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableCreatedEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableCreatedEventEntity;
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
        return new VariableCreatedEventEntity(cloudVariableCreatedEvent.getId(),
                                              cloudVariableCreatedEvent.getTimestamp(),
                                              cloudVariableCreatedEvent.getAppName(),
                                              cloudVariableCreatedEvent.getAppVersion(),
                                              cloudVariableCreatedEvent.getServiceFullName(),
                                              cloudVariableCreatedEvent.getServiceName(),
                                              cloudVariableCreatedEvent.getServiceType(),
                                              cloudVariableCreatedEvent.getServiceVersion(),
                                              cloudVariableCreatedEvent.getEntity());
    }

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
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
