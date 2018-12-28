package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.SequenceFlowEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudSequenceFlowTakenEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudSequenceFlowTakenImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.SequenceFlowAuditEventEntity;

public class SequenceFlowTakenEventConverter extends BaseEventToEntityConverter {
    
    public SequenceFlowTakenEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN.name();
    }

    @Override
    protected SequenceFlowAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudSequenceFlowTakenEvent cloudSequenceFlowTaken = (CloudSequenceFlowTakenEvent) cloudRuntimeEvent;

        return new SequenceFlowAuditEventEntity(cloudSequenceFlowTaken.getId(),
                                                cloudSequenceFlowTaken.getTimestamp(),
                                                cloudSequenceFlowTaken.getAppName(),
                                                cloudSequenceFlowTaken.getAppVersion(),
                                                cloudSequenceFlowTaken.getServiceName(),
                                                cloudSequenceFlowTaken.getServiceFullName(),
                                                cloudSequenceFlowTaken.getServiceType(),
                                                cloudSequenceFlowTaken.getServiceVersion(),
                                                cloudSequenceFlowTaken.getEntity());
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
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
        
        cloudSequenceFlowTakenEvent.setEntityId(sequenceFlowTakenAuditEventEntity.getProcessInstanceId());
        cloudSequenceFlowTakenEvent.setProcessDefinitionId(sequenceFlowTakenAuditEventEntity.getProcessDefinitionId());
        cloudSequenceFlowTakenEvent.setProcessInstanceId(sequenceFlowTakenAuditEventEntity.getProcessInstanceId());

        return cloudSequenceFlowTakenEvent;
    }
}
