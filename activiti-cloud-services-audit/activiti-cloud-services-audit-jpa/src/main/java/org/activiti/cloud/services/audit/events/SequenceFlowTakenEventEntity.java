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

package org.activiti.cloud.services.audit.events;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = SequenceFlowTakenEventEntity.SEQUENCE_FLOW_TAKEN_EVENT)
public class SequenceFlowTakenEventEntity extends AuditEventEntity {

    protected static final String SEQUENCE_FLOW_TAKEN_EVENT = "SequenceFlowTakenEvent";

    private String sequenceFlowId;
    private String sourceActivityId;
    private String sourceActivityName;
    private String sourceActivityType;
    private String targetActivityId;
    private String targetActivityName;
    private String targetActivityType;

    public SequenceFlowTakenEventEntity() {
    }

    public SequenceFlowTakenEventEntity(String eventId,
                                        Long timestamp,
                                        String eventType) {
        super(eventId,
              timestamp,
              eventType);
    }

    public SequenceFlowTakenEventEntity(String eventId,
                                        Long timestamp,
                                        String eventType,
                                        String sequenceFlowId,
                                        String sourceActivityId,
                                        String sourceActivityName,
                                        String sourceActivityType,
                                        String targetActivityId,
                                        String targetActivityName,
                                        String targetActivityType) {
        super(eventId,
              timestamp,
              eventType);
        this.sequenceFlowId = sequenceFlowId;
        this.sourceActivityId = sourceActivityId;
        this.sourceActivityName = sourceActivityName;
        this.sourceActivityType = sourceActivityType;
        this.targetActivityId = targetActivityId;
        this.targetActivityName = targetActivityName;
        this.targetActivityType = targetActivityType;
    }

    public String getSequenceFlowId() {
        return sequenceFlowId;
    }

    public String getSourceActivityId() {
        return sourceActivityId;
    }

    public String getSourceActivityName() {
        return sourceActivityName;
    }

    public String getSourceActivityType() {
        return sourceActivityType;
    }

    public String getTargetActivityId() {
        return targetActivityId;
    }

    public String getTargetActivityName() {
        return targetActivityName;
    }

    public String getTargetActivityType() {
        return targetActivityType;
    }
}
