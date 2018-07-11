package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCreatedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.SequenceFlowAuditEventEntity;
import org.activiti.runtime.api.event.BPMNActivityEvent;
import org.activiti.runtime.api.event.CloudProcessCreated;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.CloudSequenceFlowTaken;
import org.activiti.runtime.api.event.ProcessRuntimeEvent;
import org.activiti.runtime.api.event.SequenceFlowEvent;
import org.activiti.runtime.api.event.impl.CloudProcessCreatedEventImpl;
import org.activiti.runtime.api.event.impl.CloudSequenceFlowTakenImpl;
import org.springframework.stereotype.Component;

@Component
public class SequenceFlowTakenEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudSequenceFlowTaken cloudSequenceFlowTaken = (CloudSequenceFlowTaken) cloudRuntimeEvent;
        SequenceFlowAuditEventEntity sequenceFlowTakenAuditEventEntity = new SequenceFlowAuditEventEntity(cloudSequenceFlowTaken.getId(),
                                                                                                               cloudSequenceFlowTaken.getTimestamp(),
                                                                                                               cloudSequenceFlowTaken.getAppName(),
                                                                                                               cloudSequenceFlowTaken.getAppVersion(),
                                                                                                               cloudSequenceFlowTaken.getServiceFullName(),
                                                                                                               cloudSequenceFlowTaken.getServiceName(),
                                                                                                               cloudSequenceFlowTaken.getServiceType(),
                                                                                                               cloudSequenceFlowTaken.getServiceVersion(),
                                                                                                               cloudSequenceFlowTaken.getEntity());

        return sequenceFlowTakenAuditEventEntity;
    }

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
        SequenceFlowAuditEventEntity sequenceFlowTakenAuditEventEntity = (SequenceFlowAuditEventEntity) auditEventEntity;
        CloudSequenceFlowTakenImpl cloudSequenceFlowTakenEvent = new CloudSequenceFlowTakenImpl(sequenceFlowTakenAuditEventEntity.getEventId(),
                                                                                                     sequenceFlowTakenAuditEventEntity.getTimestamp(),
                                                                                                     sequenceFlowTakenAuditEventEntity.getSequenceFlow());
        cloudSequenceFlowTakenEvent.setAppName(sequenceFlowTakenAuditEventEntity.getAppName());
        cloudSequenceFlowTakenEvent.setAppVersion(sequenceFlowTakenAuditEventEntity.getAppVersion());
        cloudSequenceFlowTakenEvent.setServiceFullName(sequenceFlowTakenAuditEventEntity.getServiceFullName());
        cloudSequenceFlowTakenEvent.setServiceName(sequenceFlowTakenAuditEventEntity.getServiceName());
        cloudSequenceFlowTakenEvent.setServiceType(sequenceFlowTakenAuditEventEntity.getServiceType());
        cloudSequenceFlowTakenEvent.setServiceVersion(sequenceFlowTakenAuditEventEntity.getServiceVersion());
        return cloudSequenceFlowTakenEvent;
    }
}
