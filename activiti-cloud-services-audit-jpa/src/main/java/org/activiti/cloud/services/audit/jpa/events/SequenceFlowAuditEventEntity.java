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
import javax.persistence.Lob;

import org.activiti.api.process.model.BPMNSequenceFlow;
import org.activiti.api.process.model.events.SequenceFlowEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.SequenceFlowJpaJsonConverter;

@Entity
@DiscriminatorValue(value = SequenceFlowAuditEventEntity.SEQUENCE_FLOW_TAKEN_EVENT)
public class SequenceFlowAuditEventEntity extends AuditEventEntity {

    protected static final String SEQUENCE_FLOW_TAKEN_EVENT = "SequenceFlowTakenEvent";

    @Convert(converter = SequenceFlowJpaJsonConverter.class)
    @Lob
    @Column
    private BPMNSequenceFlow sequenceFlow;

    public SequenceFlowAuditEventEntity() {
    }

    public SequenceFlowAuditEventEntity(String eventId,
                                        Long timestamp) {
        super(eventId,
              timestamp,
              SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN.name());
    }

    public SequenceFlowAuditEventEntity(String eventId,
                                        Long timestamp,
                                        String appName,
                                        String appVersion,
                                        String serviceName,
                                        String serviceFullName,
                                        String serviceType,
                                        String serviceVersion,
                                        BPMNSequenceFlow sequenceFlow) {
        super(eventId,
              timestamp,
              SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN.name());
        setAppName(appName);
        setAppVersion(appVersion);
        setServiceName(serviceName);
        setServiceFullName(serviceFullName);
        setServiceType(serviceType);
        setServiceVersion(serviceVersion);
        setSequenceFlow(sequenceFlow) ;
    }

    public BPMNSequenceFlow getSequenceFlow() {
        return sequenceFlow;
    }

    public void setSequenceFlow(BPMNSequenceFlow sequenceFlow) {
        this.sequenceFlow = sequenceFlow;
    }
}
