package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.BPMNTimerEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerScheduledEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerScheduledEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TimerScheduledAuditEventEntity;

public class TimerScheduledEventConverter extends BaseEventToEntityConverter {

    public TimerScheduledEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return BPMNTimerEvent.TimerEvents.TIMER_SCHEDULED.name();
    }

    @Override
    protected TimerScheduledAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new TimerScheduledAuditEventEntity((CloudBPMNTimerScheduledEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        TimerScheduledAuditEventEntity timerEventEntity = (TimerScheduledAuditEventEntity) auditEventEntity;

        return new CloudBPMNTimerScheduledEventImpl(timerEventEntity.getEventId(),
                                                    timerEventEntity.getTimestamp(),
                                                    timerEventEntity.getTimer(),
                                                    timerEventEntity.getProcessDefinitionId(),
                                                    timerEventEntity.getProcessInstanceId());
    }
}
