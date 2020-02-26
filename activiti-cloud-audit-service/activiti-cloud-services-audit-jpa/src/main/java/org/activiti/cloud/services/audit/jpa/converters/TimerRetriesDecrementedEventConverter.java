package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.BPMNTimerEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerRetriesDecrementedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerRetriesDecrementedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TimerRetriesDecrementedAuditEventEntity;

public class TimerRetriesDecrementedEventConverter extends BaseEventToEntityConverter {

    public TimerRetriesDecrementedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return BPMNTimerEvent.TimerEvents.TIMER_RETRIES_DECREMENTED.name();
    }

    @Override
    protected TimerRetriesDecrementedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new TimerRetriesDecrementedAuditEventEntity((CloudBPMNTimerRetriesDecrementedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        TimerRetriesDecrementedAuditEventEntity timerEventEntity = (TimerRetriesDecrementedAuditEventEntity) auditEventEntity;
        
        return new CloudBPMNTimerRetriesDecrementedEventImpl(timerEventEntity.getEventId(),
                                                             timerEventEntity.getTimestamp(),
                                                             timerEventEntity.getTimer(),
                                                             timerEventEntity.getProcessDefinitionId(),
                                                             timerEventEntity.getProcessInstanceId());
    }
}
