package org.activiti.cloud.services.audit.jpa.events;

import org.activiti.api.process.model.BPMNTimer;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.TimerJpaJsonConverter;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(timer);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TimerAuditEventEntity other = (TimerAuditEventEntity) obj;
        return Objects.equals(timer, other.timer);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TimerAuditEventEntity [timer=")
               .append(timer)
               .append(", toString()=")
               .append(super.toString())
               .append("]");
        return builder.toString();
    }
}
