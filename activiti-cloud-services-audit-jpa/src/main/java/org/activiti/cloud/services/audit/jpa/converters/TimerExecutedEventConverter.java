package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.BPMNTimerEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerExecutedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerExecutedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TimerExecutedAuditEventEntity;

public class TimerExecutedEventConverter extends BaseEventToEntityConverter {

    public TimerExecutedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return BPMNTimerEvent.TimerEvents.TIMER_EXECUTED.name();
    }

    @Override
    protected TimerExecutedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new TimerExecutedAuditEventEntity((CloudBPMNTimerExecutedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        TimerExecutedAuditEventEntity timerEventEntity = (TimerExecutedAuditEventEntity) auditEventEntity;

        CloudBPMNTimerExecutedEventImpl cloudEvent = new CloudBPMNTimerExecutedEventImpl(timerEventEntity.getEventId(),
                                                                                         timerEventEntity.getTimestamp(),
                                                                                         timerEventEntity.getTimer(),
                                                                                         timerEventEntity.getProcessDefinitionId(),
                                                                                         timerEventEntity.getProcessInstanceId());
        return cloudEvent;
    }
}
