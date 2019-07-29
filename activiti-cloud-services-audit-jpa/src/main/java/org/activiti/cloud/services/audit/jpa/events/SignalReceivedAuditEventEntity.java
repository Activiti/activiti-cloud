/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.audit.jpa.events;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.activiti.api.process.model.BPMNSignal;
import org.activiti.cloud.api.process.model.events.CloudBPMNSignalReceivedEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.SignalJpaJsonConverter;

@Entity
@DiscriminatorValue(value = SignalReceivedAuditEventEntity.SIGNAL_RECEIVED_EVENT)
public class SignalReceivedAuditEventEntity extends AuditEventEntity {

    protected static final String SIGNAL_RECEIVED_EVENT = "SignalReceivedEvent";
    
    @Convert(converter = SignalJpaJsonConverter.class)
    @Column(columnDefinition="text")
    private BPMNSignal signal;

    public SignalReceivedAuditEventEntity() {
    }

    public SignalReceivedAuditEventEntity(CloudBPMNSignalReceivedEvent cloudEvent) {
        super(cloudEvent);
        setSignal(cloudEvent.getEntity()) ;
    }
    
    public BPMNSignal getSignal() {
        return signal;
    }

    public void setSignal(BPMNSignal signal) {
        this.signal = signal;
    }
}
