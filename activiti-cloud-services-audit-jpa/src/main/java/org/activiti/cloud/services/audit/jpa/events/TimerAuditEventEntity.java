package org.activiti.cloud.services.audit.jpa.events;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;

import org.activiti.api.process.model.BPMNTimer;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.TimerJpaJsonConverter;

@Entity
public abstract class TimerAuditEventEntity extends AuditEventEntity {

    @Convert(converter = TimerJpaJsonConverter.class)
    @Column(columnDefinition="text")
    private BPMNTimer timer;

    public TimerAuditEventEntity() {
    }

    public TimerAuditEventEntity(CloudBPMNTimerEvent cloudEvent) {
        super(cloudEvent);
        this.timer = cloudEvent.getEntity();
        if (timer != null) {
            setProcessDefinitionId(timer.getProcessDefinitionId());
            setProcessInstanceId(timer.getProcessInstanceId());
        }
        if (timer != null) {
            setEntityId(timer.getElementId());
        }
    }

    public BPMNTimer getTimer() {
        return timer;
    }

    public void setTimer(BPMNTimer timer) {
        this.timer = timer;
    }
}
