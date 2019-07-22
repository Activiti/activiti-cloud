package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.BPMNTimerEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerFiredEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerFiredEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TimerFiredAuditEventEntity;

public class TimerFiredEventConverter extends BaseEventToEntityConverter {

    public TimerFiredEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return BPMNTimerEvent.TimerEvents.TIMER_FIRED.name();
    }

    @Override
    protected TimerFiredAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new TimerFiredAuditEventEntity((CloudBPMNTimerFiredEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        TimerFiredAuditEventEntity timerEventEntity = (TimerFiredAuditEventEntity) auditEventEntity;

        CloudBPMNTimerFiredEventImpl cloudEvent = new CloudBPMNTimerFiredEventImpl(timerEventEntity.getEventId(),
                                                                                   timerEventEntity.getTimestamp(),
                                                                                   timerEventEntity.getTimer(),
                                                                                   timerEventEntity.getProcessDefinitionId(),
                                                                                   timerEventEntity.getProcessInstanceId());
        return cloudEvent;
    }
}
