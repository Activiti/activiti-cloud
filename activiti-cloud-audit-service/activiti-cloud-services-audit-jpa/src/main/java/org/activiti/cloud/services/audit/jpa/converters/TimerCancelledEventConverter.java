package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.BPMNTimerEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerCancelledEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerCancelledEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TimerCancelledAuditEventEntity;

public class TimerCancelledEventConverter extends BaseEventToEntityConverter {

    public TimerCancelledEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return BPMNTimerEvent.TimerEvents.TIMER_CANCELLED.name();
    }

    @Override
    protected TimerCancelledAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new TimerCancelledAuditEventEntity((CloudBPMNTimerCancelledEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        TimerCancelledAuditEventEntity timerEventEntity = (TimerCancelledAuditEventEntity) auditEventEntity;

        return new CloudBPMNTimerCancelledEventImpl(timerEventEntity.getEventId(),
                                                    timerEventEntity.getTimestamp(),
                                                    timerEventEntity.getTimer(),
                                                    timerEventEntity.getProcessDefinitionId(),
                                                    timerEventEntity.getProcessInstanceId());
    }
}
