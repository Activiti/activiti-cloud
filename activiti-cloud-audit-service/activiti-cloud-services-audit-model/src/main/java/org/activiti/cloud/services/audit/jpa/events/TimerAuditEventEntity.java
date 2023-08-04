/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.audit.jpa.events;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import org.activiti.api.process.model.BPMNTimer;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.TimerJpaJsonConverter;

@MappedSuperclass
public abstract class TimerAuditEventEntity extends AuditEventEntity {

    @Convert(converter = TimerJpaJsonConverter.class)
    @Column(columnDefinition = "text")
    private BPMNTimer timer;

    public TimerAuditEventEntity() {}

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
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("TimerAuditEventEntity [timer=")
            .append(timer)
            .append(", toString()=")
            .append(super.toString())
            .append("]");
        return builder.toString();
    }
}
